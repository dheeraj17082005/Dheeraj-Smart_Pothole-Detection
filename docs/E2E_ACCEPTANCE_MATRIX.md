# End-to-End Acceptance Test Matrix — PotholeX

This matrix documents the 14 critical end-to-end security, workflow, and domain scenario test cases verified for **PotholeX**.

---

## Scenario Verification Matrix

| # | Scenario Description | Input / Trigger | Expected Outcome | System Component | Status |
|---|---|---|---|---|---|
| 1 | Valid Citizen Submission & Officer Acceptance | Citizen uploads valid pothole image at `28.6210, 77.2210` | Report created (`SUBMITTED`). Officer Sharma notified. Officer Sharma accepts report → Status becomes `ACCEPTED`. Citizen notified. | `ImageDetectionService`, `PotholeStatusTransitionService` | PASS |
| 2 | No-Pothole Evidence Submission | Citizen uploads clear road image with 0 detections | System returns `NO_POTHOLE_DETECTED` status ("No pothole was detected in the submitted evidence"). Normal report NOT created. | `AiDetectionService`, `ImageDetectionService` | PASS |
| 3 | Active Duplicate Submission | Citizen uploads report near active pothole (within 15m) | System identifies existing active report and returns `POSSIBLE_DUPLICATE` / `EXISTING_ACTIVE_REPORT` with existing report ID. No duplicate work item created. | `DeduplicationService` | PASS |
| 4 | Recently Resolved Duplicate Blocking | Citizen uploads report near pothole resolved 5 days ago (within 15m & 30d) | System blocks creation and returns `POTHOLE_ALREADY_RESOLVED` with resolution date and report ID. | `DeduplicationService` | PASS |
| 5 | Reappeared Pothole After Window | Citizen uploads report near pothole resolved 45 days ago | Since resolution window (>30d) passed, system permits fresh report submission for returning defect. | `DeduplicationService` | PASS |
| 6 | Officer Rejection with Reason | Officer reviews report and selects `INSUFFICIENT_EVIDENCE` + note | Status becomes `REJECTED`. Rejection reason stored. Citizen receives rejection notification with reason. | `PotholeStatusTransitionService` | PASS |
| 7 | Officer Starts Work | Officer clicks `[Start Work]` on accepted report | Status transitions `ACCEPTED` → `IN_PROGRESS`. Citizen receives work started notification. | `PotholeStatusTransitionService` | PASS |
| 8 | Officer Resolves Work | Officer clicks `[Mark Resolved]` on in-progress report | Status transitions `IN_PROGRESS` → `RESOLVED`. Citizen receives resolution notification. | `PotholeStatusTransitionService` | PASS |
| 9 | Citizen Status Mutation Attempt | Citizen sends `PATCH /api/v1/potholes/:id/status` request | Backend security filter chain returns `403 Forbidden`. | `WebSecurityConfig`, `PotholeController` | PASS |
| 10 | Officer Out of Jurisdiction Attempt | Officer A attempts accessing report in Officer B's jurisdiction | Backend returns `403 Forbidden` ("Report is outside officer spatial jurisdiction"). | `OfficerService` | PASS |
| 11 | Unverified Officer Action Attempt | Officer with status `PENDING_VERIFICATION` attempts report inbox query | Backend returns `403 Forbidden` ("Officer account is pending verification"). | `OfficerService` | PASS |
| 12 | User Modifying Other's Report | User A attempts updating User B's report details | Security principal validation rejects request with `403 Forbidden`. | `PotholeQueryService` | PASS |
| 13 | Frontend Action Button Control | UI renders state-appropriate controls based on role and status | Citizens see no status buttons. Officers see `[Accept] [Reject]` on `SUBMITTED`, `[Start Work]` on `ACCEPTED`, `[Mark Resolved]` on `IN_PROGRESS`. | React UI Components | PASS |
| 14 | Direct REST Endpoint Security | Direct curl requests to restricted endpoints without valid JWT | Security filter chain returns `401 Unauthorized` / `403 Forbidden`. | `JwtAuthenticationFilter` | PASS |
