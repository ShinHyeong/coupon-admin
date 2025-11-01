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
     * API 1: 파일 업로드로부터 쿠폰 발급 작업 총괄
     */
    @Transactional
    public CreateCouponIssuanceJobResponse createCouponIssuanceJob(MultipartFile file, String adminName) throws IOException {

        // 1. 명세서 요구사항에 따라 업로드한 파일을 검증한다
        validateFile(file);

        // 2. 현재 로그인한 Admin ID로 DB에서 해당 Admin 찾는다
        Admin savedAdmin = adminRepository.findByAdminName(adminName)
                .orElseThrow(AdminNotFoundException::new);

        // 3. 저장소에 업로드한 파일 저장한다
        String savedFilePath = fileStorage.save(file);

        // 4. 작업(Job) 생성 후 DB에 반영한다
        CouponIssuanceJob savedJob = couponIssuanceJobRepository.save(new CouponIssuanceJob(
                file.getOriginalFilename(),
                savedFilePath,
                savedAdmin.getId()
        ));

        // 5. 비동기로 쿠폰 발급을 처리한다
        issueCoupons(savedJob.getId());

        //6. 생성한 작업 엔티티 -> DTO 변환한다

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
     * 쿠폰은 대용량으로 발급된다.
     */
    @Async // 이 메서드는 별도 스레드에서 비동기로 동작하게 한다
    @Transactional
    public void issueCoupons(Long jobId) {

        CouponIssuanceJob job = couponIssuanceJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));

        try {
            // 1. 상태 'PENDING(처리중)'으로 변경
            job.updateJobStatus(CouponIssuanceJobStatus.PENDING);
            couponIssuanceJobRepository.save(job);

            //배치 처리를 위한 리스트 생성
            List<Coupon> couponsToSave = new ArrayList<>(BATCH_SIZE);
            int totalCount = 0;

            try (InputStream inputStream = fileStorage.loadAsInputStream(job.getSavedFilePath());
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                String header = reader.readLine(); //header 건너뛰기
                String line;

                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue; // 빈 줄은 건너뛴다

                    String customerId = line.trim();
                    totalCount++;

                    // 3. 쿠폰 엔티티 생성
                    couponsToSave.add(new Coupon(
                            UUID.randomUUID().toString(), // 쿠폰 코드
                            customerId,
                            jobId,
                            LocalDateTime.now().plusDays(30) // (임의로 설정함) 보통 만료일 = 30일 뒤
                            // 엔티티 생성자 수정 필요 (아래 참고)
                    ));

                    //리스트가 BATCH_SIZE에 도달하면 DB에 저장
                    if (couponsToSave.size()==BATCH_SIZE){
                        couponRepository.saveAll(couponsToSave);
                        couponsToSave.clear(); //임의로 담아둔 리스트를 비운다
                        log.info("Job ID {}: {}개의 아이템 진행중...", jobId, totalCount);
                    }
                }
            }

            // 4. 루프가 끝난 후 리스트에 남아있는 쿠폰 저장
            if (!couponsToSave.isEmpty()) {
                couponRepository.saveAll(couponsToSave);
                log.info("Job ID {}: 남아있는 쿠폰 저장", jobId);
            }

            // 5. 작업 완료 처리
            job.updateJobStatus(CouponIssuanceJobStatus.COMPLETED);
            job.updateTotalCount(totalCount);
            job.updateSuccessCount(totalCount); // (임시) 지금은 실패 케이스가 없으므로
            job.updateCompletedAt(LocalDateTime.now());
            couponIssuanceJobRepository.save(job);

            log.info("Job ID {} 완료. 총 {}개 쿠폰 발행완료.", jobId, totalCount);

        } catch (Exception e) {
            // 6. 예외 발생 시 작업 실패 처리
            log.error("진행 실패 job ID {}: {}", jobId, e.getMessage(), e);
            job.updateJobStatus(CouponIssuanceJobStatus.FAILED);
            couponIssuanceJobRepository.save(job);
        }
    }

    // --- 헬퍼 메서드 ---

    /**
     * 명세서 요구사항 : csv와 excel 파일에 대한 검증
     * 1. 헤더값이 customer_id가 맞는지
     * 2. 회원 목록이 비어있지 않은지
     * 3. 각각의 회원번호가 유효한지는 검사하지 않는다
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidFileException("파일이 비어있습니다.");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String header = reader.readLine();

            if (header == null) {
                throw new InvalidFileException("파일 헤더가 비어있습니다.");
            }

            if (!"customer_id".equalsIgnoreCase(header.trim())) {
                throw new InvalidFileException("파일 헤더가 'customer_id'가 아닙니다.");
            }

            if (reader.readLine() == null) {
                throw new InvalidFileException("헤더 외에 회원 목록이 비어있습니다.");
            }
        }
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
