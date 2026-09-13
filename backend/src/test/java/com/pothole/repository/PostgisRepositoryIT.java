package com.pothole.repository;

import com.pothole.model.*;
import com.pothole.model.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class PostgisRepositoryIT {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

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

    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    @Autowired
    private DetectionJobRepository detectionJobRepository;

    @Autowired
    private DetectionRepository detectionRepository;

    @Autowired
    private PotholeRepository potholeRepository;

    @Autowired
    private CivicAuthorityRepository civicAuthorityRepository;

    @Test
    @DisplayName("Persist and retrieve MediaAsset, DetectionJob, Detection, and Pothole with PostGIS Point")
    void testCompletePersistenceFlow() {
        // 1. Persist MediaAsset
        MediaAsset mediaAsset = new MediaAsset(MediaType.IMAGE, "raw/2026/09/sample.jpg", "image/jpeg", 1024L);
        MediaAsset savedMedia = mediaAssetRepository.save(mediaAsset);
        assertThat(savedMedia.getId()).isNotNull();

        // 2. Persist DetectionJob linked to MediaAsset (1 -> 1)
        DetectionJob detectionJob = new DetectionJob(savedMedia, DetectionJobStatus.COMPLETED);
        detectionJob.setStartedAt(Instant.now());
        detectionJob.setCompletedAt(Instant.now());
        DetectionJob savedJob = detectionJobRepository.save(detectionJob);
        assertThat(savedJob.getId()).isNotNull();
        assertThat(savedJob.getMediaAsset().getId()).isEqualTo(savedMedia.getId());

        // 3. Persist CivicAuthority
        CivicAuthority authority = new CivicAuthority(
                "Bengaluru City Corporation",
                "BBMP_CENTRAL",
                "roads@bbmp.gov.in",
                "+918022221111",
                "MUNICIPAL"
        );
        CivicAuthority savedAuthority = civicAuthorityRepository.save(authority);

        // 4. Persist Pothole with PostGIS Point (SRID 4326)
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(77.5946, 12.9716));
        Pothole primaryPothole = new Pothole(location, 7.5, SeverityClass.HIGH, 0.92, "annotated/2026/09/primary.jpg");
        primaryPothole.setAddressText("MG Road, Bengaluru");
        primaryPothole.setCivicAuthority(savedAuthority);
        Pothole savedPrimary = potholeRepository.save(primaryPothole);

        assertThat(savedPrimary.getId()).isNotNull();
        assertThat(savedPrimary.getLocation().getSRID()).isEqualTo(4326);
        assertThat(savedPrimary.getLocation().getX()).isEqualTo(77.5946);
        assertThat(savedPrimary.getLocation().getY()).isEqualTo(12.9716);
        assertThat(savedPrimary.getStatus()).isEqualTo(PotholeStatus.REPORTED);
        assertThat(savedPrimary.getSeverityClass()).isEqualTo(SeverityClass.HIGH);
        assertThat(savedPrimary.getCivicAuthority().getCode()).isEqualTo("BBMP_CENTRAL");

        // 5. Persist Duplicate Pothole self-referencing primary
        Point duplicateLocation = GEOMETRY_FACTORY.createPoint(new Coordinate(77.5947, 12.9717));
        Pothole duplicatePothole = new Pothole(duplicateLocation, 7.0, SeverityClass.HIGH, 0.88, "annotated/2026/09/dup.jpg");
        duplicatePothole.setIsDuplicate(true);
        duplicatePothole.setDuplicateOf(savedPrimary);
        Pothole savedDuplicate = potholeRepository.save(duplicatePothole);

        assertThat(savedDuplicate.getId()).isNotNull();
        assertThat(savedDuplicate.getIsDuplicate()).isTrue();
        assertThat(savedDuplicate.getDuplicateOf().getId()).isEqualTo(savedPrimary.getId());

        // 6. Persist Detections linked to DetectionJob (1 -> *) and nullable Pothole
        Detection rawDetectionWithoutPothole = new Detection(savedJob, 100, 100, 300, 300, 0.85, 0.05);
        Detection savedRawDetection = detectionRepository.save(rawDetectionWithoutPothole);
        assertThat(savedRawDetection.getId()).isNotNull();
        assertThat(savedRawDetection.getPothole()).isNull(); // nullable relationship verified

        Detection aggregatedDetection = new Detection(savedJob, 120, 120, 320, 320, 0.92, 0.06);
        aggregatedDetection.setPothole(savedPrimary);
        Detection savedAggregatedDetection = detectionRepository.save(aggregatedDetection);
        assertThat(savedAggregatedDetection.getId()).isNotNull();
        assertThat(savedAggregatedDetection.getPothole().getId()).isEqualTo(savedPrimary.getId());

        // 7. Test Repository Query Methods
        List<Detection> jobDetections = detectionRepository.findByDetectionJobId(savedJob.getId());
        assertThat(jobDetections).hasSize(2);

        List<Detection> potholeDetections = detectionRepository.findByPotholeId(savedPrimary.getId());
        assertThat(potholeDetections).hasSize(1);

        Optional<DetectionJob> foundJob = detectionJobRepository.findByMediaAssetId(savedMedia.getId());
        assertThat(foundJob).isPresent();
        assertThat(foundJob.get().getStatus()).isEqualTo(DetectionJobStatus.COMPLETED);

        List<Pothole> nonDuplicates = potholeRepository.findByIsDuplicateFalse();
        assertThat(nonDuplicates).extracting(Pothole::getId).contains(savedPrimary.getId());
        assertThat(nonDuplicates).extracting(Pothole::getId).doesNotContain(savedDuplicate.getId());

        List<Pothole> duplicates = potholeRepository.findByDuplicateOfId(savedPrimary.getId());
        assertThat(duplicates).hasSize(1);
        assertThat(duplicates.getFirst().getId()).isEqualTo(savedDuplicate.getId());
    }
}
