package com.coupon.system.couponadmin.controller.coupon;

import com.coupon.system.couponadmin.dto.couponissurancejob.response.CreateCouponIssuanceJobResponse;
import com.coupon.system.couponadmin.dto.couponissurancejob.response.GetAllCouponIssuanceJobsResponse;
import com.coupon.system.couponadmin.service.coupon.CouponIssuanceService;
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
     * API 1: 하나의 파일 업로드하여 쿠폰발행작업 1개 생성
     * @param file (폼데이터 'file' 키)
     * @param authentication (현재 로그인한 사용자 정보)
     * @return 생성된 Job 객체 (JSON)
     */
    @PostMapping
    public ResponseEntity<CreateCouponIssuanceJobResponse> createCouponIssuanceJob(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        // 현재 로그인한 관리자의 이름(adminName)을 서비스로 전달
        String adminName = authentication.getName();

        CreateCouponIssuanceJobResponse response = couponIssuanceService.createCouponIssuanceJob(file, adminName);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * API 2: 작업 목록 조회
     * @return Job 리스트 (JSON)
     */
    @GetMapping
    public ResponseEntity<List<GetAllCouponIssuanceJobsResponse>> getAllCouponIssuanceJobs() {

        List<GetAllCouponIssuanceJobsResponse> response = couponIssuanceService.getAllCouponIssuanceJobs();

        return ResponseEntity.ok(response);
    }

    /**
     * API 3: 업로드했던 원본 파일 다운로드
     * @param jobId 다운로드할 Job ID
     * @return 파일 Resource
     */
    @GetMapping("/{jobId}/file")
    public ResponseEntity<Resource> downloadUploadedFile(@PathVariable Long jobId)
            throws MalformedURLException {

        Resource resource = couponIssuanceService.getFileResource(jobId);

        // 원본 파일명으로 다운로드되도록 헤더 설정
        String originalFileName = couponIssuanceService.getOriginalFileName(jobId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFileName + "\"")
                .body(resource);
    }
}
