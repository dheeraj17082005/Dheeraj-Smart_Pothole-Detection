package com.pothole.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiDetection(
        @JsonProperty("box") AiBoundingBox box,
        @JsonProperty("confidence") double confidence,
        @JsonProperty("class_id") int classId,
        @JsonProperty("class_name") String className,
        @JsonProperty("visual_area_ratio") double visualAreaRatio
) {}
