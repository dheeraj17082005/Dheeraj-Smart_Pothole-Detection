package com.pothole.service.detection;

import com.pothole.dto.*;
import com.pothole.dto.ai.*;
import com.pothole.model.Detection;
import com.pothole.model.DetectionJob;
import com.pothole.model.Pothole;
import com.pothole.model.enums.DetectionJobStatus;
import com.pothole.model.enums.MediaType;
import com.pothole.model.enums.PotholeStatus;
import com.pothole.model.enums.ReportStatus;
import com.pothole.model.enums.SeverityClass;
import com.pothole.repository.DetectionJobRepository;
import com.pothole.repository.DetectionRepository;
import com.pothole.repository.MediaAssetRepository;
import com.pothole.repository.PotholeRepository;
import com.pothole.repository.ReportRepository;
import com.pothole.service.ai.AiDetectionService;
import com.pothole.service.storage.StorageService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class VideoDetectionWorkflowIT {

    @Container
    static PostgreSQLContainer<?> postgisContainer = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres")
    )
            .withDatabaseName("potholedb")
            .withUsername("pothole_user")
            .withPassword("pothole_password");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        if (postgisContainer.isRunning()) {
            registry.add("spring.datasource.url", postgisContainer::getJdbcUrl);
            registry.add("spring.datasource.username", postgisContainer::getUsername);
            registry.add("spring.datasource.password", postgisContainer::getPassword);
            registry.add("spring.flyway.enabled", () -> "true");
        }
    }

    @MockBean
    private StorageService storageService;

    @MockBean
    private AiDetectionService aiDetectionService;

    @Autowired
    private VideoDetectionService videoDetectionService;

    @Autowired
    private DetectionJobRepository detectionJobRepository;

    @Autowired
    private DetectionRepository detectionRepository;

    @Autowired
    private PotholeRepository potholeRepository;

    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private com.pothole.repository.CivicAuthorityRepository civicAuthorityRepository;

    @Autowired
    private EntityManager entityManager;

    // PWD Inner Ring Road segment coordinate (lat: 28.62, lon: 77.22)
    private static final double PWD_ROAD_LAT = 28.620000;
    private static final double PWD_ROAD_LON = 77.220000;
    private final Instant baseTime = Instant.parse("2026-09-11T10:00:00Z");

    @BeforeEach
    void setUpMocksAndDb() {
        reportRepository.deleteAll();
        detectionRepository.deleteAll();
        potholeRepository.deleteAll();
        detectionJobRepository.deleteAll();
        mediaAssetRepository.deleteAll();

        when(storageService.getRawBucket()).thenReturn("pothole-raw");
        when(storageService.getAnnotatedBucket()).thenReturn("pothole-annotated");
        when(storageService.generateRawObjectKey(any())).thenReturn("raw/test-video.mp4");
        when(storageService.generateAnnotatedObjectKey()).thenReturn("annotated/frame_key.jpg");
        when(storageService.generatePresignedUrl(anyString(), anyString())).thenReturn("http://localhost:9000/presigned/test.jpg");
    }

    @Test
    @DisplayName("End-to-end video detection workflow: upload -> 202 PENDING -> async processing -> aggregation -> persistence -> civic report")
    void testEndToEndVideoWorkflow() throws Exception {
        MockMultipartFile videoFile = new MockMultipartFile(
                "file", "dashcam.mp4", "video/mp4", new byte[]{0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70}
        );

        // Configure AI mock: 2 detections on consecutive frames belonging to the SAME physical pothole track
        AiFrameDetection frame0 = new AiFrameDetection(
                0, 0.0, 1920, 1080,
                List.of(new AiDetection(new AiBoundingBox(100, 100, 300, 300), 0.85, 3, "pothole", 0.04))
        );
        AiFrameDetection frame1 = new AiFrameDetection(
                1, 0.5, 1920, 1080,
                List.of(new AiDetection(new AiBoundingBox(105, 102, 305, 302), 0.92, 3, "pothole", 0.045))
        );
        AiVideoDetectionResponse aiVideoResp = new AiVideoDetectionResponse(
                new AiModelMetadata("vinothvikas1987/pothole-detection-yolov8", "YOLOv8s-RDD"),
                new AiVideoMetadata(30.0, 1.0, 2.0, 2),
                List.of(frame0, frame1),
                new AiRepresentativeFrame(1, 0.5, "highest_confidence_detection")
        );

        when(aiDetectionService.detectVideo(any(), any(), any(), any())).thenReturn(aiVideoResp);
        when(aiDetectionService.annotateVideoFrame(any(), any(), anyInt(), any()))
                .thenReturn(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});

        // 1. Submit video detection request
        DetectVideoAcceptedResponse acceptedResponse = videoDetectionService.initiateVideoJob(
                videoFile,
                PWD_ROAD_LAT,
                PWD_ROAD_LON,
                baseTime,
                "Ring Road Dashcam Video"
        );

        assertThat(acceptedResponse).isNotNull();
        assertThat(acceptedResponse.jobId()).isNotNull();
        assertThat(acceptedResponse.status()).isEqualTo(DetectionJobStatus.PENDING);
        assertThat(acceptedResponse.mediaAssetId()).isNotNull();
        assertThat(acceptedResponse.pollUrl()).isEqualTo("/api/v1/detection-jobs/" + acceptedResponse.jobId());

        // Verify initial MediaAsset and DetectionJob
        DetectionJob pendingJob = detectionJobRepository.findById(acceptedResponse.jobId()).orElseThrow();
        assertThat(pendingJob.getStatus()).isEqualTo(DetectionJobStatus.PENDING);
        com.pothole.model.MediaAsset mediaAsset = mediaAssetRepository.findById(acceptedResponse.mediaAssetId()).orElseThrow();
        assertThat(mediaAsset.getMediaType()).isEqualTo(MediaType.VIDEO);
        assertThat(mediaAsset.getMimeType()).isEqualTo("video/mp4");

        // Wait for async processing to complete (up to 8 seconds)
        Instant deadline = Instant.now().plusSeconds(8);
        DetectionJob completedJob = null;
        while (Instant.now().isBefore(deadline)) {
            completedJob = detectionJobRepository.findById(acceptedResponse.jobId()).orElseThrow();
            if (completedJob.getStatus() == DetectionJobStatus.COMPLETED || completedJob.getStatus() == DetectionJobStatus.FAILED) {
                break;
            }
            Thread.sleep(150);
        }

        assertThat(completedJob).isNotNull();
        assertThat(completedJob.getStatus()).isEqualTo(DetectionJobStatus.COMPLETED);
        assertThat(completedJob.getStartedAt()).isNotNull();
        assertThat(completedJob.getCompletedAt()).isNotNull();
        assertThat(completedJob.getResultSummary()).isNotNull();
        assertThat(completedJob.getResultSummary()).contains("\"potholesCreated\":1");
        assertThat(completedJob.getResultSummary()).contains("\"totalFramesSampled\":2");

        // 2. Verify Detections: exactly 2 detections persisted and linked to job
        List<Detection> jobDetections = detectionRepository.findByDetectionJobId(acceptedResponse.jobId());
        assertThat(jobDetections).hasSize(2);

        // Both detections should be linked to the single aggregated physical Pothole
        Pothole associatedPothole = jobDetections.get(0).getPothole();
        assertThat(associatedPothole).isNotNull();
        assertThat(jobDetections.get(1).getPothole().getId()).isEqualTo(associatedPothole.getId());

        // 3. Verify Pothole Entity
        Pothole savedPothole = potholeRepository.findById(associatedPothole.getId()).orElseThrow();
        assertThat(savedPothole.getIsDuplicate()).isFalse();
        assertThat(savedPothole.getStatus()).isEqualTo(PotholeStatus.REPORTED);
        assertThat(savedPothole.getSeverityClass()).isEqualTo(SeverityClass.MEDIUM);
        assertThat(savedPothole.getCivicAuthority()).isNotNull();
        com.pothole.model.CivicAuthority auth = civicAuthorityRepository.findById(savedPothole.getCivicAuthority().getId()).orElseThrow();
        assertThat(auth.getCode()).isEqualTo("DEMO_PWD_ARTERIAL");

        // 4. Verify Automated Civic Reporting (Step 15)
        var report = reportRepository.findByPotholeId(savedPothole.getId());
        assertThat(report).isPresent();
        assertThat(report.get().getStatus()).isEqualTo(ReportStatus.DISPATCHED);
        assertThat(report.get().getExternalReference()).contains("PWD-DEMO-2026-");

        // 5. Verify Polling API via videoDetectionService.getJobDetail
        JobDetailResponse jobDetails = videoDetectionService.getJobDetail(acceptedResponse.jobId());
        assertThat(jobDetails.jobId()).isEqualTo(acceptedResponse.jobId());
        assertThat(jobDetails.status()).isEqualTo(DetectionJobStatus.COMPLETED);
        assertThat(jobDetails.result()).isNotNull();
        assertThat(jobDetails.result().potholesCreated()).isEqualTo(1);
        assertThat(jobDetails.result().totalFramesSampled()).isEqualTo(2);
        assertThat(jobDetails.result().duplicatesDetected()).isEqualTo(0);
        assertThat(jobDetails.progress()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Second video submission nearby is recognized as Type B duplicate with report suppression")
    void testVideoDuplicateWorkflow() throws Exception {
        MockMultipartFile videoFile = new MockMultipartFile(
                "file", "dashcam.mp4", "video/mp4", new byte[]{1, 2, 3, 4}
        );

        AiFrameDetection frame0 = new AiFrameDetection(
                0, 0.0, 1920, 1080,
                List.of(new AiDetection(new AiBoundingBox(100, 100, 300, 300), 0.88, 3, "pothole", 0.04))
        );
        AiVideoDetectionResponse aiVideoResp = new AiVideoDetectionResponse(
                new AiModelMetadata("vinothvikas1987/pothole-detection-yolov8", "YOLOv8s-RDD"),
                new AiVideoMetadata(30.0, 0.5, 2.0, 1),
                List.of(frame0),
                new AiRepresentativeFrame(0, 0.0, "highest_confidence_detection")
        );

        when(aiDetectionService.detectVideo(any(), any(), any(), any())).thenReturn(aiVideoResp);
        when(aiDetectionService.annotateVideoFrame(any(), any(), anyInt(), any()))
                .thenReturn(new byte[]{0x01, 0x02});

        // 1. First submission (unique)
        DetectVideoAcceptedResponse resp1 = videoDetectionService.initiateVideoJob(
                videoFile, PWD_ROAD_LAT, PWD_ROAD_LON, baseTime, "Location 1"
        );

        // Wait for first job
        Instant deadline = Instant.now().plusSeconds(8);
        while (Instant.now().isBefore(deadline)) {
            DetectionJob job = detectionJobRepository.findById(resp1.jobId()).orElseThrow();
            if (job.getStatus() == DetectionJobStatus.COMPLETED) {
                break;
            }
            Thread.sleep(150);
        }

        List<Pothole> uniquePotholes = potholeRepository.findByIsDuplicateFalse();
        assertThat(uniquePotholes).hasSize(1);
        UUID primaryPotholeId = uniquePotholes.get(0).getId();

        // 2. Second video submission 1 day later at exact same location (Type B duplicate)
        DetectVideoAcceptedResponse resp2 = videoDetectionService.initiateVideoJob(
                videoFile, PWD_ROAD_LAT, PWD_ROAD_LON, baseTime.plus(1, ChronoUnit.DAYS), "Location 1 Repeat"
        );

        // Wait for second job
        deadline = Instant.now().plusSeconds(8);
        while (Instant.now().isBefore(deadline)) {
            DetectionJob job = detectionJobRepository.findById(resp2.jobId()).orElseThrow();
            if (job.getStatus() == DetectionJobStatus.COMPLETED) {
                break;
            }
            Thread.sleep(150);
        }

        // Verify Type B duplicate was recorded
        List<Pothole> duplicates = potholeRepository.findByDuplicateOfId(primaryPotholeId);
        assertThat(duplicates).hasSize(1);
        Pothole dup = duplicates.get(0);
        assertThat(dup.getIsDuplicate()).isTrue();
        assertThat(dup.getDuplicateOf().getId()).isEqualTo(primaryPotholeId);

        // Duplicate suppression: duplicate must NOT have a civic report
        var dupReport = reportRepository.findByPotholeId(dup.getId());
        assertThat(dupReport).isEmpty();

        // Job details summary should reflect 1 duplicate
        JobDetailResponse jobDetails = videoDetectionService.getJobDetail(resp2.jobId());
        assertThat(jobDetails.result().duplicatesDetected()).isEqualTo(1);
    }
}
