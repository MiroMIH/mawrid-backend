package com.mawrid.storage;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(MinioClient.class)
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${app.minio.bucket}")
    private String bucket;

    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket '{}' created", bucket);
            }
        } catch (Exception ex) {
            log.warn("Could not initialize MinIO bucket '{}': {}", bucket, ex.getMessage());
        }
    }

    public String upload(MultipartFile file, String folder) {
        try {
            String objectName = folder + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.debug("Uploaded file to MinIO: {}", objectName);
            return objectName;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to upload file to storage: " + ex.getMessage(), ex);
        }
    }

    public String getPresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate presigned URL: " + ex.getMessage(), ex);
        }
    }

    public void delete(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception ex) {
            log.error("Failed to delete object '{}' from MinIO: {}", objectName, ex.getMessage());
        }
    }
}
