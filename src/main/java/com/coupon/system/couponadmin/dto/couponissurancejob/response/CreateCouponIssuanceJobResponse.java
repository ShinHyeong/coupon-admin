package com.coupon.system.couponadmin.dto.couponissurancejob.response;

import java.time.LocalDateTime;

/**
 * 생성 직후 최소한의 정보만 담았다.
 */
public record CreateCouponIssuanceJobResponse(
        Long jobId,
        String originalFileName,
        String jobStatus,
        Long adminId, //해당 파일을 올린 관리자id (FK)
        LocalDateTime createdAt
) {
}
