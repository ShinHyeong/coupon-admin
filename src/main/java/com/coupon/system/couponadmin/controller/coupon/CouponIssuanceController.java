package com.coupon.system.couponadmin.controller.coupon;

import com.coupon.system.couponadmin.domain.couponissurancejob.CouponIssuanceJob;
import com.coupon.system.couponadmin.dto.couponissurancejob.request.CreateCouponIssuanceJobRequest;
import com.coupon.system.couponadmin.dto.couponissurancejob.response.CreateCouponIssuanceJobResponse;
import com.coupon.system.couponadmin.dto.couponissurancejob.response.GetPresignedUrlResponse;
import com.coupon.system.couponadmin.dto.couponissurancejob.response.GetAllCouponIssuanceJobsResponse;
import com.coupon.system.couponadmin.dto.file.DownloadCouponIssuanceFileResponse;
import com.coupon.system.couponadmin.exception.coupon.InvalidFileException;
import com.coupon.system.couponadmin.service.coupon.CouponIssuanceService;
import com.coupon.system.couponadmin.service.file.FileService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/coupons/jobs")
public class CouponIssuanceController {

    private final CouponIssuanceService couponIssuanceService;

    public CouponIssuanceController(CouponIssuanceService couponIssuanceService) {
        this.couponIssuanceService = couponIssuanceService;
    }


    /**
     * API 1: S3 업로드 완료 보고 및 쿠폰 발행 작업 생성 요청
     * @param request (S3 파일 경로와 원본 파일명이 담긴 DTO)
     * @param authentication (현재 로그인한 사용자 정보)
     * @return 생성된 Job 객체 (JSON)
     */
    @PostMapping
    public ResponseEntity<CreateCouponIssuanceJobResponse> createCouponIssuanceJob(
            @Valid @RequestBody CreateCouponIssuanceJobRequest request,
            Authentication authentication) throws IOException {

        String adminName = authentication.getName();

        CouponIssuanceJob savedJob = couponIssuanceService.createCouponIssuanceJob(
                request.originalFileName(),
                request.savedFilePath(),
                adminName
        );

        CreateCouponIssuanceJobResponse response = CreateCouponIssuanceJobResponse.from(savedJob);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * API 2: 작업 목록 조회
     * @return Job 리스트 (JSON)
     */
    @GetMapping
    public ResponseEntity<List<GetAllCouponIssuanceJobsResponse>> getAllCouponIssuanceJobs() {

        List<CouponIssuanceJob> jobs = couponIssuanceService.getAllCouponIssuanceJobs();

        List<GetAllCouponIssuanceJobsResponse> response = jobs.stream()
                .map(GetAllCouponIssuanceJobsResponse::from)
                .collect(Collectors.toList());

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

        CouponIssuanceService.FileDownloadInfo uploadedFile = couponIssuanceService.downloadCouponIssuanceFile(jobId);

        Resource resource = uploadedFile.resource();
        String originalFileName = uploadedFile.originalFileName(); // 원본 파일명으로 다운로드되도록 헤더 설정

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFileName + "\"")
                .body(resource);
    }

}
