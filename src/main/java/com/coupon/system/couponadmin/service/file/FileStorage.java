package com.coupon.system.couponadmin.service.file;

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
}
