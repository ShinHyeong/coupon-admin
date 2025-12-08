package com.coupon.system.couponadmin.dto.couponissurancejob.response;

import com.coupon.system.couponadmin.domain.couponissurancejob.CouponIssuanceJob;

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
    public static CreateCouponIssuanceJobResponse from(CouponIssuanceJob entity) {
        return new CreateCouponIssuanceJobResponse(
                entity.getId(),
                entity.getOriginalFileName(),
                entity.getJobStatus().name(), // Enum -> String 변환
                entity.getAdminId(),
                entity.getCreatedAt()
        );
    }
}
