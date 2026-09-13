package com.pothole.service.reporting.client;

import com.pothole.model.enums.ReportChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MockAuthorityReportingClient implements AuthorityReportingClient {

    private static final Logger log = LoggerFactory.getLogger(MockAuthorityReportingClient.class);

    private final AtomicInteger ticketSequence = new AtomicInteger(1);
    private final AtomicInteger attemptCount = new AtomicInteger(0);

    private volatile boolean simulatePermanentFailure = false;
    private volatile int failFirstNAttempts = 0;

    @Override
    public ReportingDispatchResult submitReport(ReportingDispatchPayload payload) {
        int currentAttempt = attemptCount.incrementAndGet();

        ReportChannel channel = (payload.authority() != null && payload.authority().departmentType() != null
                && payload.authority().departmentType().equalsIgnoreCase("HIGHWAY"))
                ? ReportChannel.MOCK_WEBHOOK
                : ReportChannel.MOCK_EMAIL;

        if (simulatePermanentFailure || currentAttempt <= failFirstNAttempts) {
            log.warn("MockAuthorityReportingClient: Simulating dispatch failure (attempt #{}, permanent={}, failFirstN={}) for report [{}]",
                    currentAttempt, simulatePermanentFailure, failFirstNAttempts, payload.reportId());
            return ReportingDispatchResult.failure(
                    "Simulated external gateway timeout: Authority dispatch endpoint unreachable (Mock error)",
                    channel
            );
        }

        String dept = (payload.authority() != null && payload.authority().departmentType() != null)
                ? payload.authority().departmentType()
                : "CIVIC";
        String ticketId = dept + "-DEMO-2026-" + String.format("%06d", ticketSequence.getAndIncrement());
        String authorityName = (payload.authority() != null) ? payload.authority().name() : "Unknown Authority";

        String summary = String.format(
                "Simulated dispatch successful. Mock civic work order created for %s (Ticket: %s, Channel: %s)",
                authorityName, ticketId, channel
        );

        log.info("MockAuthorityReportingClient: Dispatch successful for report [{}], pothole [{}]. Generated reference: {}",
                payload.reportId(), payload.potholeId(), ticketId);

        return ReportingDispatchResult.success(ticketId, summary, channel);
    }

    public void setSimulatePermanentFailure(boolean simulatePermanentFailure) {
        this.simulatePermanentFailure = simulatePermanentFailure;
    }

    public void setFailFirstNAttempts(int count) {
        this.failFirstNAttempts = count;
    }

    public void reset() {
        this.simulatePermanentFailure = false;
        this.failFirstNAttempts = 0;
        this.attemptCount.set(0);
    }

    public int getAttemptCount() {
        return attemptCount.get();
    }
}
