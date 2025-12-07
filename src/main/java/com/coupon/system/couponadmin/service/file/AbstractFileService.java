package com.coupon.system.couponadmin.service.file;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.UUID;

public abstract class AbstractFileService implements FileService {

    @Override
    public final String save(MultipartFile file) throws IOException{
        String uniqueFileName = generateUniqueFileName(file.getOriginalFilename());
        return saveFile(file, uniqueFileName);
    }

    /**
     * 파일을 고유한 이름 정하는 규칙 결정 (공통로직)
     * @param originalFileName 원본 파일명
     * @return 파일별 고유한 이름 "UUID_원본파일명"
     */
    protected String generateUniqueFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            originalFileName = "file";
        }
        // "UUID_원본명" 정책
        return UUID.randomUUID().toString() + "_" + originalFileName;
    }

    /* 실제 저장 로직 (자식이 구현) */
    protected abstract String saveFile(MultipartFile file, String uniqueFileName) throws IOException;

    @Override
    public abstract Resource loadAsResource(String savedFilePath) throws MalformedURLException;

    @Override
    public abstract InputStream loadAsInputStream(String savedFilePath) throws IOException;
}
