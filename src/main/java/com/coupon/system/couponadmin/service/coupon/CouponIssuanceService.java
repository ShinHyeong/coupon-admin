package com.coupon.system.couponadmin.service.coupon;

import com.coupon.system.couponadmin.domain.auth.Admin;
import com.coupon.system.couponadmin.domain.auth.AdminRepository;
import com.coupon.system.couponadmin.domain.coupon.Coupon;
import com.coupon.system.couponadmin.domain.coupon.CouponRepository;
import com.coupon.system.couponadmin.domain.couponissurancejob.CouponIssuanceJob;
import com.coupon.system.couponadmin.domain.couponissurancejob.CouponIssuanceJobRepository;
import com.coupon.system.couponadmin.domain.couponissurancejob.CouponIssuanceJobStatus;
import com.coupon.system.couponadmin.exception.auth.AdminNotFoundException;
import com.coupon.system.couponadmin.exception.coupon.InvalidFileException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CouponIssuanceService {

    //청크(Chunk) 단위로 처리할 배치 크기
    private static final int BATCH_SIZE = 1000;

    private final CouponIssuanceJobRepository couponIssuanceJobRepository;
    private final CouponRepository couponRepository;
    private final AdminRepository adminRepository; // adminId를 찾기 위해
    private final String uploadDir;

    // application.yml에서 정의한 파일 저장 경로 주입
    public CouponIssuanceService(CouponIssuanceJobRepository couponIssuanceJobRepository,
                                 CouponRepository couponRepository,
                                 AdminRepository adminRepository,
                                 @Value("${file.upload-dir}") String uploadDir) {
        this.couponIssuanceJobRepository = couponIssuanceJobRepository;
        this.couponRepository = couponRepository;
        this.adminRepository = adminRepository;
        this.uploadDir = uploadDir;
    }

    /**
     * API 1: 파일 업로드 및 발급 처리 (컨트롤러가 호출)
     */
    @Transactional
    public CouponIssuanceJob uploadAndProcessFile(MultipartFile file, String adminName) throws IOException {

        // 1. 파일 검증 (명세서 요구사항)
        validateFile(file);

        // 2. 현재 로그인한 Admin ID 찾기
        Admin admin = adminRepository.findByAdminName(adminName)
                .orElseThrow(AdminNotFoundException::new);

        // 3. 파일 저장 (물리적)
        String savedFilePath = saveFile(file);

        // 4. 작업(Job) 생성 (DB)
        CouponIssuanceJob job = new CouponIssuanceJob(
                file.getOriginalFilename(),
                savedFilePath,
                admin.getId() // 엔티티 생성자 수정 필요 (아래 참고)
        );
        CouponIssuanceJob savedJob = couponIssuanceJobRepository.save(job);

        // 5. 비동기 발급 처리 (핵심)
        processCouponIssuance(savedJob.getId());

        return savedJob;
    }

    /**
     * (비동기) 실제 쿠폰 발급 처리
     */
    @Async // 이 메서드는 별도 스레드에서 비동기로 동작
    @Transactional
    public void processCouponIssuance(Long jobId) {
        // @Transactional을 @Async와 함께 쓰려면 self-invocation이 아닌
        // 다른 빈에서 호출해야 함 (지금은 Controller -> Service -> Async이므로 OK)

        CouponIssuanceJob job = couponIssuanceJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));

        try {
            // 1. 상태 'PENDING'으로 변경
            job.updateJobStatus(CouponIssuanceJobStatus.PENDING);
            couponIssuanceJobRepository.save(job);

            Path path = Paths.get(job.getSavedFilePath());

            //배치 처리를 위한 리스트
            List<Coupon> couponsToSave = new ArrayList<>(BATCH_SIZE);
            int totalCount = 0;

            // 2. 저장된 파일 읽기
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(path)))) {
                String header = reader.readLine(); // 헤더(customer_id)는 건너뛴다

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue; // 빈 줄은 건너뛴다

                    String customerId = line.trim();
                    totalCount++;

                    // 3. 쿠폰 엔티티 생성
                    Coupon coupon = new Coupon(
                            UUID.randomUUID().toString(), // 쿠폰 코드
                            customerId,
                            jobId,
                            LocalDateTime.now().plusDays(30) // (임의로 설정함) 보통 만료일 = 30일 뒤
                            // 엔티티 생성자 수정 필요 (아래 참고)
                    );
                    couponsToSave.add(coupon);

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

    /**
     * API 2: 파일 다운로드 (컨트롤러가 호출)
     * @return 다운로드할 파일 Resource 객체
     */
    @Transactional(readOnly = true)
    public Resource getFileResource(Long jobId) throws MalformedURLException {
        CouponIssuanceJob job = couponIssuanceJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));

        Path path = Paths.get(job.getSavedFilePath());
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("Could not read file: " + job.getOriginalFileName());
        }
        return resource;
    }


    // --- 헬퍼 메서드 ---

    /**
     * 파일 검증 (헤더: customer_id, 빈 파일 여부)
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidFileException("파일이 비어있습니다.");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String header = reader.readLine();

            if (header == null) {
                throw new InvalidFileException("파일이 비어있습니다.");
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
     * 파일을 물리적 저장소에 저장 (고유한 이름으로)
     * @return 저장된 실제 경로
     */
    private String saveFile(MultipartFile file) throws IOException {
        // 같은 경로 조작 방지
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            originalFileName = "file";
        }

        // 고유한 파일명 생성
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath); // 폴더가 없으면 생성
        }

        Path destinationPath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), destinationPath); // 파일 저장

        return destinationPath.toString();
    }

    /**
     * 최신순(ID 역순)으로 정렬하여 조회
     * @Returns 작업물 목록
     */
    @Transactional(readOnly = true)
    public List<CouponIssuanceJob> getCouponJobs(){
        return couponIssuanceJobRepository.findAll(
                Sort.by(Sort.Direction.DESC, "id")
        );
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
