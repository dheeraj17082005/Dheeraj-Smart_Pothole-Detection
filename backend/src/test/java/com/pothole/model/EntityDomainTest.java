package com.pothole.model;

import com.pothole.model.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EntityDomainTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Test
    @DisplayName("MediaAsset properties and defaults")
    void testMediaAsset() {
        MediaAsset asset = new MediaAsset(MediaType.IMAGE, "raw/2026/09/pothole.jpg", "image/jpeg", 204800L);
        UUID id = UUID.randomUUID();
        asset.setId(id);

        assertThat(asset.getId()).isEqualTo(id);
        assertThat(asset.getMediaType()).isEqualTo(MediaType.IMAGE);
        assertThat(asset.getRawObjectKey()).isEqualTo("raw/2026/09/pothole.jpg");
        assertThat(asset.getMimeType()).isEqualTo("image/jpeg");
        assertThat(asset.getFileSize()).isEqualTo(204800L);
        assertThat(asset.getUploadedAt()).isNotNull();
        assertThat(asset.toString()).contains("raw/2026/09/pothole.jpg");
    }

    @Test
    @DisplayName("DetectionJob properties and relationship to MediaAsset")
    void testDetectionJob() {
        MediaAsset asset = new MediaAsset(MediaType.VIDEO, "raw/2026/09/video.mp4", "video/mp4", 10485760L);
        asset.setId(UUID.randomUUID());

        DetectionJob job = new DetectionJob(asset, DetectionJobStatus.PENDING);
        UUID jobId = UUID.randomUUID();
        job.setId(jobId);

        assertThat(job.getId()).isEqualTo(jobId);
        assertThat(job.getMediaAsset()).isEqualTo(asset);
        assertThat(job.getStatus()).isEqualTo(DetectionJobStatus.PENDING);

        job.setStatus(DetectionJobStatus.PROCESSING);
        job.setStartedAt(Instant.now());
        assertThat(job.getStatus()).isEqualTo(DetectionJobStatus.PROCESSING);
        assertThat(job.getStartedAt()).isNotNull();

        job.setStatus(DetectionJobStatus.FAILED);
        job.setErrorSummary("Invalid video codec");
        job.setCompletedAt(Instant.now());
        assertThat(job.getStatus()).isEqualTo(DetectionJobStatus.FAILED);
        assertThat(job.getErrorSummary()).isEqualTo("Invalid video codec");
    }

    @Test
    @DisplayName("Pothole properties, spatial Point, lifecycle hooks, and duplicate self-reference")
    void testPothole() {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(77.5946, 12.9716));
        Pothole pothole = new Pothole(point, 8.5, SeverityClass.HIGH, 0.95, "annotated/2026/09/rep.jpg");
        UUID id = UUID.randomUUID();
        pothole.setId(id);
        pothole.setAddressText("100 Feet Rd, Indiranagar");

        assertThat(pothole.getId()).isEqualTo(id);
        assertThat(pothole.getLocation().getSRID()).isEqualTo(4326);
        assertThat(pothole.getLocation().getX()).isEqualTo(77.5946);
        assertThat(pothole.getLocation().getY()).isEqualTo(12.9716);
        assertThat(pothole.getSeverityClass()).isEqualTo(SeverityClass.HIGH);
        assertThat(pothole.getSeverityScore()).isEqualTo(8.5);
        assertThat(pothole.getMaxConfidence()).isEqualTo(0.95);
        assertThat(pothole.getStatus()).isEqualTo(PotholeStatus.REPORTED);
        assertThat(pothole.getIsDuplicate()).isFalse();
        assertThat(pothole.getRepresentativeKey()).isEqualTo("annotated/2026/09/rep.jpg");

        // Duplicate self-reference
        Point dupPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(77.5947, 12.9717));
        Pothole duplicate = new Pothole(dupPoint, 8.0, SeverityClass.HIGH, 0.91, "annotated/2026/09/dup.jpg");
        duplicate.setId(UUID.randomUUID());
        duplicate.setIsDuplicate(true);
        duplicate.setDuplicateOf(pothole);

        assertThat(duplicate.getIsDuplicate()).isTrue();
        assertThat(duplicate.getDuplicateOf()).isEqualTo(pothole);
        assertThat(duplicate.getDuplicateOf().getId()).isEqualTo(id);

        // Lifecycle pre-persist
        Pothole emptyPothole = new Pothole();
        emptyPothole.onCreate();
        assertThat(emptyPothole.getCreatedAt()).isNotNull();
        assertThat(emptyPothole.getUpdatedAt()).isNotNull();
        assertThat(emptyPothole.getStatus()).isEqualTo(PotholeStatus.REPORTED);
        assertThat(emptyPothole.getIsDuplicate()).isFalse();
    }

    @Test
    @DisplayName("Detection properties, bounding box, and nullable Pothole link")
    void testDetection() {
        MediaAsset asset = new MediaAsset(MediaType.IMAGE, "raw/test.jpg", "image/jpeg", 5000L);
        DetectionJob job = new DetectionJob(asset, DetectionJobStatus.COMPLETED);

        Detection detection = new Detection(job, 100, 150, 300, 350, 0.89, 0.045);
        UUID id = UUID.randomUUID();
        detection.setId(id);
        detection.setFrameIndex(24);
        detection.setFrameTimestampSec(1.0);

        assertThat(detection.getId()).isEqualTo(id);
        assertThat(detection.getDetectionJob()).isEqualTo(job);
        assertThat(detection.getPothole()).isNull(); // starts null before aggregation
        assertThat(detection.getFrameIndex()).isEqualTo(24);
        assertThat(detection.getFrameTimestampSec()).isEqualTo(1.0);
        assertThat(detection.getBoxXmin()).isEqualTo(100);
        assertThat(detection.getBoxYmin()).isEqualTo(150);
        assertThat(detection.getBoxXmax()).isEqualTo(300);
        assertThat(detection.getBoxYmax()).isEqualTo(350);
        assertThat(detection.getConfidence()).isEqualTo(0.89);
        assertThat(detection.getVisualAreaRatio()).isEqualTo(0.045);

        // Now link to a Pothole
        Pothole pothole = new Pothole(
                GEOMETRY_FACTORY.createPoint(new Coordinate(77.0, 12.0)),
                5.0,
                SeverityClass.MEDIUM,
                0.89,
                "annotated/test.jpg"
        );
        detection.setPothole(pothole);
        assertThat(detection.getPothole()).isEqualTo(pothole);
    }

    @Test
    @DisplayName("CivicAuthority properties")
    void testCivicAuthority() {
        CivicAuthority authority = new CivicAuthority(
                "Public Works Department",
                "PWD_KARNATAKA",
                "support@pwd.karnataka.gov.in",
                "+918012345678",
                "PWD"
        );
        UUID id = UUID.randomUUID();
        authority.setId(id);

        assertThat(authority.getId()).isEqualTo(id);
        assertThat(authority.getName()).isEqualTo("Public Works Department");
        assertThat(authority.getCode()).isEqualTo("PWD_KARNATAKA");
        assertThat(authority.getContactEmail()).isEqualTo("support@pwd.karnataka.gov.in");
        assertThat(authority.getContactPhone()).isEqualTo("+918012345678");
        assertThat(authority.getDepartmentType()).isEqualTo("PWD");
        assertThat(authority.getCreatedAt()).isNotNull();
    }
}
