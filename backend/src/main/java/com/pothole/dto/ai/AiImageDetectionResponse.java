package com.pothole.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiImageDetectionResponse(
        @JsonProperty("model") AiModelMetadata model,
        @JsonProperty("image") AiImageMetadata image,
        @JsonProperty("pothole_count") int potholeCount,
        @JsonProperty("max_confidence") double maxConfidence,
        @JsonProperty("max_area_ratio") double maxAreaRatio,
        @JsonProperty("detections") List<AiDetection> detections
) {}
