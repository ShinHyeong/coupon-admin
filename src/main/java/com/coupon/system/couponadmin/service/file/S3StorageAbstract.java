package com.coupon.system.couponadmin.service.file;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;


@Service
public class S3StorageAbstract extends AbstractFileStorage {

    @Override
    public String saveFile(MultipartFile file, String uniqueFileName) throws IOException {

        // (private final S3Client s3Client; ... 주입 필요)

        // ... (saveFile 구현) ...

        return "S3 URL";
    }

    @Override
    public Resource loadAsResource(String savedFilePath) throws MalformedURLException {

        // 예: S3Client를 사용하여 파일의 URL을 가져오거나 UrlResource 생성
        // return new UrlResource(s3Client.utilities().getUrl(...).toString());

        return null;
    }

    @Override
    public InputStream loadAsInputStream(String savedFilePath) throws IOException{

        // 예: S3Client를 사용하여 파일의 InputStream을 가져옴
        // GetObjectRequest request = GetObjectRequest.builder()...
        // return s3Client.getObject(request);

        return null;
    };
}
