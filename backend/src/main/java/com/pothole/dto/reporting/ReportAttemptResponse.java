package com.pothole.dto.reporting;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pothole.model.enums.ReportAttemptStatus;
import com.pothole.model.enums.ReportChannel;

import java.time.Instant;
import java.util.UUID;

public record ReportAttemptResponse(
        @JsonProperty("id") UUID id,
        @JsonProperty("attempt_number") Integer attemptNumber,
        @JsonProperty("channel") ReportChannel channel,
        @JsonProperty("idempotency_key") String idempotencyKey,
        @JsonProperty("attempt_timestamp") Instant attemptTimestamp,
        @JsonProperty("status") ReportAttemptStatus status,
        @JsonProperty("response_summary") String responseSummary
) {}
