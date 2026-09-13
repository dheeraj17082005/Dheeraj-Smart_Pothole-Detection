package com.pothole.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio")
public record MinioProperties(
        String endpoint,
        String publicUrl,
        String accessKey,
        String secretKey,
        String rawBucket,
        String annotatedBucket,
        int urlExpirySeconds,
        boolean autoCreateBuckets
) {
    public MinioProperties {
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = "http://localhost:9000";
        }
        if (publicUrl == null || publicUrl.isBlank()) {
            publicUrl = endpoint;
        }
        if (accessKey == null || accessKey.isBlank()) {
            accessKey = "minioadmin";
        }
        if (secretKey == null || secretKey.isBlank()) {
            secretKey = "minioadminpassword";
        }
        if (rawBucket == null || rawBucket.isBlank()) {
            rawBucket = "pothole-raw";
        }
        if (annotatedBucket == null || annotatedBucket.isBlank()) {
            annotatedBucket = "pothole-annotated";
        }
        if (urlExpirySeconds <= 0) {
            urlExpirySeconds = 3600;
        }
    }
}
