package com.pothole.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pothole.dto.ai.AiBoundingBox;
import java.util.UUID;

public record DetectionResponse(
        @JsonProperty("id") UUID id,
        @JsonProperty("box") AiBoundingBox box,
        @JsonProperty("confidence") Double confidence,
        @JsonProperty("visual_area_ratio") Double visualAreaRatio,
        @JsonProperty("frame_index") Integer frameIndex,
        @JsonProperty("frame_timestamp_sec") Double frameTimestampSec
) {}
