package com.coupon.system.couponadmin.service.file;

public record PresignedUrlInfo(
        String url,
        String savedFilePath
) {}