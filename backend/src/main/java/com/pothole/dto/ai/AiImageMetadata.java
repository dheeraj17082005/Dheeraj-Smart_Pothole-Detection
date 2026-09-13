package com.pothole.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiImageMetadata(
        @JsonProperty("width") int width,
        @JsonProperty("height") int height
) {}
