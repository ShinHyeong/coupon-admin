package com.coupon.system.couponadmin.service.coupon;

import com.coupon.system.couponadmin.domain.auth.Admin;
import com.coupon.system.couponadmin.domain.auth.AdminRepository;
import com.coupon.system.couponadmin.domain.coupon.Coupon;
import com.coupon.system.couponadmin.domain.coupon.CouponRepository;
import com.coupon.system.couponadmin.domain.couponissurancejob.CouponIssuanceJob;
import com.coupon.system.couponadmin.domain.couponissurancejob.CouponIssuanceJobRepository;
import com.coupon.system.couponadmin.domain.couponissurancejob.CouponIssuanceJobStatus;
import com.coupon.system.couponadmin.dto.couponissurancejob.response.CreateCouponIssuanceJobResponse;
import com.coupon.system.couponadmin.dto.couponissurancejob.response.GetAllCouponIssuanceJobsResponse;
import com.coupon.system.couponadmin.dto.file.DownloadCouponIssuanceFileResponse;
import com.coupon.system.couponadmin.exception.auth.AdminNotFoundException;
import com.coupon.system.couponadmin.exception.coupon.InvalidFileException;
import com.coupon.system.couponadmin.service.file.FileStorage;
import com.coupon.system.couponadmin.util.GenericBatchProcessor;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import com.coupon.system.couponadmin.dto.couponissurancejob.request.CreateCouponIssuanceJobRequest;
import com.coupon.system.couponadmin.dto.couponissurancejob.response.GetPresignedUrlResponse;
import java.io.BufferedInputStream;

@Slf4j
@Service
public class CouponIssuanceService {

    //청크(Chunk) 단위로 처리할 배치 크기
    private static final int BATCH_SIZE = 1000;

    private final CouponBatchService couponBatchService;
    private final CouponIssuanceJobRepository couponIssuanceJobRepository;
    private final CouponRepository couponRepository;
    private final AdminRepository adminRepository; // adminId를 찾기 위해

    private final FileStorage fileStorage;

    // 생성자에서 'FileStorage' 인터페이스 타입을 받고 (유연성 확보),
    // @Primary 또는 @Qualifier로 실제 구현체(S3/Local)를 주입받는다.
    public CouponIssuanceService(CouponBatchService couponBatchService, CouponIssuanceJobRepository couponIssuanceJobRepository, CouponRepository couponRepository, AdminRepository adminRepository, FileStorage fileStorage) {
        this.couponBatchService = couponBatchService;
        this.couponIssuanceJobRepository = couponIssuanceJobRepository;
        this.couponRepository = couponRepository;
        this.adminRepository = adminRepository;
        this.fileStorage = fileStorage;
    }

    /**
     * API 1: 파일 업로드를 위해 Presigned URL을 가져온다
     * 클라우드 스토리지(S3 등)에서만 지원됨
     */
    public GetPresignedUrlResponse getPresignedUrl(String fileName, String fileType) {
        // 실제 로직은 FileStorage 구현체에 위임
        return fileStorage.getPresignedUrl(fileName, fileType);
    }

    /**
     * API 2 : 다운로드에 필요한 파일 리소스와 원본 파일명을 job DB 조회 한 번으로 처리
     * @param jobId
     * @return 다운로드 파일 정보 DTO
     */
    @Transactional(readOnly = true)
    public DownloadCouponIssuanceFileResponse downloadCouponIssuanceFile(Long jobId) {

        CouponIssuanceJob savedJob = couponIssuanceJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));

        try {
            // 2. 파일 리소스 로드
            Resource resource = fileStorage.loadAsResource(savedJob.getSavedFilePath());

            // 3. DTO에 담아서 반환
            return new DownloadCouponIssuanceFileResponse(resource, savedJob.getOriginalFileName());
        } catch (Exception e) {
            throw new RuntimeException("Could not read file for job id: " + jobId, e);
        }
    }

    /**
     * API 3-1 : 파일 업로드 완료 후 쿠폰 발급 작업 생성
     */
    @Transactional
    public CreateCouponIssuanceJobResponse createCouponIssuanceJob(
            CreateCouponIssuanceJobRequest request, String adminName) throws IOException {

        //1. 현재 로그인한 Admin ID로 DB에서 해당 Admin 찾는다
        Admin savedAdmin = adminRepository.findByAdminName(adminName)
                .orElseThrow(AdminNotFoundException::new);

        //2. DTO로부터 파일 정보를 받아 쿠폰 발급 작업(Job) 생성한 후 DB에 반영한다
        CouponIssuanceJob savedJob = couponIssuanceJobRepository.save(new CouponIssuanceJob(
                request.originalFileName(),
                request.savedFilePath(), // S3 파일 경로(key) 저장
                savedAdmin.getId()
        ));

        //3. 비동기로 파일 검증과 쿠폰 발급을 처리한다
        issueCoupons(savedJob.getId());

        //4. 생성한 작업 엔티티 -> DTO 변환
        return new CreateCouponIssuanceJobResponse(
                savedJob.getId(),
                savedJob.getOriginalFileName(),
                savedJob.getJobStatus().name(),
                savedJob.getAdminId(),
                savedJob.getCreatedAt()
        );
    }

    /**
     * API 3-2 : 비동기로 파일 검증과 쿠폰 발급을 처리한다
     * 검증 -> 파싱 -> 생성
     * 쿠폰은 대용량으로 발급됨을 가정하였다
     */
    @Async // 이 메서드는 별도 스레드에서 비동기로 동작하게 한다
    public void issueCoupons(Long jobId) {

        CouponIssuanceJob savedJob = couponIssuanceJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));

        int totalCount = 0;

        try {
            // 1. 상태 'PENDING' 변경
            savedJob.updateJobStatus(CouponIssuanceJobStatus.PENDING);
            couponIssuanceJobRepository.save(savedJob);

            // 2. 파일을 딱 한 번 열고, 확장자에 따라 파일 검증 및 파싱 처리
            //배치 처리기(Processor) 생성
            Consumer<List<Coupon>> saveAction = batch ->
                    couponBatchService.saveCouponsInNewTransaction(batch); //배치단위로 트랜잭션
            GenericBatchProcessor<Coupon> batchProcessor = new GenericBatchProcessor<>(BATCH_SIZE, saveAction);

            // S3/Local 등에서 파일을 스트림으로 읽어옴
            try (InputStream inputStream = new BufferedInputStream(fileStorage.loadAsInputStream(savedJob.getSavedFilePath()))) {

                // 2-1. 스트림으로 파일 검증 (스트림을 닫지 않고 진행)
                String originalFilename  = savedJob.getOriginalFileName();
                validateFileStream(inputStream, originalFilename);

                // 2-2. 파일 파싱과 쿠폰발행작업 생성을 스트리밍으로 동시에 처리 (OOM 방지)
                if (originalFilename.endsWith(".csv")) {
                    processCsvStream(inputStream, jobId, batchProcessor);
                } else if (originalFilename.endsWith(".xls") || originalFilename.endsWith(".xlsx")) {
                    processExcelStream(inputStream, jobId, batchProcessor);
                } else {
                    throw new InvalidFileException("지원하지 않는 파일 형식입니다.");
                }
            }

            // 3. 루프가 끝난 후, 남아있는 쿠폰들을 최종적으로 저장
            batchProcessor.flush();

            // 4. 작업 완료 처리
            savedJob.updateJobStatus(CouponIssuanceJobStatus.COMPLETED);
            savedJob.updateTotalCount(totalCount);
            savedJob.updateSuccessCount(totalCount);
            savedJob.updateCompletedAt(LocalDateTime.now());
            couponIssuanceJobRepository.save(savedJob);

            log.info("Job ID {} 완료. 총 {}개 쿠폰 발행완료.", jobId, totalCount);

        } catch (Exception e) {
            // 5. 그 외 예외 처리
            log.error("진행 실패 job ID {}: {}", jobId, e.getMessage(), e);
            savedJob.updateJobStatus(CouponIssuanceJobStatus.FAILED);
            couponIssuanceJobRepository.save(savedJob);
        }
    }

    // --- 헬퍼 메서드 ---

    /**
     * InputStream을 직접 검증함
     * 스트림을 두 번 읽기 위해 mark/reset 사용
     */
    private void validateFileStream(InputStream inputStream, String originalFilename) throws IOException, InvalidFileException {
        if (!inputStream.markSupported()) {
            throw new IOException("Mark/reset not supported by this InputStream");
        }
        inputStream.mark(Integer.MAX_VALUE); // 스트림 시작점 마킹

        if (originalFilename.endsWith(".csv")) {
            validateCsv(inputStream);
        } else if (originalFilename.endsWith(".xls") || originalFilename.endsWith(".xlsx")) {
            validateExcel(inputStream);
        } else {
            throw new InvalidFileException("지원하지 않는 파일 형식입니다.");
        }

        inputStream.reset(); // 파싱을 위해 스트림 포인터를 시작점으로 되돌림
    }

    /**
     * CSV 파일 검증
     * 스트림의 생명주기는 이 메서드를 호출한 issueCoupons에서 관리함
     * reader를 직접 닫지 않음
     */
    private void validateCsv(InputStream inputStream) throws IOException, InvalidFileException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        //1. 헤더 검증
        String header = reader.readLine();
        if (header == null || !"customer_id".equalsIgnoreCase(header.trim())) {
            throw new InvalidFileException("파일 헤더가 'customer_id'가 아니거나 비어있습니다.");
        }

        //2. 내용 존재 여부 검증
        boolean hasData = false;
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                hasData = true;
                break; //한줄이라도 있으면 검증 종료
            }
        }
        if (!hasData) {
            throw new InvalidFileException("헤더 외에 회원 목록이 비어있습니다.");
        }

    }


    /**
     * Excel 파일 검증
     */
    private void validateExcel(InputStream inputStream) throws IOException, InvalidFileException {

        Workbook workbook = WorkbookFactory.create(inputStream);
        Sheet sheet = workbook.getSheetAt(0);

        //1. 헤더 검증
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new InvalidFileException("파일 헤더가 비어있습니다.");
        }
        Cell headerCell = headerRow.getCell(0);
        if (headerCell == null || !"customer_id".equalsIgnoreCase(headerCell.getStringCellValue().trim())) {
            throw new InvalidFileException("파일 헤더가 'customer_id'가 아닙니다.");
        }

        //2. 내용 존재 여부 검증
        boolean hasData = false;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row dataRow = sheet.getRow(i);
            if (dataRow == null) continue;
            Cell firstCell = dataRow.getCell(0);
            if (firstCell != null && firstCell.getCellType() != CellType.BLANK) {
                hasData = true;
                break; // 데이터 셀을 하나라도 찾으면 즉시 검증 종료! (효율적)
            }
        }
        if (!hasData) {
            throw new InvalidFileException("헤더 외에 회원 목록이 비어있습니다.");
        }
    }


    /**
     * CSV 파싱 및 쿠폰 발행 작업 생성 (배치단위)
     * 쿠폰 발행 작업 생성은 처리기가 책임을 가짐
     */
    private void processCsvStream(InputStream inputStream, Long jobId, GenericBatchProcessor<Coupon> processor) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            reader.readLine(); // 헤더 스킵
            String line;
            while ((line = reader.readLine()) != null) {
                String customerId = line.trim();
                if (customerId.isEmpty()) continue;

                Coupon coupon = new Coupon(UUID.randomUUID().toString(), customerId, jobId, LocalDateTime.now().plusDays(30));
                processor.add(coupon); // 저장 로직은 처리기에 위임
            }
        }
    }

    /**
     * Excel 파싱 및 쿠폰 발행 작업 생성 (배치단위)
     * 쿠폰 발행 작업 생성은 처리기가 책임을 가짐
     */
    private void processExcelStream(InputStream inputStream, Long jobId, GenericBatchProcessor<Coupon> processor) throws IOException {
        try (Workbook workbook = com.monitorjbl.xlsx.StreamingReader.builder().open(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // 헤더 스킵

                String customerId = parseCustomerIdFromCell(row.getCell(0));
                if (!customerId.isEmpty()) {
                    Coupon coupon = new Coupon(UUID.randomUUID().toString(), customerId, jobId, LocalDateTime.now().plusDays(30));
                    processor.add(coupon); // 저장 로직은 처리기에 위임
                }
            }
        }
    }

    /**
     * Excel 셀 파싱 로직
     */
    private String parseCustomerIdFromCell(Cell cell) {
        if (cell == null) return "";

        String customerId = "";
        if (cell.getCellType() == CellType.STRING) {
            customerId = cell.getStringCellValue().trim();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            customerId = String.valueOf((long) cell.getNumericCellValue());
        }
        return customerId;
    }

    /**
     * 최신순(ID 역순)으로 정렬하여 모든 작업 목록을 조회한다
     * @Returns 모든 작업 목록
     */
    @Transactional(readOnly = true)
    public List<GetAllCouponIssuanceJobsResponse> getAllCouponIssuanceJobs(){

        //
        List<CouponIssuanceJob> savedJobs = couponIssuanceJobRepository.findAll(
                Sort.by(Sort.Direction.DESC, "id")
        );

        return savedJobs.stream()
                .map(job -> new GetAllCouponIssuanceJobsResponse(
                        job.getId(),
                        job.getOriginalFileName(),
                        job.getJobStatus().name(),
                        job.getTotalCount(),
                        job.getSuccessCount(),
                        job.getFailCount(),
                        job.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}