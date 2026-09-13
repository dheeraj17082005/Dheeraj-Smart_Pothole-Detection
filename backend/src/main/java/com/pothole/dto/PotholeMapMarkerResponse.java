package com.pothole.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pothole.model.enums.PotholeStatus;
import com.pothole.model.enums.SeverityClass;

import java.time.Instant;
import java.util.UUID;

public record PotholeMapMarkerResponse(
        @JsonProperty("id") UUID id,
        @JsonProperty("latitude") Double latitude,
        @JsonProperty("longitude") Double longitude,
        @JsonProperty("severity_class") SeverityClass severityClass,
        @JsonProperty("severity_score") Double severityScore,
        @JsonProperty("status") PotholeStatus status,
        @JsonProperty("confidence") Double confidence,
        @JsonProperty("first_detected_at") Instant firstDetectedAt,
        @JsonProperty("authority_name") String authorityName,
        @JsonProperty("authority_code") String authorityCode,
        @JsonProperty("is_duplicate") Boolean isDuplicate
) {
    @JsonProperty("severityClass")
    public SeverityClass severityClassCamel() {
        return severityClass;
    }

    @JsonProperty("severityScore")
    public Double severityScoreCamel() {
        return severityScore;
    }

    @JsonProperty("firstDetectedAt")
    public Instant firstDetectedAtCamel() {
        return firstDetectedAt;
    }

    @JsonProperty("authorityName")
    public String authorityNameCamel() {
        return authorityName;
    }

    @JsonProperty("authorityCode")
    public String authorityCodeCamel() {
        return authorityCode;
    }

    @JsonProperty("isDuplicate")
    public Boolean isDuplicateCamel() {
        return isDuplicate;
    }
}
