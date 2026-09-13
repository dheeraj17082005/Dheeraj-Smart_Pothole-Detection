package com.pothole.service.reporting.client;

import com.pothole.model.enums.ReportChannel;

public record ReportingDispatchResult(
        boolean success,
        String externalReference,
        String responseSummary,
        ReportChannel channel
) {
    public static ReportingDispatchResult success(String externalReference, String responseSummary, ReportChannel channel) {
        return new ReportingDispatchResult(true, externalReference, responseSummary, channel);
    }

    public static ReportingDispatchResult failure(String responseSummary, ReportChannel channel) {
        return new ReportingDispatchResult(false, null, responseSummary, channel);
    }
}
