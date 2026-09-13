package com.pothole.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pothole.model.enums.PotholeStatus;
import com.pothole.model.enums.SeverityClass;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PotholeResponse(
        @JsonProperty("id") UUID id,
        @JsonProperty("latitude") Double latitude,
        @JsonProperty("longitude") Double longitude,
        @JsonProperty("address_text") String addressText,
        @JsonProperty("first_detected_at") Instant firstDetectedAt,
        @JsonProperty("severity_score") Double severityScore,
        @JsonProperty("severity_class") SeverityClass severityClass,
        @JsonProperty("max_confidence") Double maxConfidence,
        @JsonProperty("status") PotholeStatus status,
        @JsonProperty("is_duplicate") Boolean isDuplicate,
        @JsonProperty("duplicate_of_id") UUID duplicateOfId,
        @JsonProperty("authority") AuthorityResponse authority,
        @JsonProperty("authority_code") String authorityCode,
        @JsonProperty("representative_key") String representativeKey,
        @JsonProperty("representative_image_url") String representativeImageUrl,
        @JsonProperty("detections") List<DetectionResponse> detections,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
    @JsonProperty("isDuplicate")
    public Boolean isDuplicateCamel() {
        return isDuplicate;
    }

    @JsonProperty("duplicateOfId")
    public UUID duplicateOfIdCamel() {
        return duplicateOfId;
    }

    @JsonProperty("addressText")
    public String addressTextCamel() {
        return addressText;
    }

    @JsonProperty("firstDetectedAt")
    public Instant firstDetectedAtCamel() {
        return firstDetectedAt;
    }

    @JsonProperty("severityScore")
    public Double severityScoreCamel() {
        return severityScore;
    }

    @JsonProperty("severityClass")
    public SeverityClass severityClassCamel() {
        return severityClass;
    }

    @JsonProperty("maxConfidence")
    public Double maxConfidenceCamel() {
        return maxConfidence;
    }

    @JsonProperty("authorityCode")
    public String authorityCodeCamel() {
        return authorityCode;
    }

    @JsonProperty("representativeKey")
    public String representativeKeyCamel() {
        return representativeKey;
    }

    @JsonProperty("representativeImageUrl")
    public String representativeImageUrlCamel() {
        return representativeImageUrl;
    }

    @JsonProperty("createdAt")
    public Instant createdAtCamel() {
        return createdAt;
    }

    @JsonProperty("updatedAt")
    public Instant updatedAtCamel() {
        return updatedAt;
    }
}
