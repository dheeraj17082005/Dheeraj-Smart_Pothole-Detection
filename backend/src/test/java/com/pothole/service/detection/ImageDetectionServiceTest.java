package com.pothole.service.detection;

import com.pothole.dto.*;
import com.pothole.dto.ai.*;
import com.pothole.exception.AiServiceUnavailableException;
import com.pothole.exception.InvalidMediaException;
import com.pothole.exception.StorageException;
import com.pothole.model.*;
import com.pothole.model.enums.DetectionJobStatus;
import com.pothole.model.enums.MediaType;
import com.pothole.model.enums.PotholeStatus;
import com.pothole.model.enums.SeverityClass;
import com.pothole.service.ai.AiDetectionService;
import com.pothole.service.authority.AuthorityResolutionResult;
import com.pothole.service.authority.AuthorityResolverService;
import com.pothole.service.deduplication.DeduplicationResult;
import com.pothole.service.deduplication.DeduplicationService;
import com.pothole.service.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageDetectionServiceTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Mock
    private StorageService storageService;

    @Mock
    private AiDetectionService aiDetectionService;

    @Mock
    private ImageDetectionPersistenceService persistenceService;

    @Mock
    private AuthorityResolverService authorityResolverService;

    @Mock
    private DeduplicationService deduplicationService;

    @Mock
    private com.pothole.service.reporting.AuthorityReportingService authorityReportingService;

    private com.pothole.service.severity.SeverityService severityService;
    private ImageDetectionService imageDetectionService;

    @BeforeEach
    void setUp() {
        severityService = new com.pothole.service.severity.SeverityService();
        imageDetectionService = new ImageDetectionService(
                storageService,
                aiDetectionService,
                persistenceService,
                severityService,
                authorityResolverService,
                deduplicationService,
                authorityReportingService
        );
    }

    @Test
    @DisplayName("1 & 12. Successful image with one pothole creates Pothole, Detections, resolved Authority, and COMPLETED Job")
    void testSinglePotholeDetectionSuccess() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pothole.jpg", "image/jpeg", new byte[]{1, 2, 3, 4}
        );
        DetectImageRequest request = new DetectImageRequest(
                file, 12.9716, 77.5946, Instant.now(), "MG Road", 0.35
        );

        when(storageService.generateRawObjectKey(any())).thenReturn("raw/2026/09/uuid.jpg");
        when(storageService.getRawBucket()).thenReturn("pothole-raw");
        when(storageService.getAnnotatedBucket()).thenReturn("pothole-annotated");
        when(storageService.generateAnnotatedObjectKey()).thenReturn("annotated/2026/09/uuid.jpg");
        when(storageService.generatePresignedUrl(eq("pothole-annotated"), eq("annotated/2026/09/uuid.jpg")))
                .thenReturn("http://localhost:9000/annotated/2026/09/uuid.jpg?sig=xyz");

        AiImageDetectionResponse aiResponse = new AiImageDetectionResponse(
                new AiModelMetadata("vinothvikas1987/pothole-detection-yolov8", "YOLOv8s-RDD"),
                new AiImageMetadata(1920, 1080),
                1,
                0.885,
                0.042,
                List.of(new AiDetection(new AiBoundingBox(100, 100, 300, 300), 0.885, 3, "pothole", 0.042))
        );
        byte[] annotatedBytes = new byte[]{5, 6, 7, 8};
        when(aiDetectionService.detectAndAnnotateImage(any(), eq("pothole.jpg"), eq(0.35)))
                .thenReturn(new AiAnnotatedDetectionResult(aiResponse, annotatedBytes));

        CivicAuthority authority = new CivicAuthority("Delhi PWD", "PWD", "pwd@delhi.gov", "123", "PWD");
        authority.setId(UUID.randomUUID());
        when(authorityResolverService.resolveAuthority(12.9716, 77.5946))
                .thenReturn(AuthorityResolutionResult.resolved(authority));

        when(deduplicationService.evaluateDuplicate(eq(12.9716), eq(77.5946), any()))
                .thenReturn(DeduplicationResult.nonDuplicate());

        MediaAsset mediaAsset = new MediaAsset(MediaType.IMAGE, "raw/2026/09/uuid.jpg", "image/jpeg", 4L);
        mediaAsset.setId(UUID.randomUUID());
        DetectionJob detectionJob = new DetectionJob(mediaAsset, DetectionJobStatus.COMPLETED);
        detectionJob.setId(UUID.randomUUID());
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(77.5946, 12.9716));
        Pothole pothole = new Pothole(point, 37.17, SeverityClass.MEDIUM, 0.885, "annotated/2026/09/uuid.jpg");
        pothole.setId(UUID.randomUUID());
        pothole.setAddressText("MG Road");
        pothole.setCivicAuthority(authority);
        Detection detection = new Detection(detectionJob, 100, 100, 300, 300, 0.885, 0.042);
        detection.setId(UUID.randomUUID());
        detection.setPothole(pothole);
        PotholeStatusHistory history = new PotholeStatusHistory(pothole, null, PotholeStatus.REPORTED, "SYSTEM", "Initial");

        when(persistenceService.persistDetectionSuccess(any(), any(), anyLong(), eq(request), any(), any(), any(), eq(authority), any(), any(), any(), isNull()))
                .thenReturn(new ImageDetectionPersistenceService.SavedDetectionBundle(
                        mediaAsset, detectionJob, pothole, List.of(detection), history
                ));

        DetectImageResponse response = imageDetectionService.processImageDetection(request);

        assertThat(response).isNotNull();
        assertThat(response.potholeCreated()).isTrue();
        assertThat(response.potholeCount()).isEqualTo(1);
        assertThat(response.pothole().id()).isEqualTo(pothole.getId());
        assertThat(response.pothole().latitude()).isEqualTo(12.9716);
        assertThat(response.pothole().longitude()).isEqualTo(77.5946);
        assertThat(response.pothole().maxConfidence()).isEqualTo(0.885);
        assertThat(response.pothole().severityScore()).isEqualTo(37.17);
        assertThat(response.pothole().severityClass()).isEqualTo(SeverityClass.MEDIUM);
        assertThat(response.pothole().isDuplicate()).isFalse();
        assertThat(response.pothole().duplicateOfId()).isNull();
        assertThat(response.pothole().authority()).isNotNull();
        assertThat(response.pothole().authority().code()).isEqualTo("PWD");
        assertThat(response.pothole().authorityCode()).isEqualTo("PWD");
        assertThat(response.pothole().detections()).hasSize(1);
        assertThat(response.pothole().representativeImageUrl()).contains("http://localhost:9000/annotated/");
        assertThat(response.job().status()).isEqualTo(DetectionJobStatus.COMPLETED);

        verify(storageService).putObject(eq("pothole-raw"), eq("raw/2026/09/uuid.jpg"), any(InputStream.class), eq(4L), eq("image/jpeg"));
        verify(storageService).putObject(eq("pothole-annotated"), eq("annotated/2026/09/uuid.jpg"), eq(annotatedBytes), eq("image/jpeg"));
    }

    @Test
    @DisplayName("2. Successful image with multiple pothole detections aggregates into single Pothole event")
    void testMultiplePotholesDetectionSuccess() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "multi.jpg", "image/jpeg", new byte[]{1, 2, 3, 4}
        );
        DetectImageRequest request = new DetectImageRequest(
                file, 12.9716, 77.5946, null, "Outer Ring Road", null
        );

        when(storageService.generateRawObjectKey(any())).thenReturn("raw/2026/09/multi.jpg");
        when(storageService.getRawBucket()).thenReturn("pothole-raw");
        when(storageService.getAnnotatedBucket()).thenReturn("pothole-annotated");
        when(storageService.generateAnnotatedObjectKey()).thenReturn("annotated/2026/09/multi.jpg");

        AiImageDetectionResponse aiResponse = new AiImageDetectionResponse(
                new AiModelMetadata("vinothvikas1987/pothole-detection-yolov8", "YOLOv8s-RDD"),
                new AiImageMetadata(1920, 1080),
                2,
                0.91,
                0.08,
                List.of(
                        new AiDetection(new AiBoundingBox(100, 100, 300, 300), 0.91, 3, "pothole", 0.05),
                        new AiDetection(new AiBoundingBox(400, 400, 600, 600), 0.78, 3, "pothole", 0.03)
                )
        );
        when(aiDetectionService.detectAndAnnotateImage(any(), any(), any()))
                .thenReturn(new AiAnnotatedDetectionResult(aiResponse, new byte[]{1}));

        when(authorityResolverService.resolveAuthority(12.9716, 77.5946))
                .thenReturn(AuthorityResolutionResult.unknown());

        when(deduplicationService.evaluateDuplicate(eq(12.9716), eq(77.5946), any()))
                .thenReturn(DeduplicationResult.nonDuplicate());

        MediaAsset mediaAsset = new MediaAsset(MediaType.IMAGE, "raw/multi.jpg", "image/jpeg", 4L);
        mediaAsset.setId(UUID.randomUUID());
        DetectionJob job = new DetectionJob(mediaAsset, DetectionJobStatus.COMPLETED);
        job.setId(UUID.randomUUID());
        Pothole pothole = new Pothole(GEOMETRY_FACTORY.createPoint(new Coordinate(77.5946, 12.9716)), 68.9, SeverityClass.HIGH, 0.91, "annotated/multi.jpg");
        pothole.setId(UUID.randomUUID());

        Detection d1 = new Detection(job, 100, 100, 300, 300, 0.91, 0.05);
        d1.setId(UUID.randomUUID());
        Detection d2 = new Detection(job, 400, 400, 600, 600, 0.78, 0.03);
        d2.setId(UUID.randomUUID());

        when(persistenceService.persistDetectionSuccess(any(), any(), anyLong(), eq(request), any(), any(), any(), isNull(), any(), any(), any(), isNull()))
                .thenReturn(new ImageDetectionPersistenceService.SavedDetectionBundle(
                        mediaAsset, job, pothole, List.of(d1, d2), null
                ));

        DetectImageResponse response = imageDetectionService.processImageDetection(request);

        assertThat(response.potholeCount()).isEqualTo(2);
        assertThat(response.potholeCreated()).isTrue();
        assertThat(response.pothole().authority()).isNull();
        assertThat(response.pothole().authorityCode()).isEqualTo("UNKNOWN_AUTHORITY");
        assertThat(response.pothole().isDuplicate()).isFalse();
        assertThat(response.pothole().detections()).hasSize(2);
    }

    @Test
    @DisplayName("2a. Regression Test — Large detection array (11 potholes) is preserved without truncation across AI response -> DTO -> Pothole domain mapping")
    void testDetectionArrayPreservationNoTruncation() {
        MockMultipartFile file = new MockMultipartFile("file", "istockphoto.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});
        DetectImageRequest request = new DetectImageRequest(file, 37.7749, -122.4194, null, "Test Road", null);

        when(storageService.generateRawObjectKey(any())).thenReturn("raw/istockphoto.jpg");
        when(storageService.getRawBucket()).thenReturn("pothole-raw");
        when(storageService.getAnnotatedBucket()).thenReturn("pothole-annotated");
        when(storageService.generateAnnotatedObjectKey()).thenReturn("annotated/istockphoto.jpg");

        List<AiDetection> aiDetections = List.of(
                new AiDetection(new AiBoundingBox(295, 66, 375, 94), 0.9404, 0, "pothole", 0.0088),
                new AiDetection(new AiBoundingBox(249, 34, 328, 57), 0.9157, 0, "pothole", 0.0071),
                new AiDetection(new AiBoundingBox(102, 200, 360, 349), 0.8827, 0, "pothole", 0.1542),
                new AiDetection(new AiBoundingBox(365, 116, 569, 258), 0.7511, 0, "pothole", 0.1155),
                new AiDetection(new AiBoundingBox(173, 89, 387, 152), 0.6705, 0, "pothole", 0.0541),
                new AiDetection(new AiBoundingBox(174, 43, 250, 63), 0.6289, 0, "pothole", 0.0060),
                new AiDetection(new AiBoundingBox(115, 0, 186, 34), 0.4389, 0, "pothole", 0.0096),
                new AiDetection(new AiBoundingBox(251, 52, 321, 72), 0.4296, 0, "pothole", 0.0054),
                new AiDetection(new AiBoundingBox(166, 65, 251, 104), 0.3445, 0, "pothole", 0.0134),
                new AiDetection(new AiBoundingBox(134, 48, 182, 69), 0.2541, 0, "pothole", 0.0042),
                new AiDetection(new AiBoundingBox(369, 9, 477, 34), 0.1715, 0, "pothole", 0.0106)
        );

        AiImageDetectionResponse aiResponse = new AiImageDetectionResponse(
                new AiModelMetadata("custom-yolov8s-pothole", "YOLOv8s"),
                new AiImageMetadata(612, 408),
                11,
                0.9404,
                0.1542,
                aiDetections
        );

        when(aiDetectionService.detectAndAnnotateImage(any(), any(), any()))
                .thenReturn(new AiAnnotatedDetectionResult(aiResponse, new byte[]{1}));

        when(authorityResolverService.resolveAuthority(anyDouble(), anyDouble()))
                .thenReturn(AuthorityResolutionResult.unknown());

        when(deduplicationService.evaluateDuplicate(anyDouble(), anyDouble(), any()))
                .thenReturn(DeduplicationResult.nonDuplicate());

        MediaAsset mediaAsset = new MediaAsset(MediaType.IMAGE, "raw/istockphoto.jpg", "image/jpeg", 4L);
        mediaAsset.setId(UUID.randomUUID());
        DetectionJob job = new DetectionJob(mediaAsset, DetectionJobStatus.COMPLETED);
        job.setId(UUID.randomUUID());
        Pothole pothole = new Pothole(GEOMETRY_FACTORY.createPoint(new Coordinate(-122.4194, 37.7749)), 100.0, SeverityClass.HIGH, 0.9404, "annotated/istockphoto.jpg");
        pothole.setId(UUID.randomUUID());

        List<Detection> domainDetections = aiDetections.stream().map(aiDet -> {
            Detection d = new Detection(job, aiDet.box().xmin(), aiDet.box().ymin(), aiDet.box().xmax(), aiDet.box().ymax(), aiDet.confidence(), aiDet.visualAreaRatio());
            d.setId(UUID.randomUUID());
            d.setPothole(pothole);
            return d;
        }).toList();

        when(persistenceService.persistDetectionSuccess(any(), any(), anyLong(), eq(request), any(), any(), any(), isNull(), any(), any(), any(), isNull()))
                .thenReturn(new ImageDetectionPersistenceService.SavedDetectionBundle(
                        mediaAsset, job, pothole, domainDetections, null
                ));

        DetectImageResponse response = imageDetectionService.processImageDetection(request);

        assertThat(response.potholeCount()).isEqualTo(11);
        assertThat(response.pothole().detections()).hasSize(11);
        assertThat(response.pothole().detections()).extracting(DetectionResponse::confidence)
                .containsExactly(0.9404, 0.9157, 0.8827, 0.7511, 0.6705, 0.6289, 0.4389, 0.4296, 0.3445, 0.2541, 0.1715);
    }

    @Test
    @DisplayName("2b. Duplicate pothole detection marks isDuplicate=true and sets duplicateOfId")
    void testDuplicatePotholeDetectionSuccess() {
        MockMultipartFile file = new MockMultipartFile("file", "dup.jpg", "image/jpeg", new byte[]{1, 2, 3});
        DetectImageRequest request = new DetectImageRequest(file, 12.9716, 77.5946, null, "MG Road", null);

        when(storageService.generateRawObjectKey(any())).thenReturn("raw/dup.jpg");
        when(storageService.getRawBucket()).thenReturn("pothole-raw");
        when(storageService.getAnnotatedBucket()).thenReturn("pothole-annotated");
        when(storageService.generateAnnotatedObjectKey()).thenReturn("annotated/dup.jpg");

        AiImageDetectionResponse aiResponse = new AiImageDetectionResponse(
                new AiModelMetadata("vinothvikas1987/pothole-detection-yolov8", "YOLOv8s-RDD"),
                new AiImageMetadata(1920, 1080),
                1,
                0.88,
                0.04,
                List.of(new AiDetection(new AiBoundingBox(100, 100, 300, 300), 0.88, 3, "pothole", 0.04))
        );
        when(aiDetectionService.detectAndAnnotateImage(any(), any(), any()))
                .thenReturn(new AiAnnotatedDetectionResult(aiResponse, new byte[]{1}));

        when(authorityResolverService.resolveAuthority(12.9716, 77.5946))
                .thenReturn(AuthorityResolutionResult.unknown());

        UUID parentId = UUID.randomUUID();
        Pothole parentPothole = new Pothole(GEOMETRY_FACTORY.createPoint(new Coordinate(77.5946, 12.9716)), 35.0, SeverityClass.MEDIUM, 0.88, "ann/parent.jpg");
        parentPothole.setId(parentId);

        when(deduplicationService.evaluateDuplicate(eq(12.9716), eq(77.5946), any()))
                .thenReturn(DeduplicationResult.duplicate(parentPothole));

        MediaAsset mediaAsset = new MediaAsset(MediaType.IMAGE, "raw/dup.jpg", "image/jpeg", 3L);
        mediaAsset.setId(UUID.randomUUID());
        DetectionJob job = new DetectionJob(mediaAsset, DetectionJobStatus.COMPLETED);
        job.setId(UUID.randomUUID());

        Pothole duplicatePothole = new Pothole(GEOMETRY_FACTORY.createPoint(new Coordinate(77.5946, 12.9716)), 35.0, SeverityClass.MEDIUM, 0.88, "annotated/dup.jpg");
        duplicatePothole.setId(UUID.randomUUID());
        duplicatePothole.setIsDuplicate(true);
        duplicatePothole.setDuplicateOf(parentPothole);

        Detection detection = new Detection(job, 100, 100, 300, 300, 0.88, 0.04);
        detection.setId(UUID.randomUUID());
        detection.setPothole(duplicatePothole);

        when(persistenceService.persistDetectionSuccess(any(), any(), anyLong(), eq(request), any(), any(), any(), isNull(), any(), any(), any(), isNull()))
                .thenReturn(new ImageDetectionPersistenceService.SavedDetectionBundle(
                        mediaAsset, job, duplicatePothole, List.of(detection), null
                ));

        DetectImageResponse response = imageDetectionService.processImageDetection(request);

        assertThat(response.potholeCreated()).isTrue();
        assertThat(response.pothole().isDuplicate()).isTrue();
        assertThat(response.pothole().duplicateOfId()).isEqualTo(parentId);
    }

    @Test
    @DisplayName("3 & 15. Image with zero potholes completes job and does NOT create Pothole")
    void testZeroPotholesNoPotholeCreated() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "clean.jpg", "image/jpeg", new byte[]{1, 2, 3, 4}
        );
        DetectImageRequest request = new DetectImageRequest(
                file, 12.9716, 77.5946, null, "Highway", null
        );

        when(storageService.generateRawObjectKey(any())).thenReturn("raw/clean.jpg");
        when(storageService.getRawBucket()).thenReturn("pothole-raw");

        AiImageDetectionResponse aiResponse = new AiImageDetectionResponse(
                new AiModelMetadata("vinothvikas1987/pothole-detection-yolov8", "YOLOv8s-RDD"),
                new AiImageMetadata(1920, 1080),
                0,
                0.0,
                0.0,
                List.of()
        );
        when(aiDetectionService.detectAndAnnotateImage(any(), any(), any()))
                .thenReturn(new AiAnnotatedDetectionResult(aiResponse, new byte[]{1}));

        MediaAsset mediaAsset = new MediaAsset(MediaType.IMAGE, "raw/clean.jpg", "image/jpeg", 4L);
        mediaAsset.setId(UUID.randomUUID());
        DetectionJob job = new DetectionJob(mediaAsset, DetectionJobStatus.COMPLETED);
        job.setId(UUID.randomUUID());

        when(persistenceService.persistNoPotholeFound(any(), any(), anyLong(), any(), any()))
                .thenReturn(new ImageDetectionPersistenceService.SavedDetectionBundle(
                        mediaAsset, job, null, List.of(), null
                ));

        DetectImageResponse response = imageDetectionService.processImageDetection(request);

        assertThat(response.potholeCount()).isEqualTo(0);
        assertThat(response.potholeCreated()).isFalse();
        assertThat(response.pothole()).isNull();
        assertThat(response.job().status()).isEqualTo(DetectionJobStatus.COMPLETED);
        assertThat(response.message()).contains("No potholes detected");

        verify(storageService, never()).putObject(eq("pothole-annotated"), anyString(), any(byte[].class), anyString());
    }

    @Test
    @DisplayName("4. Invalid coordinates throw InvalidMediaException")
    void testInvalidCoordinates() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1});
        DetectImageRequest invalidLat = new DetectImageRequest(file, 95.0, 77.0, null, null, null);
        assertThatThrownBy(() -> imageDetectionService.processImageDetection(invalidLat))
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("Latitude must be between -90.0 and 90.0");

        DetectImageRequest invalidLon = new DetectImageRequest(file, 12.0, 195.0, null, null, null);
        assertThatThrownBy(() -> imageDetectionService.processImageDetection(invalidLon))
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("Longitude must be between -180.0 and 180.0");
    }

    @Test
    @DisplayName("5. Missing or empty file throws InvalidMediaException")
    void testMissingFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{});
        DetectImageRequest request = new DetectImageRequest(emptyFile, 12.0, 77.0, null, null, null);
        assertThatThrownBy(() -> imageDetectionService.processImageDetection(request))
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("Image file must not be empty");
    }

    @Test
    @DisplayName("6. Unsupported image type throws InvalidMediaException")
    void testUnsupportedImageType() {
        MockMultipartFile textFile = new MockMultipartFile("file", "test.txt", "text/plain", new byte[]{1, 2, 3});
        DetectImageRequest request = new DetectImageRequest(textFile, 12.0, 77.0, null, null, null);
        assertThatThrownBy(() -> imageDetectionService.processImageDetection(request))
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("Unsupported image content type");
    }

    @Test
    @DisplayName("7 & 10. AI service failure marks DetectionJob as FAILED and rethrows exception")
    void testAiServiceFailure() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3});
        DetectImageRequest request = new DetectImageRequest(file, 12.0, 77.0, null, null, null);

        when(storageService.generateRawObjectKey(any())).thenReturn("raw/test.jpg");
        when(storageService.getRawBucket()).thenReturn("pothole-raw");

        when(aiDetectionService.detectAndAnnotateImage(any(), any(), any()))
                .thenThrow(new AiServiceUnavailableException("AI service down"));

        assertThatThrownBy(() -> imageDetectionService.processImageDetection(request))
                .isInstanceOf(AiServiceUnavailableException.class)
                .hasMessageContaining("AI service down");

        verify(persistenceService).persistDetectionFailure(
                eq("raw/test.jpg"), eq("image/jpeg"), eq(3L), any(Instant.class), eq("AI service down")
        );
    }

    @Test
    @DisplayName("8. MinIO storage failure throws StorageException before AI call")
    void testMinioFailure() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3});
        DetectImageRequest request = new DetectImageRequest(file, 12.0, 77.0, null, null, null);

        when(storageService.generateRawObjectKey(any())).thenReturn("raw/test.jpg");
        when(storageService.getRawBucket()).thenReturn("pothole-raw");
        doThrow(new StorageException("MinIO bucket unreachable", "STORAGE_UNAVAILABLE"))
                .when(storageService).putObject(anyString(), anyString(), any(InputStream.class), anyLong(), anyString());

        assertThatThrownBy(() -> imageDetectionService.processImageDetection(request))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("MinIO bucket unreachable");

        verifyNoInteractions(aiDetectionService);
    }
}
