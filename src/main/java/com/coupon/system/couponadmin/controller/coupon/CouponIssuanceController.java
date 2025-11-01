package com.coupon.system.couponadmin.controller.coupon;

import com.coupon.system.couponadmin.service.coupon.CouponIssuanceService;
import org.springframework.web.bind.annotation.RestController;

import com.coupon.system.couponadmin.domain.couponissurancejob.CouponIssuanceJob;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

@RestController
@RequestMapping("/coupons/jobs")
public class CouponIssuanceController {

    private final CouponIssuanceService couponIssuanceService;

    public CouponIssuanceController(CouponIssuanceService couponIssuanceService) {
        this.couponIssuanceService = couponIssuanceService;
    }

    /**
     * API 1: 파일 업로드
     * @param file (폼데이터 'file' 키)
     * @param authentication (현재 로그인한 사용자 정보)
     * @return 생성된 Job 객체 (JSON)
     */
    @PostMapping()
    public ResponseEntity<CouponIssuanceJob> uploadCouponFile(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        // 현재 로그인한 관리자의 이름(adminName)을 서비스로 전달
        String adminName = authentication.getName();

        CouponIssuanceJob job = couponIssuanceService.uploadAndProcessFile(file, adminName);

        // 202 Accepted: 요청은 받았고, 비동기 처리를 시작함
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
    }

    /**
     * API 2: 작업 목록 조회
     * @return Job 리스트 (JSON)
     */
    @GetMapping
    public ResponseEntity<List<CouponIssuanceJob>> getCouponJobs() {
        List<CouponIssuanceJob> jobs = couponIssuanceService.getCouponJobs();
        return ResponseEntity.ok(jobs);
    }

    /**
     * API 3: 파일 다운로드
     * @param jobId 다운로드할 Job ID
     * @return 파일 Resource
     */
    @GetMapping("/{jobId}/file")
    public ResponseEntity<Resource> downloadCouponFile(@PathVariable Long jobId)
            throws MalformedURLException {

        Resource resource = couponIssuanceService.getFileResource(jobId);

        // 원본 파일명으로 다운로드되도록 헤더 설정
        // (참고: getOriginalFileName()을 Job 엔티티에 만들어야 함)
        String originalFileName = couponIssuanceService.getOriginalFileName(jobId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFileName + "\"")
                .body(resource);
    }
}
