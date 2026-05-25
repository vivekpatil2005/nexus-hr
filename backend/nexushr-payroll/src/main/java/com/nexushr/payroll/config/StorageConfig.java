package com.nexushr.payroll.config;

import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Value("${nexushr.storage.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${nexushr.storage.access-key:nexushr_admin}")
    private String accessKey;

    @Value("${nexushr.storage.secret-key:nexushr_minio_2026}")
    private String secretKey;

    @Value("${nexushr.storage.type:local}")
    private String storageType;

    @Bean
    public MinioClient minioClient() {
        if (!"minio".equalsIgnoreCase(storageType)) {
            log.info("MinIO storage is not selected (type={}). Skipping MinioClient bean creation.", storageType);
            return null;
        }
        try {
            return MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
        } catch (Exception e) {
            log.error("Failed to initialize MinIO Client. Fallback to local storage will be active.", e);
            return null;
        }
    }
}
