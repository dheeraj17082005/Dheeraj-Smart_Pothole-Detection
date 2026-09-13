package com.pothole.dto.reporting;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pothole.dto.AuthorityResponse;
import com.pothole.model.Report;
import com.pothole.model.enums.ReportStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record ReportResponse(
        @JsonProperty("id") UUID id,
        @JsonProperty("pothole_id") UUID potholeId,
        @JsonProperty("authority") AuthorityResponse authority,
        @JsonProperty("status") ReportStatus status,
        @JsonProperty("idempotency_key") String idempotencyKey,
        @JsonProperty("created_timestamp") Instant createdTimestamp,
        @JsonProperty("external_reference") String externalReference,
        @JsonProperty("attempts") List<ReportAttemptResponse> attempts
) {
    public static ReportResponse fromEntity(Report report) {
        if (report == null) {
            return null;
        }

        AuthorityResponse authorityDto = (report.getAuthority() != null)
                ? new AuthorityResponse(
                        report.getAuthority().getId(),
                        report.getAuthority().getName(),
                        report.getAuthority().getCode(),
                        report.getAuthority().getDepartmentType()
                  )
                : null;

        List<ReportAttemptResponse> attemptDtos = (report.getAttempts() != null)
                ? report.getAttempts().stream()
                        .map(a -> new ReportAttemptResponse(
                                a.getId(),
                                a.getAttemptNumber(),
                                a.getChannel(),
                                a.getIdempotencyKey(),
                                a.getAttemptTimestamp(),
                                a.getStatus(),
                                a.getResponseSummary()
                        ))
                        .toList()
                : Collections.emptyList();

        UUID potholeId = (report.getPothole() != null) ? report.getPothole().getId() : null;

        return new ReportResponse(
                report.getId(),
                potholeId,
                authorityDto,
                report.getStatus(),
                report.getIdempotencyKey(),
                report.getCreatedTimestamp(),
                report.getExternalReference(),
                attemptDtos
        );
    }
}
