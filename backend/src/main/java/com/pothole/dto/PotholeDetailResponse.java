package com.pothole.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pothole.dto.reporting.ReportResponse;
import com.pothole.model.enums.PotholeStatus;
import com.pothole.model.enums.SeverityClass;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PotholeDetailResponse(
        @JsonProperty("id") UUID id,
        @JsonProperty("latitude") Double latitude,
        @JsonProperty("longitude") Double longitude,
        @JsonProperty("addressText") String addressText,
        @JsonProperty("firstDetectedAt") Instant firstDetectedAt,
        @JsonProperty("severityScore") Double severityScore,
        @JsonProperty("severityClass") SeverityClass severityClass,
        @JsonProperty("maxConfidence") Double maxConfidence,
        @JsonProperty("status") PotholeStatus status,
        @JsonProperty("isDuplicate") Boolean isDuplicate,
        @JsonProperty("duplicateOfId") UUID duplicateOfId,
        @JsonProperty("authority") AuthorityResponse authority,
        @JsonProperty("authorityCode") String authorityCode,
        @JsonProperty("evidence") PotholeEvidence evidence,
        @JsonProperty("report") ReportResponse report,
        @JsonProperty("detections") List<DetectionResponse> detections,
        @JsonProperty("createdAt") Instant createdAt,
        @JsonProperty("updatedAt") Instant updatedAt
) {
    @JsonProperty("address_text")
    public String addressTextSnake() {
        return addressText;
    }

    @JsonProperty("first_detected_at")
    public Instant firstDetectedAtSnake() {
        return firstDetectedAt;
    }

    @JsonProperty("severity_score")
    public Double severityScoreSnake() {
        return severityScore;
    }

    @JsonProperty("severity_class")
    public SeverityClass severityClassSnake() {
        return severityClass;
    }

    @JsonProperty("max_confidence")
    public Double maxConfidenceSnake() {
        return maxConfidence;
    }

    @JsonProperty("is_duplicate")
    public Boolean isDuplicateSnake() {
        return isDuplicate;
    }

    @JsonProperty("duplicate_of_id")
    public UUID duplicateOfIdSnake() {
        return duplicateOfId;
    }

    @JsonProperty("authority_code")
    public String authorityCodeSnake() {
        return authorityCode;
    }

    @JsonProperty("created_at")
    public Instant createdAtSnake() {
        return createdAt;
    }

    @JsonProperty("updated_at")
    public Instant updatedAtSnake() {
        return updatedAt;
    }

    public record PotholeEvidence(
            @JsonProperty("representativeImageUrl") String representativeImageUrl,
            @JsonProperty("representativeKey") String representativeKey,
            @JsonProperty("rawMediaUrl") String rawMediaUrl,
            @JsonProperty("rawKey") String rawKey,
            @JsonProperty("mediaType") String mediaType
    ) {
        @JsonProperty("representative_image_url")
        public String representativeImageUrlSnake() {
            return representativeImageUrl;
        }

        @JsonProperty("representative_key")
        public String representativeKeySnake() {
            return representativeKey;
        }

        @JsonProperty("raw_media_url")
        public String rawMediaUrlSnake() {
            return rawMediaUrl;
        }

        @JsonProperty("raw_key")
        public String rawKeySnake() {
            return rawKey;
        }

        @JsonProperty("media_type")
        public String mediaTypeSnake() {
            return mediaType;
        }
    }
}
