package com.pothole.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pothole.model.enums.PotholeStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePotholeStatusRequest(
        @NotNull(message = "newStatus is required")
        @JsonProperty("newStatus")
        @JsonAlias({"new_status", "status"})
        PotholeStatus newStatus,

        @JsonProperty("changedBy")
        @JsonAlias({"changed_by"})
        String changedBy,

        @JsonProperty("notes")
        String notes
) {
}
