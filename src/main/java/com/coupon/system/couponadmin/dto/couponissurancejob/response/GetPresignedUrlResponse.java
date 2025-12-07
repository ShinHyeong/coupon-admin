package com.coupon.system.couponadmin.dto.couponissurancejob.response;

public record GetPresignedUrlResponse(
        String presignedUrl,
        String savedFilePath // S3 Object Key
) {
}
