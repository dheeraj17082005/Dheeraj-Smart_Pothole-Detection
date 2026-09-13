# Role-Based End-to-End Test & Acceptance Matrix

| Scenario ID | Actor | Action Performed | Expected UI Behavior | Expected Backend Status | Verification Result |
|---|---|---|---|---|---|
| E2E-01 | Citizen (ROLE_USER) | Login & Navigate to Home `/` | Renders `DashboardPage` (My Reports, Report Pothole, Live Map) | 200 OK | PASSED |
| E2E-02 | Citizen (ROLE_USER) | Navigate to `/upload` & Submit Evidence | Defect saved, AI inference triggered, status set to `REPORTED` / `PENDING_OFFICER_REVIEW` | 201 Created | PASSED |
| E2E-03 | Citizen (ROLE_USER) | Attempt direct access to `/officer/dashboard` | Redirected to `/` by `ProtectedRoute` | 403 / Redirect | PASSED |
| E2E-04 | Citizen (ROLE_USER) | Direct API call to `POST /api/v1/officer/potholes/:id/accept` | Request rejected with HTTP 403 | 403 Forbidden | PASSED |
| E2E-05 | Officer (ROLE_OFFICER, PENDING) | Login & Navigate to `/officer/dashboard` | Officer profile shown, verification status alert displayed, operational controls locked | 200 OK (Profile) / 403 (Reports) | PASSED |
| E2E-06 | Officer (ROLE_OFFICER, PENDING) | Attempt direct API call to `GET /api/v1/officer/potholes` | Access denied due to unverified status | 403 Forbidden | PASSED |
| E2E-07 | Officer (ROLE_OFFICER, VERIFIED) | Login & Navigate to Home `/` | Automatically routed to `OfficerDashboardPage` | 200 OK | PASSED |
| E2E-08 | Officer (ROLE_OFFICER, VERIFIED) | Accept incoming report in jurisdiction | Status transitions from `REPORTED` -> `ACCEPTED` | 200 OK | PASSED |
| E2E-09 | Officer (ROLE_OFFICER, VERIFIED) | Start work on accepted report | Status transitions from `ACCEPTED` -> `IN_PROGRESS` | 200 OK | PASSED |
| E2E-10 | Officer (ROLE_OFFICER, VERIFIED) | Mark report resolved | Status transitions from `IN_PROGRESS` -> `RESOLVED` | 200 OK | PASSED |
| E2E-11 | Officer (ROLE_OFFICER, VERIFIED) | Reject invalid report with reason | Status transitions to `REJECTED` with rejection reason stored | 200 OK | PASSED |
| E2E-12 | Officer (ROLE_OFFICER, VERIFIED) | Direct API call to `POST /api/v1/potholes/upload` | Request denied since officers cannot report defects | 403 Forbidden | PASSED |
