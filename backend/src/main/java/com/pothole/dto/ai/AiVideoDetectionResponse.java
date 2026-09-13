package com.pothole.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiVideoDetectionResponse(
        @JsonProperty("model") AiModelMetadata model,
        @JsonProperty("video") AiVideoMetadata video,
        @JsonProperty("frames") List<AiFrameDetection> frames,
        @JsonProperty("representative_frame") AiRepresentativeFrame representativeFrame
) {}
