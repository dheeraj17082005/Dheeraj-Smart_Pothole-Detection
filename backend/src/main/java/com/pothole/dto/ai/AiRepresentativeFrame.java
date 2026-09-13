package com.pothole.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiRepresentativeFrame(
        @JsonProperty("frame_index") Integer frameIndex,
        @JsonProperty("timestamp_sec") Double timestampSec,
        @JsonProperty("reason") String reason
) {}
