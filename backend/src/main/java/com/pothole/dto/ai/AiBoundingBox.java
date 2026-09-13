package com.pothole.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiBoundingBox(
        @JsonProperty("xmin") int xmin,
        @JsonProperty("ymin") int ymin,
        @JsonProperty("xmax") int xmax,
        @JsonProperty("ymax") int ymax
) {}
