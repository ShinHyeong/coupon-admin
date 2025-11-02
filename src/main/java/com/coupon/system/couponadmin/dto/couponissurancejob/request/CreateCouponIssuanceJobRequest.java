package com.coupon.system.couponadmin.dto.couponissurancejob.request;

public record CreateCouponIssuanceJobRequest(
        String originalFileName,
        String savedFilePath // S3 Object Key
) {
}
