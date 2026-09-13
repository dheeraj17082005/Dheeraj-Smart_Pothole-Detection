package com.pothole.service.reporting;

import com.pothole.exception.PotholeNotReportableException;
import com.pothole.model.CivicAuthority;
import com.pothole.model.Pothole;
import com.pothole.model.Report;
import com.pothole.model.enums.ReportChannel;
import com.pothole.model.enums.ReportStatus;
import com.pothole.model.enums.SeverityClass;
import com.pothole.repository.ReportRepository;
import com.pothole.service.reporting.client.AuthorityReportingClient;
import com.pothole.service.reporting.client.ReportingDispatchPayload;
import com.pothole.service.reporting.client.ReportingDispatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorityReportingServiceTest {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Mock
    private ReportPersistenceService reportPersistenceService;

    @Mock
    private AuthorityReportingClient reportingClient;

    @Mock
    private ReportRepository reportRepository;

    private AuthorityReportingService reportingService;

    private Pothole testPothole;
    private CivicAuthority testAuthority;
    private Report pendingReport;

    @BeforeEach
    void setUp() {
        reportingService = new AuthorityReportingService(
                reportPersistenceService,
                reportingClient,
                reportRepository
        );
        reportingService.setInitialDelayMs(1L); // Fast test backoff

        testAuthority = new CivicAuthority(
                "Delhi Public Works Department",
                "DEMO_PWD_ARTERIAL",
                "pwd@delhi.gov.in",
                "011-23456789",
                "PWD"
        );
        testAuthority.setId(UUID.randomUUID());

        Point location = GF.createPoint(new Coordinate(77.22, 28.62));
        testPothole = new Pothole(location, 45.0, SeverityClass.MEDIUM, 0.88, "rep/key.jpg");
        testPothole.setId(UUID.randomUUID());
        testPothole.setIsDuplicate(false);
        testPothole.setCivicAuthority(testAuthority);

        pendingReport = new Report(testPothole, testAuthority, "report-" + testPothole.getId() + "-" + testAuthority.getId());
        pendingReport.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Scenario 1: First dispatch attempt succeeds")
    void testFirstAttemptSucceeds() {
        when(reportPersistenceService.getOrCreatePendingReport(eq(testPothole), eq(testAuthority), anyString()))
                .thenReturn(pendingReport);

        ReportingDispatchResult successResult = ReportingDispatchResult.success(
                "PWD-DEMO-2026-000001",
                "Simulated dispatch successful",
                ReportChannel.MOCK_EMAIL
        );
        when(reportingClient.submitReport(any(ReportingDispatchPayload.class)))
                .thenReturn(successResult);

        Report dispatchedReport = new Report(testPothole, testAuthority, pendingReport.getIdempotencyKey());
        dispatchedReport.setId(pendingReport.getId());
        dispatchedReport.setStatus(ReportStatus.DISPATCHED);
        dispatchedReport.setExternalReference("PWD-DEMO-2026-000001");

        when(reportPersistenceService.recordAttemptAndUpdateStatus(
                eq(pendingReport.getId()), eq(1), eq(ReportChannel.MOCK_EMAIL), anyString(),
                eq(true), anyString(), eq("PWD-DEMO-2026-000001"), eq(false)
        )).thenReturn(dispatchedReport);

        Report result = reportingService.dispatchReportForPothole(testPothole);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ReportStatus.DISPATCHED);
        assertThat(result.getExternalReference()).isEqualTo("PWD-DEMO-2026-000001");

        verify(reportingClient, times(1)).submitReport(any(ReportingDispatchPayload.class));
        verify(reportPersistenceService, times(1)).recordAttemptAndUpdateStatus(
                any(), eq(1), any(), any(), eq(true), any(), eq("PWD-DEMO-2026-000001"), eq(false)
        );
    }

    @Test
    @DisplayName("Scenario 2: First attempt fails, second attempt succeeds (Retry then Success)")
    void testFirstAttemptFailsSecondSucceeds() {
        when(reportPersistenceService.getOrCreatePendingReport(eq(testPothole), eq(testAuthority), anyString()))
                .thenReturn(pendingReport);

        ReportingDispatchResult failResult = ReportingDispatchResult.failure(
                "Gateway Timeout",
                ReportChannel.MOCK_EMAIL
        );
        ReportingDispatchResult successResult = ReportingDispatchResult.success(
                "PWD-DEMO-2026-000002",
                "Simulated dispatch successful",
                ReportChannel.MOCK_EMAIL
        );

        when(reportingClient.submitReport(any(ReportingDispatchPayload.class)))
                .thenReturn(failResult)
                .thenReturn(successResult);

        Report intermediateReport = new Report(testPothole, testAuthority, pendingReport.getIdempotencyKey());
        intermediateReport.setId(pendingReport.getId());
        intermediateReport.setStatus(ReportStatus.PENDING);

        Report dispatchedReport = new Report(testPothole, testAuthority, pendingReport.getIdempotencyKey());
        dispatchedReport.setId(pendingReport.getId());
        dispatchedReport.setStatus(ReportStatus.DISPATCHED);
        dispatchedReport.setExternalReference("PWD-DEMO-2026-000002");

        when(reportPersistenceService.recordAttemptAndUpdateStatus(
                eq(pendingReport.getId()), eq(1), any(), anyString(),
                eq(false), anyString(), isNull(), eq(false)
        )).thenReturn(intermediateReport);

        when(reportPersistenceService.recordAttemptAndUpdateStatus(
                eq(pendingReport.getId()), eq(2), any(), anyString(),
                eq(true), anyString(), eq("PWD-DEMO-2026-000002"), eq(false)
        )).thenReturn(dispatchedReport);

        Report result = reportingService.dispatchReportForPothole(testPothole);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ReportStatus.DISPATCHED);
        assertThat(result.getExternalReference()).isEqualTo("PWD-DEMO-2026-000002");

        verify(reportingClient, times(2)).submitReport(any(ReportingDispatchPayload.class));
    }

    @Test
    @DisplayName("Scenario 3: All 3 attempts fail (Permanent Failure)")
    void testAllThreeAttemptsFail() {
        when(reportPersistenceService.getOrCreatePendingReport(eq(testPothole), eq(testAuthority), anyString()))
                .thenReturn(pendingReport);

        ReportingDispatchResult failResult = ReportingDispatchResult.failure(
                "Simulated gateway unreachable",
                ReportChannel.MOCK_EMAIL
        );

        when(reportingClient.submitReport(any(ReportingDispatchPayload.class)))
                .thenReturn(failResult);

        Report failedReport = new Report(testPothole, testAuthority, pendingReport.getIdempotencyKey());
        failedReport.setId(pendingReport.getId());
        failedReport.setStatus(ReportStatus.FAILED);

        when(reportPersistenceService.recordAttemptAndUpdateStatus(
                eq(pendingReport.getId()), anyInt(), any(), anyString(),
                eq(false), anyString(), isNull(), anyBoolean()
        )).thenReturn(failedReport);

        Report result = reportingService.dispatchReportForPothole(testPothole);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ReportStatus.FAILED);

        verify(reportingClient, times(3)).submitReport(any(ReportingDispatchPayload.class));
        verify(reportPersistenceService, times(1)).recordAttemptAndUpdateStatus(
                any(), eq(3), any(), any(), eq(false), any(), isNull(), eq(true)
        );
    }

    @Test
    @DisplayName("Scenario 4: Duplicate pothole does not create a report (Duplicate Suppression)")
    void testDuplicatePotholeSuppressed() {
        testPothole.setIsDuplicate(true);

        assertThatThrownBy(() -> reportingService.dispatchReportForPothole(testPothole))
                .isInstanceOf(PotholeNotReportableException.class)
                .hasMessageContaining("flagged as a duplicate");

        verifyNoInteractions(reportingClient);
        verifyNoInteractions(reportPersistenceService);
    }

    @Test
    @DisplayName("Scenario 5: Pothole with UNKNOWN_AUTHORITY does not dispatch (Unknown Authority Suppression)")
    void testUnknownAuthoritySuppressed() {
        testPothole.setCivicAuthority(null);

        assertThatThrownBy(() -> reportingService.dispatchReportForPothole(testPothole))
                .isInstanceOf(PotholeNotReportableException.class)
                .hasMessageContaining("UNKNOWN_AUTHORITY");

        verifyNoInteractions(reportingClient);
        verifyNoInteractions(reportPersistenceService);
    }

    @Test
    @DisplayName("Scenario 6: Retrying reporting for already dispatched report returns existing report without re-dispatch")
    void testAlreadyDispatchedReportReturnedWithoutSecondDispatch() {
        Report alreadyDispatched = new Report(testPothole, testAuthority, pendingReport.getIdempotencyKey());
        alreadyDispatched.setId(pendingReport.getId());
        alreadyDispatched.setStatus(ReportStatus.DISPATCHED);
        alreadyDispatched.setExternalReference("PWD-DEMO-2026-000001");

        when(reportPersistenceService.getOrCreatePendingReport(eq(testPothole), eq(testAuthority), anyString()))
                .thenReturn(alreadyDispatched);

        Report result = reportingService.dispatchReportForPothole(testPothole);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ReportStatus.DISPATCHED);
        assertThat(result.getExternalReference()).isEqualTo("PWD-DEMO-2026-000001");

        // External dispatch was NOT called again
        verifyNoInteractions(reportingClient);
    }

    @Test
    @DisplayName("Scenario 7: Stable idempotency key format report-{potholeId}-{authorityId}")
    void testStableIdempotencyKeyFormat() {
        String key = reportingService.buildIdempotencyKey(testPothole.getId(), testAuthority.getId());
        assertThat(key).isEqualTo("report-" + testPothole.getId() + "-" + testAuthority.getId());
    }
}
