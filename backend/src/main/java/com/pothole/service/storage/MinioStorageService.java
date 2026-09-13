package com.pothole.service.storage;

import com.pothole.config.MinioProperties;
import com.pothole.exception.ObjectNotFoundException;
import com.pothole.exception.StorageException;
import com.pothole.exception.StorageUnavailableException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MinioStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);
    private static final long DEFAULT_MULTIPART_PART_SIZE = 10L * 1024 * 1024; // 10MB

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public MinioStorageService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @PostConstruct
    public void initBuckets() {
        if (!properties.autoCreateBuckets()) {
            return;
        }
        try {
            ensureBucketExists(properties.rawBucket());
            ensureBucketExists(properties.annotatedBucket());
        } catch (Exception e) {
            log.warn("Could not auto-initialize MinIO buckets (storage might be offline): {}", e.getMessage());
        }
    }

    public void ensureBucketExists(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Successfully created MinIO bucket: {}", bucketName);
            }
            String policy = """
                    {
                      "Version": "2012-10-17",
                      "Statement": [
                        {
                          "Effect": "Allow",
                          "Principal": "*",
                          "Action": ["s3:GetObject"],
                          "Resource": ["arn:aws:s3:::%s/*"]
                        }
                      ]
                    }
                    """.formatted(bucketName);
            try {
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build()
                );
                log.info("Configured read policy on MinIO bucket: {}", bucketName);
            } catch (Exception pe) {
                log.debug("Bucket policy notice for '{}': {}", bucketName, pe.getMessage());
            }
        } catch (Exception e) {
            log.warn("Failed to check/create bucket '{}': {}", bucketName, e.getMessage());
            throw translateException(e, bucketName, "");
        }
    }

    @Override
    public void putObject(String bucket, String key, InputStream inputStream, long size, String contentType) {
        if (bucket == null || bucket.isBlank()) {
            throw new StorageException("Bucket name must not be empty", "INVALID_BUCKET");
        }
        if (key == null || key.isBlank()) {
            throw new StorageException("Object key must not be empty", "INVALID_KEY");
        }
        if (inputStream == null) {
            throw new StorageException("Input stream must not be null", "INVALID_PAYLOAD");
        }

        long partSize = (size <= 0) ? DEFAULT_MULTIPART_PART_SIZE : -1;
        String resolvedContentType = (contentType != null && !contentType.isBlank())
                ? contentType
                : "application/octet-stream";

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .stream(inputStream, size, partSize)
                            .contentType(resolvedContentType)
                            .build()
            );
            log.debug("Stored object in [{}/{}], size={} bytes", bucket, key, size);
        } catch (Exception e) {
            throw translateException(e, bucket, key);
        }
    }

    @Override
    public void putObject(String bucket, String key, byte[] content, String contentType) {
        if (content == null) {
            throw new StorageException("Content byte array must not be null", "INVALID_PAYLOAD");
        }
        putObject(bucket, key, new ByteArrayInputStream(content), content.length, contentType);
    }

    @Override
    public InputStream getObject(String bucket, String key) {
        if (bucket == null || bucket.isBlank()) {
            throw new StorageException("Bucket name must not be empty", "INVALID_BUCKET");
        }
        if (key == null || key.isBlank()) {
            throw new StorageException("Object key must not be empty", "INVALID_KEY");
        }

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
        } catch (Exception e) {
            throw translateException(e, bucket, key);
        }
    }

    @Override
    public byte[] getObjectBytes(String bucket, String key) {
        try (InputStream is = getObject(bucket, key)) {
            return is.readAllBytes();
        } catch (IOException e) {
            throw new StorageException("Failed to read object content for [" + bucket + "/" + key + "]", "IO_ERROR", e);
        }
    }

    @Override
    public boolean objectExists(String bucket, String key) {
        if (bucket == null || bucket.isBlank() || key == null || key.isBlank()) {
            return false;
        }

        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
            return true;
        } catch (ErrorResponseException ere) {
            String code = ere.errorResponse().code();
            if ("NoSuchKey".equalsIgnoreCase(code) || "ResourceNotFound".equalsIgnoreCase(code) || "NoSuchBucket".equalsIgnoreCase(code)) {
                return false;
            }
            throw translateException(ere, bucket, key);
        } catch (Exception e) {
            throw translateException(e, bucket, key);
        }
    }

    @Override
    public void deleteObject(String bucket, String key) {
        if (bucket == null || bucket.isBlank()) {
            throw new StorageException("Bucket name must not be empty", "INVALID_BUCKET");
        }
        if (key == null || key.isBlank()) {
            throw new StorageException("Object key must not be empty", "INVALID_KEY");
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
            log.debug("Deleted object from [{}/{}]", bucket, key);
        } catch (Exception e) {
            throw translateException(e, bucket, key);
        }
    }

    @Override
    public String generatePresignedUrl(String bucket, String key) {
        return generatePresignedUrl(bucket, key, properties.urlExpirySeconds());
    }

    @Override
    public String generatePresignedUrl(String bucket, String key, int expirySeconds) {
        if (bucket == null || bucket.isBlank()) {
            throw new StorageException("Bucket name must not be empty", "INVALID_BUCKET");
        }
        if (key == null || key.isBlank()) {
            throw new StorageException("Object key must not be empty", "INVALID_KEY");
        }

        if (properties.publicUrl() != null && !properties.publicUrl().isBlank()) {
            String baseUrl = properties.publicUrl().replaceAll("/+$", "");
            return String.format("%s/%s/%s", baseUrl, bucket, key);
        }

        int effectiveExpiry = (expirySeconds > 0) ? expirySeconds : properties.urlExpirySeconds();

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(key)
                            .expiry(effectiveExpiry, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            throw translateException(e, bucket, key);
        }
    }

    @Override
    public String generateRawObjectKey(String originalFilename) {
        LocalDate now = LocalDate.now();
        String year = String.format("%04d", now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        String uuid = UUID.randomUUID().toString();
        String extension = extractExtension(originalFilename);
        return String.format("raw/%s/%s/%s%s", year, month, uuid, extension);
    }

    @Override
    public String generateAnnotatedObjectKey() {
        LocalDate now = LocalDate.now();
        String year = String.format("%04d", now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        String uuid = UUID.randomUUID().toString();
        return String.format("annotated/%s/%s/%s.jpg", year, month, uuid);
    }

    @Override
    public String getRawBucket() {
        return properties.rawBucket();
    }

    @Override
    public String getAnnotatedBucket() {
        return properties.annotatedBucket();
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < filename.length() - 1) {
            String ext = filename.substring(lastDot).toLowerCase().replaceAll("[^a-z0-9.]", "");
            return ext.length() <= 10 ? ext : "";
        }
        return "";
    }

    private RuntimeException translateException(Exception e, String bucket, String key) {
        if (e instanceof StorageException se) {
            return se;
        }
        if (e instanceof ErrorResponseException ere) {
            String code = ere.errorResponse().code();
            if ("NoSuchKey".equalsIgnoreCase(code) || "ResourceNotFound".equalsIgnoreCase(code)) {
                return new ObjectNotFoundException("Object not found in bucket '" + bucket + "': " + key, ere);
            }
            if ("NoSuchBucket".equalsIgnoreCase(code)) {
                return new ObjectNotFoundException("Bucket not found: " + bucket, ere);
            }
        }
        if (e instanceof IOException || (e.getCause() != null && e.getCause() instanceof IOException)) {
            return new StorageUnavailableException("Storage communication failure for [" + bucket + "/" + key + "]: " + e.getMessage(), e);
        }
        return new StorageException("Storage operation failed for [" + bucket + "/" + key + "]: " + e.getMessage(), "STORAGE_ERROR", e);
    }
}
