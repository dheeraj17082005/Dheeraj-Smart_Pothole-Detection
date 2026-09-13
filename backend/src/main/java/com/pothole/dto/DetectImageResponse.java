package com.pothole.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record DetectImageResponse(
        @JsonProperty("job") JobResponse job,
        @JsonProperty("media_asset_id") UUID mediaAssetId,
        @JsonProperty("pothole_count") int potholeCount,
        @JsonProperty("pothole_created") boolean potholeCreated,
        @JsonProperty("pothole") PotholeResponse pothole,
        @JsonProperty("message") String message
) {}
