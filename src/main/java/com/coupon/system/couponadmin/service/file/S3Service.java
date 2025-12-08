package com.coupon.system.couponadmin.service.file;

import com.coupon.system.couponadmin.dto.couponissurancejob.response.GetPresignedUrlResponse;
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
import java.util.Map;

@Service
@Primary
public class S3Service extends AbstractFileService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;

    private static final Map<String, String> MIME_TYPES = Map.of(
            "csv", "text/csv",
            "xls", "application/vnd.ms-excel",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    public S3Service(S3Client s3Client,
                     S3Presigner s3Presigner,
                     @Value("${aws.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }



    /**
     * API 1: 클라우드 스토리지(S3) 파일 업로드를 위한 Presigned URL 생성
     * @return Presigned URL과 S3에 저장될 객체 키(경로)를 담은 응답 DTO
     */
    @Override
    public PresignedUrlInfo generatePresignedUrl(String fileName, String fileType) {
        String s3ObjectKey = "uploads/" + generateUniqueFileName(fileName);

        String contentType = determineContentType(fileType);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3ObjectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10)) // 10분 유효
                .putObjectRequest(objectRequest)
                .build();

        // S3Presigner를 사용하여 Presigned URL을 "생성"
        String url = s3Presigner.presignPutObject(presignRequest).url().toString();

        return new PresignedUrlInfo(url, s3ObjectKey);
    }

    // 헬퍼 메서드: 확장자/타입 문자열을 MIME 타입으로 변환
    private String determineContentType(String fileType) {
        if (fileType == null) return "application/octet-stream";
        String normalizedType = fileType.toLowerCase().replace(".", ""); // .csv -> csv
        return MIME_TYPES.getOrDefault(normalizedType, "application/octet-stream");
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
