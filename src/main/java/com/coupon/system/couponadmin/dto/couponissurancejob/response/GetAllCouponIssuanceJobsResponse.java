package com.coupon.system.couponadmin.dto.couponissurancejob.response;

import java.time.LocalDateTime;

/**
 * main.html의 테이블에 필요한 모든 정보를 담는다.
 */
public record GetAllCouponIssuanceJobsResponse(
        Long jobId,
        String originalFileName,
        String jobStatus,
        int totalCount,
        int successCount,
        int failCount,
        LocalDateTime createdAt
) {
}
