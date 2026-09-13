package com.pothole.service.detection;

import com.pothole.dto.*;
import com.pothole.dto.ai.*;
import com.pothole.model.Pothole;
import com.pothole.model.enums.PotholeStatus;
import com.pothole.model.enums.SeverityClass;
import com.pothole.repository.PotholeRepository;
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
import org.springframework.transaction.annotation.Transactional;
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
@Transactional
class ImageDetectionWorkflowIT {

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
    private ImageDetectionService imageDetectionService;

    @Autowired
    private PotholeRepository potholeRepository;

    @Autowired
    private com.pothole.repository.ReportRepository reportRepository;

    @Autowired
    private EntityManager entityManager;

    // PWD Inner Ring Road segment coordinate (lat: 28.62, lon: 77.22)
    private static final double PWD_ROAD_LAT = 28.620000;
    private static final double PWD_ROAD_LON = 77.220000;
    private final Instant baseTime = Instant.parse("2026-09-11T10:00:00Z");

    @BeforeEach
    void setUpMocksAndDb() {
        potholeRepository.deleteAll();
        entityManager.flush();

        when(storageService.getRawBucket()).thenReturn("pothole-raw");
        when(storageService.getAnnotatedBucket()).thenReturn("pothole-annotated");
        when(storageService.generateRawObjectKey(any())).thenReturn("raw/test.jpg");
        when(storageService.generateAnnotatedObjectKey()).thenReturn("annotated/test.jpg");
        when(storageService.generatePresignedUrl(anyString(), anyString())).thenReturn("http://localhost:9000/presigned/test.jpg");

        AiImageDetectionResponse aiResp = new AiImageDetectionResponse(
                new AiModelMetadata("vinothvikas1987/pothole-detection-yolov8", "YOLOv8s-RDD"),
                new AiImageMetadata(1920, 1080),
                1,
                0.885,
                0.042,
                List.of(new AiDetection(new AiBoundingBox(100, 100, 300, 300), 0.885, 3, "pothole", 0.042))
        );
        when(aiDetectionService.detectAndAnnotateImage(any(), any(), any()))
                .thenReturn(new AiAnnotatedDetectionResult(aiResp, new byte[]{1, 2, 3}));
    }

    @Test
    @DisplayName("End-to-end duplicate workflow: Request A (primary) -> Request B (duplicate) -> Resolved parent check -> Far location check")
    void testEndToEndDuplicateDetectionWorkflow() {
        MockMultipartFile imageFile = new MockMultipartFile(
                "file", "road.jpg", "image/jpeg", new byte[]{10, 20, 30, 40}
        );

        // 1. Request A: Initial submission at PWD road location
        DetectImageRequest requestA = new DetectImageRequest(
                imageFile,
                PWD_ROAD_LAT,
                PWD_ROAD_LON,
                baseTime,
                "Ring Road near Connaught Place",
                0.35
        );
        DetectImageResponse responseA = imageDetectionService.processImageDetection(requestA);

        assertThat(responseA.potholeCreated()).isTrue();
        assertThat(responseA.pothole()).isNotNull();
        UUID potholeIdA = responseA.pothole().id();
        assertThat(responseA.pothole().isDuplicate()).isFalse();
        assertThat(responseA.pothole().duplicateOfId()).isNull();
        assertThat(responseA.pothole().authority()).isNotNull();
        assertThat(responseA.pothole().authority().code()).isEqualTo("DEMO_PWD_ARTERIAL");
        assertThat(responseA.pothole().severityClass()).isEqualTo(SeverityClass.MEDIUM);

        entityManager.flush();
        entityManager.clear();

        // Verify A in database
        Pothole savedA = potholeRepository.findById(potholeIdA).orElseThrow();
        assertThat(savedA.getIsDuplicate()).isFalse();
        assertThat(savedA.getDuplicateOf()).isNull();
        assertThat(savedA.getStatus()).isEqualTo(PotholeStatus.REPORTED);

        // 2. Request B: Duplicate submission 2 days later at exact same location (within 15m and within 7 days)
        Instant timeB = baseTime.plus(2, ChronoUnit.DAYS);
        DetectImageRequest requestB = new DetectImageRequest(
                imageFile,
                PWD_ROAD_LAT,
                PWD_ROAD_LON,
                timeB,
                "Ring Road citizen report",
                0.35
        );
        DetectImageResponse responseB = imageDetectionService.processImageDetection(requestB);

        assertThat(responseB.potholeCreated()).isTrue();
        assertThat(responseB.pothole()).isNotNull();
        UUID potholeIdB = responseB.pothole().id();

        // Verify B properties
        assertThat(potholeIdB).isNotEqualTo(potholeIdA);
        assertThat(responseB.pothole().isDuplicate()).isTrue();
        assertThat(responseB.pothole().duplicateOfId()).isEqualTo(potholeIdA);
        assertThat(responseB.pothole().authority()).isNotNull();
        assertThat(responseB.pothole().authority().code()).isEqualTo("DEMO_PWD_ARTERIAL");
        assertThat(responseB.pothole().severityClass()).isEqualTo(SeverityClass.MEDIUM);

        entityManager.flush();
        entityManager.clear();

        // Verify both A and B exist in database
        Pothole reloadedA = potholeRepository.findById(potholeIdA).orElseThrow();
        Pothole reloadedB = potholeRepository.findById(potholeIdB).orElseThrow();

        assertThat(reloadedA.getIsDuplicate()).isFalse();
        assertThat(reloadedA.getDuplicateOf()).isNull();
        assertThat(reloadedB.getIsDuplicate()).isTrue();
        assertThat(reloadedB.getDuplicateOf().getId()).isEqualTo(potholeIdA);

        // Verify automated authority reporting behavior (STEP 15)
        // Pothole A (unique, PWD) MUST have a report created with status DISPATCHED
        var reportA = reportRepository.findByPotholeId(potholeIdA);
        assertThat(reportA).isPresent();
        assertThat(reportA.get().getStatus()).isEqualTo(com.pothole.model.enums.ReportStatus.DISPATCHED);
        assertThat(reportA.get().getExternalReference()).contains("PWD-DEMO-2026-");

        // Pothole B (duplicate) MUST NOT have a report created (Duplicate Suppression)
        var reportB = reportRepository.findByPotholeId(potholeIdB);
        assertThat(reportB).isEmpty();

        // Verify confirmation count on parent A
        long confirmations = potholeRepository.countByDuplicateOfId(potholeIdA);
        assertThat(confirmations).isEqualTo(1L);

        // 3. Test: When parent A is marked RESOLVED, a new submission at same location is NOT duplicate
        reloadedA.setStatus(PotholeStatus.RESOLVED);
        potholeRepository.save(reloadedA);
        entityManager.flush();
        entityManager.clear();

        Instant timeC = baseTime.plus(3, ChronoUnit.DAYS);
        DetectImageRequest requestC = new DetectImageRequest(
                imageFile,
                PWD_ROAD_LAT,
                PWD_ROAD_LON,
                timeC,
                "Ring Road post-repair",
                0.35
        );
        DetectImageResponse responseC = imageDetectionService.processImageDetection(requestC);
        assertThat(responseC.potholeCreated()).isTrue();
        assertThat(responseC.pothole().isDuplicate()).isFalse();
        assertThat(responseC.pothole().duplicateOfId()).isNull();

        // 4. Test: Location far away (>15 meters away, e.g. 50m north: lat 28.6205) -> NOT duplicate
        DetectImageRequest requestFar = new DetectImageRequest(
                imageFile,
                PWD_ROAD_LAT + 0.0006, // ~66.5 meters north
                PWD_ROAD_LON,
                baseTime.plus(1, ChronoUnit.DAYS),
                "Far road point",
                0.35
        );
        DetectImageResponse responseFar = imageDetectionService.processImageDetection(requestFar);
        assertThat(responseFar.potholeCreated()).isTrue();
        assertThat(responseFar.pothole().isDuplicate()).isFalse();
        assertThat(responseFar.pothole().duplicateOfId()).isNull();
    }
}
