package com.coupon.system.couponadmin.service.file;

import com.coupon.system.couponadmin.dto.couponissurancejob.response.GetPresignedUrlResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

public interface FileStorage {
    /**
     * 저장소별 저장 절차 조율
     * 1. DB에 저장할 [파일별 고유한 이름]을 정한다
     * 2. 실제 저장 로직을 호출한다.
     * @param file 저장할 파일
     * @return 실제로 저장된 최종 경로 (e.g., S3 URL)
     */
    String save(MultipartFile file) throws IOException;

    /* 저장된 파일 다운로드할 수 있는 Resource로 로드 */
    Resource loadAsResource(String savedFilePath) throws MalformedURLException;

    /* 저장된 파일을 '처리'할 수 있는 InputStream으로 로드 */
    InputStream loadAsInputStream(String savedFilePath) throws IOException;

    /**
     * Client-side Upload를 위한 Presigned URL을 생성함
     * 클라우드 스토리지(S3 등)에서만 지원됨
     * @param fileName 원본 파일명
     * @return Presigned URL, 저장될 파일 경로(Key)가 담긴 DTO
     */
    default GetPresignedUrlResponse getPresignedUrl(String fileName) {
        //이 저장소 (Ex. 로컬저장소)는 Presigned URL을 지원하지 않는다는 메세지와 함께 예외처리
        throw new UnsupportedOperationException("Presigned URL generation is not supported by this storage type.");
    }
}
