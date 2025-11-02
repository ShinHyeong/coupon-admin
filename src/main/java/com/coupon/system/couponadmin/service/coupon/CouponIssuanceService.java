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
import com.coupon.system.couponadmin.exception.auth.AdminNotFoundException;
import com.coupon.system.couponadmin.exception.coupon.InvalidFileException;
import com.coupon.system.couponadmin.service.file.FileStorage;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

@Slf4j
@Service
public class CouponIssuanceService {

    //청크(Chunk) 단위로 처리할 배치 크기
    private static final int BATCH_SIZE = 1000;

    private final CouponIssuanceJobRepository couponIssuanceJobRepository;
    private final CouponRepository couponRepository;
    private final AdminRepository adminRepository; // adminId를 찾기 위해

    private final FileStorage fileStorage;

    // 생성자에서 'FileStorage' 인터페이스 타입을 받고 (유연성 확보),
    // @Primary 또는 @Qualifier로 실제 구현체(S3/Local)를 주입받는다.
    public CouponIssuanceService(CouponIssuanceJobRepository couponIssuanceJobRepository,
                                 CouponRepository couponRepository,
                                 AdminRepository adminRepository,
                                 FileStorage fileStorage) {
        this.couponIssuanceJobRepository = couponIssuanceJobRepository;
        this.couponRepository = couponRepository;
        this.adminRepository = adminRepository;
        this.fileStorage = fileStorage;
    }

    /**
     * API 1: 파일 업로드를 위한 Presigned URL 생성
     * 클라우드 스토리지(S3 등)에서만 지원됨
     */
    public CreatePresignedUrlResponse createPresignedUrl(String originalFileName) {
        // 실제 로직은 FileStorage 구현체에 위임
        return fileStorage.createPresignedUrl(originalFileName);
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
     * API 2: 파일 다운로드 (컨트롤러가 호출)
     * @return 다운로드할 파일 Resource 객체
     */
    @Transactional(readOnly = true)
    public Resource getFileResource(Long jobId){

        CouponIssuanceJob job = couponIssuanceJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));

        try {
            return fileStorage.loadAsResource(job.getSavedFilePath());
        } catch (Exception e) {
            throw new RuntimeException("Could not read file: " + job.getOriginalFileName(), e);
        }
    }


    /**
     * API 3 : 비동기로 쿠폰 발급 처리
     * 검증 -> 파싱 -> 생성
     * 쿠폰은 대용량으로 발급된다.
     */
    @Async // 이 메서드는 별도 스레드에서 비동기로 동작하게 한다
    @Transactional
    public void issueCoupons(Long jobId) {

        CouponIssuanceJob job = couponIssuanceJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));

        // 파싱 결과 (고객 ID 목록)
        List<String> customerIds;
        int totalCount = 0;

        try {
            // 1. 상태 'PENDING' 변경
            job.updateJobStatus(CouponIssuanceJobStatus.PENDING);
            couponIssuanceJobRepository.save(job);

            // S3/Local 등에서 파일을 스트림으로 읽어옴
            try (InputStream inputStream = new BufferedInputStream(fileStorage.loadAsInputStream(savedJob.getSavedFilePath()))) {

                if (originalFilename.endsWith(".csv")) {
                    customerIds = parseCsv(inputStream);
                } else if (originalFilename.endsWith(".xls") || originalFilename.endsWith(".xlsx") || originalFilename.endsWith(".xlsm")) {
                    customerIds = parseExcel(inputStream);
                } else {
                    throw new InvalidFileException("지원하지 않는 파일 형식입니다.");
                }
            }

            // 3. 회원 목록이 비어있는지 검증
            if (customerIds.isEmpty()) {
                throw new InvalidFileException("헤더 외에 회원 목록이 비어있습니다.");
            }

            // 4. [배치 처리] 파싱된 'customerIds' 리스트로 쿠폰 생성
            totalCount = customerIds.size();
            List<Coupon> couponsToSave = new ArrayList<>(BATCH_SIZE);

            for (String customerId : customerIds) {
                couponsToSave.add(new Coupon(
                        UUID.randomUUID().toString(),
                        customerId,
                        jobId,
                        LocalDateTime.now().plusDays(30)
                ));

                if (couponsToSave.size() == BATCH_SIZE) {
                    couponRepository.saveAll(couponsToSave);
                    couponsToSave.clear();
                    log.info("Job ID {}: {}/{} 아이템 진행중...", jobId, couponsToSave.size(), totalCount);
                }
            }

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
     * 파일 확장자를 확인하고, 스트림을 열어 각 포맷에 맞는 검증 메서드를 호출
     */
    private void validateFile(MultipartFile multipartFile) throws IOException, InvalidFileException {
        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new InvalidFileException("파일 이름이 없습니다.");
        }

        try (InputStream inputStream = multipartFile.getInputStream()) {
            if (originalFilename.endsWith(".csv")) {
                validateCsv(inputStream);
            } else if (originalFilename.endsWith(".xls") || originalFilename.endsWith(".xlsx") || originalFilename.endsWith(".xlsm")) {
                validateExcel(inputStream);
            } else {
                throw new InvalidFileException("지원하지 않는 파일 형식입니다.");
            }
        }
    }

    /**
     * CSV 파일 검증
     */
    private void validateCsv(InputStream inputStream) throws IOException, InvalidFileException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            //1. 헤더 검증
            String header = reader.readLine();
            if (header == null) {
                throw new InvalidFileException("파일 헤더가 비어있습니다.");
            }
            if (!"customer_id".equalsIgnoreCase(header.trim())) {
                throw new InvalidFileException("파일 헤더가 'customer_id'가 아닙니다.");
            }

            //2. 내용 존재 여부 검증 : 데이터가 한 줄 이상 있는지 확인 (빈 줄은 무시)
            boolean hasData = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    hasData = true;
                    break; // 데이터 한 줄이라도 찾으면 즉시 검증 종료! (효율적)
                }
            }
            if (!hasData) {
                throw new InvalidFileException("헤더 외에 회원 목록이 비어있습니다.");
            }
        }
    }

    /**
     * Excel 파일 검증
     */
    private void validateExcel(InputStream inputStream) throws IOException, InvalidFileException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
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
    }

    /**
     * CSV 파일 파서
     * InputStream -> List<String>으로 변환함
     */
    private List<String> parseCsv(InputStream inputStream) throws IOException, InvalidFileException {
        List<String> customerIds = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String header = reader.readLine();
            // 2. 데이터 파싱
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    customerIds.add(line.trim());
                }
            }
        }
        return customerIds;
    }

    /**
     * Excel 파일 파서
     * InputStream -> List<String>으로 변환함
     */
    private List<String> parseExcel(InputStream inputStream) throws IOException, InvalidFileException {
        List<String> customerIds = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            // 2. 데이터 파싱 (1번 row부터)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell cell = row.getCell(0);
                if (cell == null) continue;

                String customerId = "";
                if (cell.getCellType() == CellType.STRING) {
                    customerId = cell.getStringCellValue().trim();
                } else if (cell.getCellType() == CellType.NUMERIC) {
                    customerId = String.valueOf((long) cell.getNumericCellValue());
                }

                if (!customerId.isEmpty()) {
                    customerIds.add(customerId);
                }
            }
        }
        return customerIds;
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

    /**
     * 작업id로 원본 파일명 조회
     * @param jobId
     * @return 원본 파일명
     */
    @Transactional(readOnly = true)
    public String getOriginalFileName(Long jobId){
        return couponIssuanceJobRepository.findById(jobId)
                .map(CouponIssuanceJob::getOriginalFileName)
                .orElse("file.csv");
    }
}