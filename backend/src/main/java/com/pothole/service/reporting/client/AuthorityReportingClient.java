package com.pothole.service.reporting.client;

public interface AuthorityReportingClient {
    /**
     * Submits a civic authority report payload to the designated authority channel.
     *
     * @param payload Structured dispatch payload containing defect, location, and authority metadata
     * @return Dispatch result indicating success or failure, external reference ticket, and channel details
     */
    ReportingDispatchResult submitReport(ReportingDispatchPayload payload);
}
