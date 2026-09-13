package com.pothole.service.deduplication;

import com.pothole.model.Pothole;

import java.util.UUID;

public record DeduplicationResult(
        boolean isDuplicate,
        Pothole parentPothole,
        UUID duplicateOfId
) {
    public static DeduplicationResult duplicate(Pothole parent) {
        return new DeduplicationResult(true, parent, parent.getId());
    }

    public static DeduplicationResult nonDuplicate() {
        return new DeduplicationResult(false, null, null);
    }
}
