package com.nexushr.payroll.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    @Autowired(required = false)
    private MinioClient minioClient;

    @Value("${nexushr.storage.type:local}")
    private String storageType;

    @Value("${nexushr.storage.bucket:nexushr-documents}")
    private String bucketName;

    @Value("${nexushr.storage.local-dir:./payslips}")
    private String localDir;

    private boolean isMinioActive;

    @PostConstruct
    public void init() {
        isMinioActive = "minio".equalsIgnoreCase(storageType) && minioClient != null;
        if (isMinioActive) {
            try {
                boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
                if (!found) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                    log.info("Created MinIO bucket: {}", bucketName);
                } else {
                    log.info("MinIO bucket {} already exists.", bucketName);
                }
            } catch (Exception e) {
                log.error("Failed to verify/create MinIO bucket. Falling back to local storage.", e);
                isMinioActive = false;
            }
        }
        
        // Ensure local fallback directory exists
        try {
            Files.createDirectories(Paths.get(localDir));
            log.info("Local storage directory initialized at: {}", Paths.get(localDir).toAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to create local storage directory at {}", localDir, e);
        }
    }

    public String uploadFile(String fileName, byte[] content, String contentType) {
        if (isMinioActive) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(content);
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fileName)
                                .stream(bais, content.length, -1)
                                .contentType(contentType)
                                .build()
                );
                log.info("Uploaded file to MinIO: {}", fileName);
                return fileName;
            } catch (Exception e) {
                log.error("MinIO upload failed for {}, falling back to local filesystem", fileName, e);
            }
        }

        // Local filesystem fallback
        try {
            Path targetPath = Paths.get(localDir, fileName);
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, content);
            log.info("Saved file locally: {}", targetPath.toAbsolutePath());
            return fileName;
        } catch (Exception e) {
            log.error("Local file save failed for {}", fileName, e);
            throw new RuntimeException("Failed to store file: " + fileName, e);
        }
    }

    public byte[] downloadFile(String key) {
        if (isMinioActive) {
            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .build()
            )) {
                return stream.readAllBytes();
            } catch (Exception e) {
                log.error("MinIO download failed for key: {}, trying local fallback", key, e);
            }
        }

        // Local filesystem fallback
        try {
            Path filePath = Paths.get(localDir, key);
            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            } else {
                throw new RuntimeException("File not found on local storage: " + key);
            }
        } catch (Exception e) {
            log.error("Local download failed for key: {}", key, e);
            throw new RuntimeException("Failed to retrieve file for key: " + key, e);
        }
    }

    public String generatePresignedUrl(String key) {
        if (isMinioActive) {
            try {
                return minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(bucketName)
                                .object(key)
                                .expiry(24, TimeUnit.HOURS)
                                .build()
                );
            } catch (Exception e) {
                log.error("Failed to generate presigned URL for key: {}", key, e);
            }
        }
        // Fallback or local: Return the backend API path for downloading
        return "/api/payroll/payslips/download-by-key?key=" + key;
    }
}
