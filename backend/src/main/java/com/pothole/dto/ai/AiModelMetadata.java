package com.pothole.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiModelMetadata(
        @JsonProperty("name") String name,
        @JsonProperty("version") String version
) {}
