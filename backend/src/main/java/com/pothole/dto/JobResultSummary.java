package com.pothole.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JobResultSummary(
        @JsonProperty("totalFramesSampled") int totalFramesSampled,
        @JsonProperty("framesWithPotholes") int framesWithPotholes,
        @JsonProperty("potholesCreated") int potholesCreated,
        @JsonProperty("duplicatesDetected") int duplicatesDetected
) {}
