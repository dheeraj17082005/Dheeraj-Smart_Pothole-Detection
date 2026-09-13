package com.pothole.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiFrameDetection(
        @JsonProperty("frame_index") Integer frameIndex,
        @JsonProperty("timestamp_sec") Double timestampSec,
        @JsonProperty("image_width") Integer imageWidth,
        @JsonProperty("image_height") Integer imageHeight,
        @JsonProperty("detections") List<AiDetection> detections
) {}
