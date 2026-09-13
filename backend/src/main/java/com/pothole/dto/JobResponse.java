package com.pothole.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pothole.model.enums.DetectionJobStatus;
import java.time.Instant;
import java.util.UUID;

public record JobResponse(
        @JsonProperty("id") UUID id,
        @JsonProperty("status") DetectionJobStatus status,
        @JsonProperty("started_at") Instant startedAt,
        @JsonProperty("completed_at") Instant completedAt,
        @JsonProperty("error_summary") String errorSummary
) {}
