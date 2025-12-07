package com.coupon.system.couponadmin.service.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class LocalService extends AbstractFileService {

    private final String uploadDir;
    public LocalService(@Value("${file.upload-dir}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public String saveFile(MultipartFile file, String uniqueFileName) throws IOException {

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath); // 폴더가 없으면 생성
        }

        Path destinationPath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), destinationPath); // 파일 저장

        return destinationPath.toString();
    }

    @Override
    public Resource loadAsResource(String savedFilePath) throws MalformedURLException {
        Path path = Paths.get(savedFilePath);
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("Could not read file: " + savedFilePath);
        }
        return resource;
    }

    @Override
    public InputStream loadAsInputStream(String savedFilePath) throws IOException{
        Path path = Paths.get(savedFilePath);
        return Files.newInputStream(path);
    };
}
