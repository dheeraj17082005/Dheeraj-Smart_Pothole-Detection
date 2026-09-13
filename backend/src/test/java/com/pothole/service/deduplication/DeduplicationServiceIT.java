package com.pothole.service.deduplication;

import com.pothole.model.Pothole;
import com.pothole.model.enums.PotholeStatus;
import com.pothole.model.enums.SeverityClass;
import com.pothole.repository.PotholeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class DeduplicationServiceIT {

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
    private DeduplicationService deduplicationService;

    @Autowired
    private PotholeRepository potholeRepository;

    @Autowired
    private EntityManager entityManager;

    private static final double BASE_LAT = 28.620000;
    private static final double BASE_LON = 77.220000;
    private final Instant baseTime = Instant.parse("2026-09-11T12:00:00Z");

    @BeforeEach
    void cleanUp() {
        potholeRepository.deleteAll();
        entityManager.flush();
    }

    private Point projectPoint(double lon, double lat, double distanceMeters, double azimuthDegrees) {
        String sql = """
                SELECT 
                    ST_X(CAST(ST_Project(CAST(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326) AS geography), :dist, radians(:azimuth)) AS geometry)),
                    ST_Y(CAST(ST_Project(CAST(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326) AS geography), :dist, radians(:azimuth)) AS geometry))
                """;
        Object[] coords = (Object[]) entityManager.createNativeQuery(sql)
                .setParameter("lon", lon)
                .setParameter("lat", lat)
                .setParameter("dist", distanceMeters)
                .setParameter("azimuth", azimuthDegrees)
                .getSingleResult();

        double projLon = ((Number) coords[0]).doubleValue();
        double projLat = ((Number) coords[1]).doubleValue();
        return GEOMETRY_FACTORY.createPoint(new Coordinate(projLon, projLat));
    }

    private Pothole createCandidate(Point location, Instant detectedAt, PotholeStatus status) {
        Pothole p = new Pothole(location, 40.0, SeverityClass.MEDIUM, 0.85, "rep/candidate.jpg");
        p.setFirstDetectedAt(detectedAt);
        p.setStatus(status);
        p.setIsDuplicate(false);
        Pothole saved = potholeRepository.save(p);
        entityManager.flush();
        return saved;
    }

    @Test
    @DisplayName("1. New pothole with no nearby candidate -> not duplicate")
    void testNoCandidateNotDuplicate() {
        DeduplicationResult result = deduplicationService.evaluateDuplicate(BASE_LAT, BASE_LON, baseTime);

        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.parentPothole()).isNull();
        assertThat(result.duplicateOfId()).isNull();
    }

    @Test
    @DisplayName("2. Candidate exactly 15 metres away -> duplicate")
    void testCandidateExactly15MetersAwayIsDuplicate() {
        Point pt15m = projectPoint(BASE_LON, BASE_LAT, 15.0, 0.0);
        Pothole candidate = createCandidate(pt15m, baseTime.minus(2, ChronoUnit.DAYS), PotholeStatus.REPORTED);

        DeduplicationResult result = deduplicationService.evaluateDuplicate(BASE_LAT, BASE_LON, baseTime);

        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.duplicateOfId()).isEqualTo(candidate.getId());
        assertThat(result.parentPothole().getId()).isEqualTo(candidate.getId());
    }

    @Test
    @DisplayName("3. Candidate beyond 15 metres (25m) -> not duplicate")
    void testCandidateBeyond15MetersIsNotDuplicate() {
        Point pt25m = projectPoint(BASE_LON, BASE_LAT, 25.0, 0.0);
        createCandidate(pt25m, baseTime.minus(2, ChronoUnit.DAYS), PotholeStatus.REPORTED);

        DeduplicationResult result = deduplicationService.evaluateDuplicate(BASE_LAT, BASE_LON, baseTime);

        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.parentPothole()).isNull();
    }

    @Test
    @DisplayName("4. Candidate inside 15 metres but older than 7 days (8 days old) -> not duplicate")
    void testCandidateOlderThan7DaysIsNotDuplicate() {
        Point pt10m = projectPoint(BASE_LON, BASE_LAT, 10.0, 0.0);
        Instant eightDaysAgo = baseTime.minus(8, ChronoUnit.DAYS);
        createCandidate(pt10m, eightDaysAgo, PotholeStatus.REPORTED);

        DeduplicationResult result = deduplicationService.evaluateDuplicate(BASE_LAT, BASE_LON, baseTime);

        assertThat(result.isDuplicate()).isFalse();
    }

    @Test
    @DisplayName("5. Candidate inside 15 metres and exactly 7 days old -> duplicate (inclusive boundary)")
    void testCandidateExactly7DaysOldIsDuplicate() {
        Point pt10m = projectPoint(BASE_LON, BASE_LAT, 10.0, 0.0);
        Instant exactly7DaysAgo = baseTime.minus(7, ChronoUnit.DAYS);
        Pothole candidate = createCandidate(pt10m, exactly7DaysAgo, PotholeStatus.REPORTED);

        DeduplicationResult result = deduplicationService.evaluateDuplicate(BASE_LAT, BASE_LON, baseTime);

        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.duplicateOfId()).isEqualTo(candidate.getId());
    }

    @Test
    @DisplayName("6. Candidate with RESOLVED status -> not duplicate")
    void testCandidateResolvedStatusIsNotDuplicate() {
        Point pt5m = projectPoint(BASE_LON, BASE_LAT, 5.0, 0.0);
        createCandidate(pt5m, baseTime.minus(1, ChronoUnit.DAYS), PotholeStatus.RESOLVED);

        DeduplicationResult result = deduplicationService.evaluateDuplicate(BASE_LAT, BASE_LON, baseTime);

        assertThat(result.isDuplicate()).isFalse();
    }

    @Test
    @DisplayName("7. Candidate with REPORTED status -> duplicate")
    void testCandidateReportedStatusIsDuplicate() {
        Point pt5m = projectPoint(BASE_LON, BASE_LAT, 5.0, 0.0);
        Pothole candidate = createCandidate(pt5m, baseTime.minus(1, ChronoUnit.DAYS), PotholeStatus.REPORTED);

        DeduplicationResult result = deduplicationService.evaluateDuplicate(BASE_LAT, BASE_LON, baseTime);

        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.duplicateOfId()).isEqualTo(candidate.getId());
    }

    @Test
    @DisplayName("8. Candidate with ACKNOWLEDGED status -> duplicate")
    void testCandidateAcknowledgedStatusIsDuplicate() {
        Point pt5m = projectPoint(BASE_LON, BASE_LAT, 5.0, 0.0);
        Pothole candidate = createCandidate(pt5m, baseTime.minus(2, ChronoUnit.DAYS), PotholeStatus.ACKNOWLEDGED);

        DeduplicationResult result = deduplicationService.evaluateDuplicate(BASE_LAT, BASE_LON, baseTime);

        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.duplicateOfId()).isEqualTo(candidate.getId());
    }

    @Test
    @DisplayName("9. Candidate with IN_PROGRESS status -> duplicate")
    void testCandidateInProgressStatusIsDuplicate() {
        Point pt5m = projectPoint(BASE_LON, BASE_LAT, 5.0, 0.0);
        Pothole candidate = createCandidate(pt5m, baseTime.minus(3, ChronoUnit.DAYS), PotholeStatus.IN_PROGRESS);

        DeduplicationResult result = deduplicationService.evaluateDuplicate(BASE_LAT, BASE_LON, baseTime);

        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.duplicateOfId()).isEqualTo(candidate.getId());
    }

    @Test
    @DisplayName("10. Multiple matching candidates -> selects closest candidate deterministically")
    void testMultipleCandidatesSelectsClosestDeterministically() {
        // Candidate 1: 12 meters away, detected 1 day ago
        Point pt12m = projectPoint(BASE_LON, BASE_LAT, 12.0, 90.0);
        Pothole fartherCandidate = createCandidate(pt12m, baseTime.minus(1, ChronoUnit.DAYS), PotholeStatus.REPORTED);

        // Candidate 2: 4 meters away, detected 3 days ago
        Point pt4m = projectPoint(BASE_LON, BASE_LAT, 4.0, 0.0);
        Pothole closerCandidate = createCandidate(pt4m, baseTime.minus(3, ChronoUnit.DAYS), PotholeStatus.REPORTED);

        DeduplicationResult result = deduplicationService.evaluateDuplicate(BASE_LAT, BASE_LON, baseTime);

        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.duplicateOfId()).isEqualTo(closerCandidate.getId());
        assertThat(result.parentPothole().getId()).isEqualTo(closerCandidate.getId());
    }

    @Test
    @DisplayName("11 & 12. New duplicate receives duplicateOfId; parent pothole remains unchanged")
    void testParentPotholeRemainsUnchanged() {
        Point pt5m = projectPoint(BASE_LON, BASE_LAT, 5.0, 0.0);
        Pothole parent = createCandidate(pt5m, baseTime.minus(1, ChronoUnit.DAYS), PotholeStatus.REPORTED);
        UUID parentId = parent.getId();
        Instant originalCreatedAt = parent.getCreatedAt();

        // Simulate new submission duplicate
        DeduplicationResult result = deduplicationService.evaluateDuplicate(BASE_LAT, BASE_LON, baseTime);
        assertThat(result.isDuplicate()).isTrue();

        Point newPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(BASE_LON, BASE_LAT));
        Pothole duplicate = new Pothole(newPoint, 42.0, SeverityClass.MEDIUM, 0.89, "rep/dup.jpg");
        duplicate.setIsDuplicate(true);
        duplicate.setDuplicateOf(result.parentPothole());
        potholeRepository.save(duplicate);
        entityManager.flush();
        entityManager.clear();

        // Verify parent is untouched
        Pothole reloadedParent = potholeRepository.findById(parentId).orElseThrow();
        assertThat(reloadedParent.getIsDuplicate()).isFalse();
        assertThat(reloadedParent.getDuplicateOf()).isNull();
        assertThat(reloadedParent.getStatus()).isEqualTo(PotholeStatus.REPORTED);
        assertThat(reloadedParent.getCreatedAt()).isEqualTo(originalCreatedAt);

        // Verify duplicate confirmation count
        long confirmations = deduplicationService.getConfirmationCount(parentId);
        assertThat(confirmations).isEqualTo(1L);
    }
}
