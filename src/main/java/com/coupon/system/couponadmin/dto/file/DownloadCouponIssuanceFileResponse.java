package com.coupon.system.couponadmin.dto.file;

import org.springframework.core.io.Resource;

public record DownloadCouponIssuanceFileResponse(
        Resource resource,
        String originalFileName
) {
}
