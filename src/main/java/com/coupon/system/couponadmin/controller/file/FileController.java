package com.coupon.system.couponadmin.controller.file;

import com.coupon.system.couponadmin.dto.couponissurancejob.response.GetPresignedUrlResponse;
import com.coupon.system.couponadmin.dto.file.DownloadCouponIssuanceFileResponse;
import com.coupon.system.couponadmin.exception.coupon.InvalidFileException;
import com.coupon.system.couponadmin.service.file.FileService;
import com.coupon.system.couponadmin.service.file.PresignedUrlInfo;
import jakarta.validation.constraints.NotBlank;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * API 1: S3 업로드를 위한 Presigned URL 가져오는 요청
     * @return Presigned URL, S3 파일 경로
     */
    @GetMapping("/presigned-url")
    public ResponseEntity<GetPresignedUrlResponse> getPresignedUrl(
            @RequestParam @NotBlank(message = "파일명은 필수입니다.") String fileName,
            @RequestParam @NotBlank(message = "파일 타입은 필수입니다.") String fileType
    ) {
        validateFileExtension(fileName);

        PresignedUrlInfo info = fileService.generatePresignedUrl(fileName, fileType);
        return ResponseEntity.ok(GetPresignedUrlResponse.from(info));
    }

    private void validateFileExtension(String fileName) {
        // 1. 확장자 추출
        String ext = org.springframework.util.StringUtils.getFilenameExtension(fileName);

        // 2. 검증
        if (ext == null || !isValidExtension(ext)) {
            throw new InvalidFileException("지원하지 않는 파일 형식입니다. (csv, xls, xlsx만 가능)");
        }
    }

    private boolean isValidExtension(String ext) {
        return List.of("csv", "xls", "xlsx").contains(ext.toLowerCase());
    }
}
