package com.pothole.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pothole.model.enums.DetectionJobStatus;

import java.util.UUID;

public record DetectVideoAcceptedResponse(
        @JsonProperty("jobId") UUID jobId,
        @JsonProperty("status") DetectionJobStatus status,
        @JsonProperty("mediaAssetId") UUID mediaAssetId,
        @JsonProperty("pollUrl") String pollUrl
) {}
