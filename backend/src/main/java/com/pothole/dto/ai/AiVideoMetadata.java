package com.pothole.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiVideoMetadata(
        @JsonProperty("fps") Double fps,
        @JsonProperty("duration_seconds") Double durationSeconds,
        @JsonProperty("sample_fps") Double sampleFps,
        @JsonProperty("total_frames_sampled") Integer totalFramesSampled
) {}
