package com.pothole.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiErrorResponse(
        @JsonProperty("error") AiErrorDetail error
) {
    public record AiErrorDetail(
            @JsonProperty("code") String code,
            @JsonProperty("message") String message
    ) {}
}
