package com.coupon.system.couponadmin.dto.couponissurancejob.response;

import com.coupon.system.couponadmin.service.file.PresignedUrlInfo;

public record GetPresignedUrlResponse(
        String presignedUrl,
        String savedFilePath // S3 Object Key
){
    public static GetPresignedUrlResponse from(PresignedUrlInfo info) {
        return new GetPresignedUrlResponse(info.url(), info.savedFilePath());
    }
}