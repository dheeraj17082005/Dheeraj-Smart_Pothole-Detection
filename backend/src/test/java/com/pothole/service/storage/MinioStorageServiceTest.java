package com.pothole.service.storage;

import com.pothole.config.MinioProperties;
import com.pothole.exception.ObjectNotFoundException;
import com.pothole.exception.StorageException;
import com.pothole.exception.StorageUnavailableException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    private MinioProperties properties;
    private MinioStorageService storageService;

    @BeforeEach
    void setUp() {
        properties = new MinioProperties(
                "http://localhost:9000",
                null,
                "minioadmin",
                "minioadminpassword",
                "test-raw",
                "test-annotated",
                1800,
                true
        );
        storageService = new MinioStorageService(minioClient, properties);
    }

    @Test
    @DisplayName("initBuckets creates buckets if they do not exist")
    void testInitBucketsWhenNotExisting() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        storageService.initBuckets();

        verify(minioClient, times(2)).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, times(2)).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("initBuckets does nothing if buckets already exist")
    void testInitBucketsWhenAlreadyExisting() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        storageService.initBuckets();

        verify(minioClient, times(2)).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("putObject successfully uploads InputStream")
    void testPutObjectStream() throws Exception {
        byte[] data = "test image data".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream is = new ByteArrayInputStream(data);

        storageService.putObject("test-raw", "raw/2026/09/sample.jpg", is, data.length, "image/jpeg");

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());
        PutObjectArgs args = captor.getValue();
        assertThat(args.bucket()).isEqualTo("test-raw");
        assertThat(args.object()).isEqualTo("raw/2026/09/sample.jpg");
        assertThat(args.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("putObject successfully uploads byte array")
    void testPutObjectBytes() throws Exception {
        byte[] data = "sample bytes".getBytes(StandardCharsets.UTF_8);

        storageService.putObject("test-annotated", "annotated/2026/09/sample.jpg", data, "image/jpeg");

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());
        PutObjectArgs args = captor.getValue();
        assertThat(args.bucket()).isEqualTo("test-annotated");
        assertThat(args.object()).isEqualTo("annotated/2026/09/sample.jpg");
    }

    @Test
    @DisplayName("putObject validates parameters")
    void testPutObjectValidation() {
        byte[] data = new byte[]{1, 2, 3};
        assertThatThrownBy(() -> storageService.putObject("", "key", data, "image/jpeg"))
                .isInstanceOf(StorageException.class);
        assertThatThrownBy(() -> storageService.putObject("bucket", "", data, "image/jpeg"))
                .isInstanceOf(StorageException.class);
        assertThatThrownBy(() -> storageService.putObject("bucket", "key", (byte[]) null, "image/jpeg"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("getObject returns stream on success")
    void testGetObject() throws Exception {
        GetObjectResponse mockResponse = mock(GetObjectResponse.class);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(mockResponse);

        InputStream result = storageService.getObject("test-raw", "raw/sample.jpg");
        assertThat(result).isSameAs(mockResponse);
    }

    @Test
    @DisplayName("objectExists returns true when object exists")
    void testObjectExistsTrue() throws Exception {
        StatObjectResponse mockStat = mock(StatObjectResponse.class);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(mockStat);

        boolean exists = storageService.objectExists("test-raw", "raw/sample.jpg");
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("objectExists returns false when NoSuchKey is returned")
    void testObjectExistsFalse() throws Exception {
        ErrorResponse errorResponse = mock(ErrorResponse.class);
        when(errorResponse.code()).thenReturn("NoSuchKey");
        ErrorResponseException exception = new ErrorResponseException(errorResponse, null, null);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(exception);

        boolean exists = storageService.objectExists("test-raw", "raw/sample.jpg");
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("deleteObject calls minio removeObject")
    void testDeleteObject() throws Exception {
        storageService.deleteObject("test-raw", "raw/sample.jpg");

        ArgumentCaptor<RemoveObjectArgs> captor = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient).removeObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("test-raw");
        assertThat(captor.getValue().object()).isEqualTo("raw/sample.jpg");
    }

    @Test
    @DisplayName("generatePresignedUrl returns direct public URL using configured endpoint/publicUrl")
    void testGeneratePresignedUrl() {
        String url = storageService.generatePresignedUrl("test-raw", "raw/sample.jpg", 3600);
        assertThat(url).isEqualTo("http://localhost:9000/test-raw/raw/sample.jpg");
    }

    @Test
    @DisplayName("generatePresignedUrl formats publicUrl when configured")
    void testGeneratePresignedUrlWithPublicUrl() {
        MinioProperties customProps = new MinioProperties(
                "http://minio-internal:9000",
                "http://localhost:9000",
                "minioadmin",
                "minioadminpassword",
                "test-raw",
                "test-annotated",
                3600,
                false
        );
        MinioStorageService customService = new MinioStorageService(minioClient, customProps);

        String url = customService.generatePresignedUrl("test-raw", "raw/sample.jpg");
        assertThat(url).isEqualTo("http://localhost:9000/test-raw/raw/sample.jpg");
    }

    @Test
    @DisplayName("generateRawObjectKey generates key adhering to raw/{yyyy}/{MM}/{UUID}.{ext}")
    void testGenerateRawObjectKey() {
        LocalDate now = LocalDate.now();
        String year = String.format("%04d", now.getYear());
        String month = String.format("%02d", now.getMonthValue());

        String key = storageService.generateRawObjectKey("street_pothole.JPG");

        Pattern pattern = Pattern.compile("^raw/" + year + "/" + month + "/[0-9a-fA-F\\-]{36}\\.jpg$");
        assertThat(key).matches(pattern);
    }

    @Test
    @DisplayName("generateAnnotatedObjectKey generates key adhering to annotated/{yyyy}/{MM}/{UUID}.jpg")
    void testGenerateAnnotatedObjectKey() {
        LocalDate now = LocalDate.now();
        String year = String.format("%04d", now.getYear());
        String month = String.format("%02d", now.getMonthValue());

        String key = storageService.generateAnnotatedObjectKey();

        Pattern pattern = Pattern.compile("^annotated/" + year + "/" + month + "/[0-9a-fA-F\\-]{36}\\.jpg$");
        assertThat(key).matches(pattern);
    }

    @Test
    @DisplayName("getObject translates NoSuchKey to ObjectNotFoundException")
    void testGetObjectNotFound() throws Exception {
        ErrorResponse errorResponse = mock(ErrorResponse.class);
        when(errorResponse.code()).thenReturn("NoSuchKey");
        ErrorResponseException exception = new ErrorResponseException(errorResponse, null, null);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(exception);

        assertThatThrownBy(() -> storageService.getObject("test-raw", "missing.jpg"))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessageContaining("Object not found in bucket 'test-raw'");
    }

    @Test
    @DisplayName("translateException maps IOException to StorageUnavailableException")
    void testStorageUnavailable() throws Exception {
        when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(new IOException("Connection refused"));

        assertThatThrownBy(() -> storageService.getObject("test-raw", "sample.jpg"))
                .isInstanceOf(StorageUnavailableException.class)
                .hasMessageContaining("Storage communication failure");
    }
}
