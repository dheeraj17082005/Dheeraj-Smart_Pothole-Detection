package com.pothole.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pothole.model.enums.DetectionJobStatus;

import java.time.Instant;
import java.util.UUID;

public record JobDetailResponse(
        @JsonProperty("jobId") UUID jobId,
        @JsonProperty("status") DetectionJobStatus status,
        @JsonProperty("startedAt") Instant startedAt,
        @JsonProperty("completedAt") Instant completedAt,
        @JsonProperty("progress") Double progress,
        @JsonProperty("result") JobResultSummary result,
        @JsonProperty("error") String error
) {}
