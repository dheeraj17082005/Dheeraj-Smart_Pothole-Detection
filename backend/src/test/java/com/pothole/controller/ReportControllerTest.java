package com.pothole.controller;

import com.pothole.dto.AuthorityResponse;
import com.pothole.dto.reporting.ReportAttemptResponse;
import com.pothole.dto.reporting.ReportResponse;
import com.pothole.exception.GlobalExceptionHandler;
import com.pothole.exception.ObjectNotFoundException;
import com.pothole.exception.PotholeNotReportableException;
import com.pothole.model.CivicAuthority;
import com.pothole.model.Pothole;
import com.pothole.model.Report;
import com.pothole.model.ReportAttempt;
import com.pothole.model.enums.*;
import com.pothole.repository.PotholeRepository;
import com.pothole.service.reporting.AuthorityReportingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private MockMvc mockMvc;

    @Mock
    private AuthorityReportingService reportingService;

    @Mock
    private PotholeRepository potholeRepository;

    @InjectMocks
    private ReportController controller;

    private UUID potholeId;
    private UUID reportId;
    private Pothole pothole;
    private CivicAuthority authority;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        potholeId = UUID.randomUUID();
        reportId = UUID.randomUUID();

        authority = new CivicAuthority(
                "Delhi Public Works Department",
                "DEMO_PWD_ARTERIAL",
                "pwd@delhi.gov.in",
                "011-23456789",
                "PWD"
        );
        authority.setId(UUID.randomUUID());

        Point location = GF.createPoint(new Coordinate(77.22, 28.62));
        pothole = new Pothole(location, 40.0, SeverityClass.MEDIUM, 0.85, "rep/key.jpg");
        pothole.setId(potholeId);
        pothole.setIsDuplicate(false);
        pothole.setCivicAuthority(authority);
    }

    @Test
    @DisplayName("POST /api/v1/potholes/{id}/report returns 404 when pothole does not exist")
    void testReportPotholeNotFound() throws Exception {
        when(potholeRepository.findById(potholeId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/potholes/{id}/report", potholeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Object Not Found"));
    }

    @Test
    @DisplayName("POST /api/v1/potholes/{id}/report returns 400 when pothole is a duplicate")
    void testReportPotholeDuplicateRejected() throws Exception {
        pothole.setIsDuplicate(true);
        when(potholeRepository.findById(potholeId)).thenReturn(Optional.of(pothole));

        mockMvc.perform(post("/api/v1/potholes/{id}/report", potholeId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Pothole Not Reportable"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("flagged as a duplicate")));
    }

    @Test
    @DisplayName("POST /api/v1/potholes/{id}/report returns 400 when pothole has UNKNOWN_AUTHORITY")
    void testReportPotholeUnknownAuthorityRejected() throws Exception {
        pothole.setCivicAuthority(null);
        when(potholeRepository.findById(potholeId)).thenReturn(Optional.of(pothole));

        mockMvc.perform(post("/api/v1/potholes/{id}/report", potholeId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Pothole Not Reportable"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("UNKNOWN_AUTHORITY")));
    }

    @Test
    @DisplayName("POST /api/v1/potholes/{id}/report returns 200 OK with existing report if already created")
    void testReportPotholeReturnsExistingReport() throws Exception {
        when(potholeRepository.findById(potholeId)).thenReturn(Optional.of(pothole));

        Report existing = new Report(pothole, authority, "report-" + potholeId + "-" + authority.getId());
        existing.setId(reportId);
        existing.setStatus(ReportStatus.DISPATCHED);
        existing.setExternalReference("PWD-DEMO-2026-000001");

        when(reportingService.findReportForPothole(potholeId)).thenReturn(Optional.of(existing));

        mockMvc.perform(post("/api/v1/potholes/{id}/report", potholeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId.toString()))
                .andExpect(jsonPath("$.status").value("DISPATCHED"))
                .andExpect(jsonPath("$.external_reference").value("PWD-DEMO-2026-000001"));
    }

    @Test
    @DisplayName("POST /api/v1/potholes/{id}/report dispatches new report and returns 201 Created")
    void testReportPotholeCreatesNewReport() throws Exception {
        when(potholeRepository.findById(potholeId)).thenReturn(Optional.of(pothole));
        when(reportingService.findReportForPothole(potholeId)).thenReturn(Optional.empty());

        Report newReport = new Report(pothole, authority, "report-" + potholeId + "-" + authority.getId());
        newReport.setId(reportId);
        newReport.setStatus(ReportStatus.DISPATCHED);
        newReport.setExternalReference("PWD-DEMO-2026-000002");

        ReportAttempt attempt = new ReportAttempt(
                newReport, 1, ReportChannel.MOCK_EMAIL, newReport.getIdempotencyKey(),
                ReportAttemptStatus.SUCCESS, "Mock dispatch successful"
        );
        newReport.addAttempt(attempt);

        when(reportingService.dispatchReportForPothole(pothole)).thenReturn(newReport);

        mockMvc.perform(post("/api/v1/potholes/{id}/report", potholeId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(reportId.toString()))
                .andExpect(jsonPath("$.pothole_id").value(potholeId.toString()))
                .andExpect(jsonPath("$.status").value("DISPATCHED"))
                .andExpect(jsonPath("$.external_reference").value("PWD-DEMO-2026-000002"))
                .andExpect(jsonPath("$.attempts[0].attempt_number").value(1))
                .andExpect(jsonPath("$.attempts[0].status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/v1/reports/{reportId} returns 200 OK with report details")
    void testGetReportSuccess() throws Exception {
        Report report = new Report(pothole, authority, "report-" + potholeId + "-" + authority.getId());
        report.setId(reportId);
        report.setStatus(ReportStatus.DISPATCHED);
        report.setExternalReference("PWD-DEMO-2026-000003");

        when(reportingService.getReport(reportId)).thenReturn(report);

        mockMvc.perform(get("/api/v1/reports/{reportId}", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId.toString()))
                .andExpect(jsonPath("$.status").value("DISPATCHED"))
                .andExpect(jsonPath("$.external_reference").value("PWD-DEMO-2026-000003"));
    }

    @Test
    @DisplayName("GET /api/v1/reports/{reportId} returns 404 when report not found")
    void testGetReportNotFound() throws Exception {
        when(reportingService.getReport(reportId))
                .thenThrow(new ObjectNotFoundException("Report not found with ID: " + reportId));

        mockMvc.perform(get("/api/v1/reports/{reportId}", reportId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Object Not Found"));
    }
}
