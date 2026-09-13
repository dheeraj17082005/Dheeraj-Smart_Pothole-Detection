package com.pothole.service.storage;

import java.io.InputStream;

/**
 * Storage abstraction layer decoupling the application from the concrete storage provider (MinIO/S3).
 */
public interface StorageService {

    /**
     * Upload an object to storage from an InputStream.
     *
     * @param bucket destination bucket
     * @param key object key / path
     * @param inputStream data stream
     * @param size size of the data in bytes (-1 if unknown)
     * @param contentType MIME type of the data
     */
    void putObject(String bucket, String key, InputStream inputStream, long size, String contentType);

    /**
     * Upload an object to storage from a byte array.
     *
     * @param bucket destination bucket
     * @param key object key / path
     * @param content byte array content
     * @param contentType MIME type of the data
     */
    void putObject(String bucket, String key, byte[] content, String contentType);

    /**
     * Retrieve an object stream from storage.
     * Caller is responsible for closing the returned InputStream.
     *
     * @param bucket source bucket
     * @param key object key / path
     * @return InputStream of the object
     */
    InputStream getObject(String bucket, String key);

    /**
     * Retrieve an object as a byte array from storage.
     *
     * @param bucket source bucket
     * @param key object key / path
     * @return byte array content
     */
    byte[] getObjectBytes(String bucket, String key);

    /**
     * Check if an object exists in storage.
     *
     * @param bucket target bucket
     * @param key object key / path
     * @return true if object exists, false otherwise
     */
    boolean objectExists(String bucket, String key);

    /**
     * Delete an object from storage.
     *
     * @param bucket target bucket
     * @param key object key / path
     */
    void deleteObject(String bucket, String key);

    /**
     * Generate a presigned GET URL for an object with default expiry.
     *
     * @param bucket target bucket
     * @param key object key / path
     * @return presigned URL string
     */
    String generatePresignedUrl(String bucket, String key);

    /**
     * Generate a presigned GET URL for an object with specified expiry.
     *
     * @param bucket target bucket
     * @param key object key / path
     * @param expirySeconds expiration duration in seconds
     * @return presigned URL string
     */
    String generatePresignedUrl(String bucket, String key, int expirySeconds);

    /**
     * Generate a deterministic raw object key: raw/{yyyy}/{MM}/{uuid}.{ext}
     *
     * @param originalFilename original upload filename or null
     * @return storage key
     */
    String generateRawObjectKey(String originalFilename);

    /**
     * Generate a deterministic annotated object key: annotated/{yyyy}/{MM}/{uuid}.jpg
     *
     * @return storage key
     */
    String generateAnnotatedObjectKey();

    /**
     * Get configured raw bucket name.
     */
    String getRawBucket();

    /**
     * Get configured annotated bucket name.
     */
    String getAnnotatedBucket();
}
