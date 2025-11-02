package com.coupon.system.couponadmin.service.file;

import com.coupon.system.couponadmin.dto.couponissurancejob.response.CreatePresignedUrlResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.time.Duration;

@Service
@Primary
public class S3Storage extends AbstractFileStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;

    public S3Storage(S3Client s3Client,
                     S3Presigner s3Presigner,
                     @Value("${aws.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    @Override
    public CreatePresignedUrlResponse createPresignedUrl(String originalFileName) {
        String s3ObjectKey = "uploads/" + generateUniqueFileName(originalFileName);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3ObjectKey)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10)) // 10분 유효
                .putObjectRequest(objectRequest)
                .build();

        String url = s3Presigner.presignPutObject(presignRequest).url().toString();

        return new CreatePresignedUrlResponse(url, s3ObjectKey);
    }

    @Override
    public String saveFile(MultipartFile file,
                           String uniqueFileName) throws IOException {

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
    public InputStream loadAsInputStream(String savedFilePath) throws IOException {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(savedFilePath)
                .build();
        return s3Client.getObject(request);
    }
}
