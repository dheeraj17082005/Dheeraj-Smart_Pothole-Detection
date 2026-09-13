package com.pothole.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DashboardStatsResponse(
        @JsonProperty("totalPotholes") long totalPotholes,
        @JsonProperty("reportedCount") long reportedCount,
        @JsonProperty("acknowledgedCount") long acknowledgedCount,
        @JsonProperty("inProgressCount") long inProgressCount,
        @JsonProperty("resolvedCount") long resolvedCount,
        @JsonProperty("highSeverityCount") long highSeverityCount
) {
    @JsonProperty("total_potholes")
    public long totalPotholesSnake() {
        return totalPotholes;
    }

    @JsonProperty("reported_count")
    public long reportedCountSnake() {
        return reportedCount;
    }

    @JsonProperty("acknowledged_count")
    public long acknowledgedCountSnake() {
        return acknowledgedCount;
    }

    @JsonProperty("in_progress_count")
    public long inProgressCountSnake() {
        return inProgressCount;
    }

    @JsonProperty("resolved_count")
    public long resolvedCountSnake() {
        return resolvedCount;
    }

    @JsonProperty("high_severity_count")
    public long highSeverityCountSnake() {
        return highSeverityCount;
    }
}
