package com.pothole.dto;

import com.pothole.model.enums.RejectionReason;
import jakarta.validation.constraints.NotNull;

public record RejectReportRequest(
        @NotNull(message = "Rejection reason is required")
        RejectionReason reason,
        String notes
) {}
