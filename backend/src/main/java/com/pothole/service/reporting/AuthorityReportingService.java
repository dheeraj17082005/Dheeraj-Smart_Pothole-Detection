package com.pothole.service.reporting;

import com.pothole.dto.AuthorityResponse;
import com.pothole.exception.ObjectNotFoundException;
import com.pothole.exception.PotholeNotReportableException;
import com.pothole.model.CivicAuthority;
import com.pothole.model.Pothole;
import com.pothole.model.Report;
import com.pothole.model.enums.ReportStatus;
import com.pothole.repository.ReportRepository;
import com.pothole.service.reporting.client.AuthorityReportingClient;
import com.pothole.service.reporting.client.ReportingDispatchPayload;
import com.pothole.service.reporting.client.ReportingDispatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthorityReportingService {

    private static final Logger log = LoggerFactory.getLogger(AuthorityReportingService.class);

    public static final int MAX_ATTEMPTS = 3;

    private final ReportPersistenceService reportPersistenceService;
    private final AuthorityReportingClient reportingClient;
    private final ReportRepository reportRepository;

    @Value("${app.reporting.retry.initial-delay-ms:2000}")
    private long initialDelayMs = 2000L;

    public AuthorityReportingService(
            ReportPersistenceService reportPersistenceService,
            AuthorityReportingClient reportingClient,
            ReportRepository reportRepository
    ) {
        this.reportPersistenceService = reportPersistenceService;
        this.reportingClient = reportingClient;
        this.reportRepository = reportRepository;
    }

    /**
     * Dispatches a civic authority report for a valid, non-duplicate pothole with a resolved civic authority.
     * Uses strict transaction boundaries: no transactions held open during external dispatch or retry sleep.
     *
     * @param pothole Pothole entity to report
     * @return Dispatched or updated Report entity
     */
    public Report dispatchReportForPothole(Pothole pothole) {
        if (pothole == null) {
            throw new IllegalArgumentException("Pothole must not be null");
        }

        if (Boolean.TRUE.equals(pothole.getIsDuplicate())) {
            log.warn("Pothole [{}] is flagged as duplicate. Rejecting authority reporting.", pothole.getId());
            throw new PotholeNotReportableException(
                    "Pothole " + pothole.getId() + " is flagged as a duplicate and cannot be reported to authorities."
            );
        }

        if (pothole.getCivicAuthority() == null) {
            log.warn("Pothole [{}] has no resolved civic authority (UNKNOWN_AUTHORITY). Rejecting authority reporting.", pothole.getId());
            throw new PotholeNotReportableException(
                    "Pothole " + pothole.getId() + " has no assigned civic authority (UNKNOWN_AUTHORITY) and cannot be reported."
            );
        }

        CivicAuthority authority = pothole.getCivicAuthority();
        String idempotencyKey = buildIdempotencyKey(pothole.getId(), authority.getId());

        // Step 1: Create or fetch PENDING report in its own transaction
        Report report = reportPersistenceService.getOrCreatePendingReport(pothole, authority, idempotencyKey);

        // If report is already DISPATCHED, do not re-dispatch
        if (report.getStatus() == ReportStatus.DISPATCHED) {
            log.info("Report [{}] for pothole [{}] is already DISPATCHED. Returning existing report.",
                    report.getId(), pothole.getId());
            return report;
        }

        // Build dispatch payload
        AuthorityResponse authorityDto = new AuthorityResponse(
                authority.getId(),
                authority.getName(),
                authority.getCode(),
                authority.getDepartmentType()
        );

        double latitude = pothole.getLocation() != null ? pothole.getLocation().getY() : 0.0;
        double longitude = pothole.getLocation() != null ? pothole.getLocation().getX() : 0.0;

        ReportingDispatchPayload payload = new ReportingDispatchPayload(
                report.getId(),
                pothole.getId(),
                latitude,
                longitude,
                pothole.getFirstDetectedAt(),
                pothole.getSeverityScore(),
                pothole.getSeverityClass(),
                pothole.getMaxConfidence(),
                pothole.getRepresentativeKey(),
                authorityDto,
                idempotencyKey
        );

        // Step 2: Bounded retry loop (Max 3 attempts, exponential backoff: 2s, 4s, 8s)
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            log.info("Executing authority dispatch attempt #{} for report [{}] (Pothole: {}, Authority: {})",
                    attempt, report.getId(), pothole.getId(), authority.getCode());

            // External dispatch outside any DB transaction
            ReportingDispatchResult result = reportingClient.submitReport(payload);

            boolean isFinalAttempt = (attempt == MAX_ATTEMPTS);

            // Record attempt in short committed transaction
            report = reportPersistenceService.recordAttemptAndUpdateStatus(
                    report.getId(),
                    attempt,
                    result.channel(),
                    idempotencyKey,
                    result.success(),
                    result.responseSummary(),
                    result.externalReference(),
                    isFinalAttempt
            );

            if (result.success()) {
                log.info("Authority dispatch attempt #{} succeeded for report [{}] with ticket [{}]",
                        attempt, report.getId(), result.externalReference());
                return report;
            }

            if (!isFinalAttempt) {
                long backoffMs = calculateBackoff(attempt);
                log.warn("Dispatch attempt #{} failed for report [{}]. Backing off for {} ms before retry...",
                        attempt, report.getId(), backoffMs);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Dispatch retry sleep interrupted for report [{}]", report.getId());
                    break;
                }
            }
        }

        log.warn("All {} dispatch attempts exhausted for report [{}]. Final status: FAILED.",
                MAX_ATTEMPTS, report.getId());
        return report;
    }

    /**
     * Builds the deterministic idempotency key for a pothole report.
     * Format: report-{potholeId}-{authorityId}
     */
    public String buildIdempotencyKey(UUID potholeId, UUID authorityId) {
        return "report-" + potholeId + "-" + authorityId;
    }

    /**
     * Calculates exponential backoff: attempt 1 -> initialDelayMs (e.g. 2000ms),
     * attempt 2 -> initialDelayMs * 2 (e.g. 4000ms), attempt 3 -> initialDelayMs * 4 (e.g. 8000ms).
     */
    private long calculateBackoff(int attempt) {
        return initialDelayMs * (1L << (attempt - 1));
    }

    public Report getReport(UUID reportId) {
        return reportRepository.findByIdWithDetails(reportId)
                .orElseThrow(() -> new ObjectNotFoundException("Report not found with ID: " + reportId));
    }

    public Optional<Report> findReportForPothole(UUID potholeId) {
        return reportRepository.findByPotholeIdWithDetails(potholeId);
    }

    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }
}
