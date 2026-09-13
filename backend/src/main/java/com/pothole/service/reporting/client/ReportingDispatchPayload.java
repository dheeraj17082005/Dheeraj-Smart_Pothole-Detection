package com.pothole.service.reporting.client;

import com.pothole.dto.AuthorityResponse;
import com.pothole.model.enums.SeverityClass;

import java.time.Instant;
import java.util.UUID;

public record ReportingDispatchPayload(
        UUID reportId,
        UUID potholeId,
        double latitude,
        double longitude,
        Instant detectedTimestamp,
        Double severityScore,
        SeverityClass severityClass,
        Double confidence,
        String evidenceReference,
        AuthorityResponse authority,
        String idempotencyKey
) {}
