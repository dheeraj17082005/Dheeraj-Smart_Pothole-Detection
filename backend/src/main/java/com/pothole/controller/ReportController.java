package com.pothole.controller;

import com.pothole.dto.reporting.ReportResponse;
import com.pothole.exception.ObjectNotFoundException;
import com.pothole.exception.PotholeNotReportableException;
import com.pothole.model.Pothole;
import com.pothole.model.Report;
import com.pothole.repository.PotholeRepository;
import com.pothole.service.reporting.AuthorityReportingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ReportController {

    private final AuthorityReportingService reportingService;
    private final PotholeRepository potholeRepository;

    public ReportController(AuthorityReportingService reportingService, PotholeRepository potholeRepository) {
        this.reportingService = reportingService;
        this.potholeRepository = potholeRepository;
    }

    /**
     * Dispatches an automated authority report for a specific detected pothole.
     * Idempotent: returns existing report if one has already been created.
     */
    @PostMapping(value = "/potholes/{id}/report", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReportResponse> reportPothole(@PathVariable("id") UUID potholeId) {
        Pothole pothole = potholeRepository.findById(potholeId)
                .orElseThrow(() -> new ObjectNotFoundException("Pothole not found with ID: " + potholeId));

        if (Boolean.TRUE.equals(pothole.getIsDuplicate())) {
            throw new PotholeNotReportableException(
                    "Pothole " + potholeId + " is flagged as a duplicate and cannot be reported to authorities."
            );
        }

        if (pothole.getCivicAuthority() == null) {
            throw new PotholeNotReportableException(
                    "Pothole " + potholeId + " has no assigned civic authority (UNKNOWN_AUTHORITY) and cannot be reported."
            );
        }

        // Return existing report if one was already created
        Optional<Report> existingReport = reportingService.findReportForPothole(potholeId);
        if (existingReport.isPresent()) {
            return ResponseEntity.ok(ReportResponse.fromEntity(existingReport.get()));
        }

        // Create and dispatch new report
        Report report = reportingService.dispatchReportForPothole(pothole);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReportResponse.fromEntity(report));
    }

    /**
     * Retrieves report metadata, status, attempts, and external ticket references.
     */
    @GetMapping(value = "/reports/{reportId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReportResponse> getReport(@PathVariable("reportId") UUID reportId) {
        Report report = reportingService.getReport(reportId);
        return ResponseEntity.ok(ReportResponse.fromEntity(report));
    }
}
