package com.pothole.service.reporting;

import com.pothole.exception.PotholeNotReportableException;
import com.pothole.model.CivicAuthority;
import com.pothole.model.Pothole;
import com.pothole.model.Report;
import com.pothole.model.ReportAttempt;
import com.pothole.model.enums.ReportAttemptStatus;
import com.pothole.model.enums.ReportStatus;
import com.pothole.model.enums.SeverityClass;
import com.pothole.repository.CivicAuthorityRepository;
import com.pothole.repository.PotholeRepository;
import com.pothole.repository.ReportAttemptRepository;
import com.pothole.repository.ReportRepository;
import com.pothole.service.reporting.client.MockAuthorityReportingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class AuthorityReportingIT {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

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
            registry.add("app.reporting.retry.initial-delay-ms", () -> "10"); // fast test retry backoff
        }
    }

    @Autowired
    private AuthorityReportingService reportingService;

    @Autowired
    private ReportPersistenceService persistenceService;

    @Autowired
    private MockAuthorityReportingClient mockReportingClient;

    @Autowired
    private PotholeRepository potholeRepository;

    @Autowired
    private CivicAuthorityRepository authorityRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportAttemptRepository reportAttemptRepository;

    private CivicAuthority pwdAuthority;

    @BeforeEach
    void setUp() {
        mockReportingClient.reset();
        reportingService.setInitialDelayMs(10L);

        // Fetch or create PWD authority from seeded V3
        pwdAuthority = authorityRepository.findByCode("DEMO_PWD_ARTERIAL")
                .orElseGet(() -> authorityRepository.save(new CivicAuthority(
                        "Delhi Public Works Department",
                        "DEMO_PWD_ARTERIAL",
                        "pwd@delhi.gov.in",
                        "011-23456789",
                        "PWD"
                )));
    }

    private Pothole createPothole(double lat, double lon, boolean isDuplicate, CivicAuthority authority) {
        Point point = GF.createPoint(new Coordinate(lon, lat));
        Pothole pothole = new Pothole(point, 42.0, SeverityClass.MEDIUM, 0.85, "rep/key.jpg");
        pothole.setIsDuplicate(isDuplicate);
        pothole.setCivicAuthority(authority);
        pothole.setFirstDetectedAt(Instant.now());
        return potholeRepository.save(pothole);
    }

    @Test
    @DisplayName("1. Real DB: First attempt succeeds and persists DISPATCHED Report + SUCCESS ReportAttempt")
    void testSuccessfulDispatchAndPersistence() {
        Pothole pothole = createPothole(28.62, 77.22, false, pwdAuthority);

        Report report = reportingService.dispatchReportForPothole(pothole);

        assertThat(report).isNotNull();
        assertThat(report.getId()).isNotNull();
        assertThat(report.getStatus()).isEqualTo(ReportStatus.DISPATCHED);
        assertThat(report.getExternalReference()).contains("PWD-DEMO-2026-");

        // Verify database persistence
        Report persistedReport = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(persistedReport.getStatus()).isEqualTo(ReportStatus.DISPATCHED);
        assertThat(persistedReport.getExternalReference()).isEqualTo(report.getExternalReference());

        List<ReportAttempt> attempts = reportAttemptRepository.findByReportIdOrderByAttemptNumberAsc(report.getId());
        assertThat(attempts).hasSize(1);
        ReportAttempt attempt1 = attempts.get(0);
        assertThat(attempt1.getAttemptNumber()).isEqualTo(1);
        assertThat(attempt1.getStatus()).isEqualTo(ReportAttemptStatus.SUCCESS);
        assertThat(attempt1.getIdempotencyKey()).isEqualTo(report.getIdempotencyKey());
        assertThat(attempt1.getResponseSummary()).contains("Simulated dispatch successful");
    }

    @Test
    @DisplayName("2. Real DB: First attempt fails, second succeeds -> 2 ReportAttempts persisted, final DISPATCHED")
    void testRetryThenSuccessPersistence() {
        Pothole pothole = createPothole(28.621, 77.221, false, pwdAuthority);
        mockReportingClient.setFailFirstNAttempts(1);

        Report report = reportingService.dispatchReportForPothole(pothole);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.DISPATCHED);
        assertThat(report.getExternalReference()).isNotNull();

        List<ReportAttempt> attempts = reportAttemptRepository.findByReportIdOrderByAttemptNumberAsc(report.getId());
        assertThat(attempts).hasSize(2);

        ReportAttempt attempt1 = attempts.get(0);
        assertThat(attempt1.getAttemptNumber()).isEqualTo(1);
        assertThat(attempt1.getStatus()).isEqualTo(ReportAttemptStatus.FAILED);

        ReportAttempt attempt2 = attempts.get(1);
        assertThat(attempt2.getAttemptNumber()).isEqualTo(2);
        assertThat(attempt2.getStatus()).isEqualTo(ReportAttemptStatus.SUCCESS);
    }

    @Test
    @DisplayName("3. Real DB: All 3 attempts fail -> 3 ReportAttempts persisted, final FAILED, attempts not deleted")
    void testPermanentFailurePersistence() {
        Pothole pothole = createPothole(28.622, 77.222, false, pwdAuthority);
        mockReportingClient.setSimulatePermanentFailure(true);

        Report report = reportingService.dispatchReportForPothole(pothole);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
        assertThat(report.getExternalReference()).isNull();

        List<ReportAttempt> attempts = reportAttemptRepository.findByReportIdOrderByAttemptNumberAsc(report.getId());
        assertThat(attempts).hasSize(3);
        assertThat(attempts).allMatch(a -> a.getStatus() == ReportAttemptStatus.FAILED);
        assertThat(attempts.get(0).getAttemptNumber()).isEqualTo(1);
        assertThat(attempts.get(1).getAttemptNumber()).isEqualTo(2);
        assertThat(attempts.get(2).getAttemptNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("4. Real DB: Database unique constraints prevent duplicate report creation for same pothole/authority")
    void testIdempotencyUniqueConstraint() {
        Pothole pothole = createPothole(28.623, 77.223, false, pwdAuthority);

        Report report1 = reportingService.dispatchReportForPothole(pothole);
        assertThat(report1.getStatus()).isEqualTo(ReportStatus.DISPATCHED);

        // Calling dispatch again returns existing report without creating a second report or attempts
        Report report2 = reportingService.dispatchReportForPothole(pothole);
        assertThat(report2.getId()).isEqualTo(report1.getId());

        List<ReportAttempt> attempts = reportAttemptRepository.findByReportIdOrderByAttemptNumberAsc(report1.getId());
        assertThat(attempts).hasSize(1); // No second dispatch

        // Direct DB attempt to insert duplicate with same idempotency key fails
        Report duplicateReport = new Report(pothole, pwdAuthority, report1.getIdempotencyKey());
        assertThatThrownBy(() -> reportRepository.saveAndFlush(duplicateReport))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("5. Real DB: Duplicate pothole is suppressed from reporting")
    void testDuplicatePotholeSuppressed() {
        Pothole pothole = createPothole(28.624, 77.224, true, pwdAuthority);

        assertThatThrownBy(() -> reportingService.dispatchReportForPothole(pothole))
                .isInstanceOf(PotholeNotReportableException.class);

        assertThat(reportRepository.findByPotholeId(pothole.getId())).isEmpty();
    }

    @Test
    @DisplayName("6. Real DB: Pothole with UNKNOWN_AUTHORITY is suppressed from dispatch")
    void testUnknownAuthoritySuppressed() {
        Pothole pothole = createPothole(12.9716, 77.5946, false, null);

        assertThatThrownBy(() -> reportingService.dispatchReportForPothole(pothole))
                .isInstanceOf(PotholeNotReportableException.class);

        assertThat(reportRepository.findByPotholeId(pothole.getId())).isEmpty();
    }
}
