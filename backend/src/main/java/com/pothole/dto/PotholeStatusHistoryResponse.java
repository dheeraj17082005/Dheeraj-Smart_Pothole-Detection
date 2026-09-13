package com.pothole.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pothole.model.PotholeStatusHistory;
import com.pothole.model.enums.PotholeStatus;

import java.time.Instant;
import java.util.UUID;

public record PotholeStatusHistoryResponse(
        @JsonProperty("id") UUID id,
        @JsonProperty("pothole_id") UUID potholeId,
        @JsonProperty("previous_status") PotholeStatus previousStatus,
        @JsonProperty("new_status") PotholeStatus newStatus,
        @JsonProperty("changed_at") Instant changedAt,
        @JsonProperty("changed_by") String changedBy,
        @JsonProperty("notes") String notes
) {
    public static PotholeStatusHistoryResponse fromEntity(PotholeStatusHistory history) {
        if (history == null) {
            return null;
        }
        UUID pId = (history.getPothole() != null) ? history.getPothole().getId() : null;
        return new PotholeStatusHistoryResponse(
                history.getId(),
                pId,
                history.getPreviousStatus(),
                history.getNewStatus(),
                history.getChangedAt(),
                history.getChangedBy(),
                history.getNotes()
        );
    }
}
