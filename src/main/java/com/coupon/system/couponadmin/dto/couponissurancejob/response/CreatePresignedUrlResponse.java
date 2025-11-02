package com.coupon.system.couponadmin.dto.couponissurancejob.response;

public record CreatePresignedUrlResponse(
        String presignedUrl,
        String savedFilePath // S3 Object Key
) {
}
