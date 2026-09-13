package com.pothole.service.reporting;

import com.pothole.model.CivicAuthority;
import com.pothole.model.Pothole;
import com.pothole.model.Report;
import com.pothole.model.ReportAttempt;
import com.pothole.model.enums.ReportAttemptStatus;
import com.pothole.model.enums.ReportChannel;
import com.pothole.model.enums.ReportStatus;
import com.pothole.repository.ReportAttemptRepository;
import com.pothole.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class ReportPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(ReportPersistenceService.class);

    private final ReportRepository reportRepository;
    private final ReportAttemptRepository reportAttemptRepository;

    public ReportPersistenceService(ReportRepository reportRepository, ReportAttemptRepository reportAttemptRepository) {
        this.reportRepository = reportRepository;
        this.reportAttemptRepository = reportAttemptRepository;
    }

    /**
     * Transaction 1: Create a PENDING report or retrieve the existing one.
     * Committed before any external dispatch call.
     */
    @Transactional
    public Report getOrCreatePendingReport(Pothole pothole, CivicAuthority authority, String idempotencyKey) {
        Optional<Report> existingByIdempotency = reportRepository.findByIdempotencyKey(idempotencyKey);
        if (existingByIdempotency.isPresent()) {
            log.info("Found existing report [{}] by idempotency key [{}]",
                    existingByIdempotency.get().getId(), idempotencyKey);
            return existingByIdempotency.get();
        }

        if (authority != null) {
            Optional<Report> existingByPotholeAndAuth = reportRepository.findByPotholeIdAndAuthorityId(
                    pothole.getId(), authority.getId()
            );
            if (existingByPotholeAndAuth.isPresent()) {
                log.info("Found existing report [{}] by pothole ID [{}] and authority ID [{}]",
                        existingByPotholeAndAuth.get().getId(), pothole.getId(), authority.getId());
                return existingByPotholeAndAuth.get();
            }
        }

        log.info("Creating new PENDING report for pothole [{}] and authority [{}] with idempotency key [{}]",
                pothole.getId(), (authority != null ? authority.getCode() : "NONE"), idempotencyKey);

        Report newReport = new Report(pothole, authority, idempotencyKey);
        return reportRepository.saveAndFlush(newReport);
    }

    /**
     * Transaction 2: Record an individual dispatch attempt and update report status.
     * Committed immediately after each dispatch attempt.
     */
    @Transactional
    public Report recordAttemptAndUpdateStatus(
            UUID reportId,
            int attemptNumber,
            ReportChannel channel,
            String idempotencyKey,
            boolean success,
            String responseSummary,
            String externalReference,
            boolean isFinalAttempt
    ) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + reportId));

        ReportAttemptStatus attemptStatus = success ? ReportAttemptStatus.SUCCESS : ReportAttemptStatus.FAILED;
        ReportAttempt attempt = new ReportAttempt(
                report,
                attemptNumber,
                channel,
                idempotencyKey,
                attemptStatus,
                responseSummary
        );
        reportAttemptRepository.saveAndFlush(attempt);
        report.addAttempt(attempt);

        if (success) {
            report.setStatus(ReportStatus.DISPATCHED);
            report.setExternalReference(externalReference);
            log.info("Report [{}] status updated to DISPATCHED on attempt #{} with ticket [{}]",
                    reportId, attemptNumber, externalReference);
        } else if (isFinalAttempt) {
            report.setStatus(ReportStatus.FAILED);
            log.warn("Report [{}] status updated to FAILED after exhausting all {} attempts",
                    reportId, attemptNumber);
        }

        return reportRepository.saveAndFlush(report);
    }
}
