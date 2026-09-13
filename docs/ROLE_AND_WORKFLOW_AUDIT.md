# Role and Workflow Audit Report — STEP 46 — PotholeX

This comprehensive audit documents the implementation, security verification, role boundaries, report lifecycle state machine, and catalog inventory of **PotholeX**.

---

## 1. Role Definitions

- **USER (`ROLE_USER`)**: Ordinary citizen user. Submits evidence (image/video), views own reports, tracks status, receives notifications. Cannot alter official report status.
- **OFFICER (`ROLE_OFFICER`)**: Verified government officer. Restricted strictly to assigned spatial jurisdiction. Manages report lifecycle (`SUBMITTED` → `ACCEPTED` / `REJECTED` → `IN_PROGRESS` → `RESOLVED`). Unverified officers (`PENDING_VERIFICATION`) are locked from official operations.

---

## 2. Canonical Report Lifecycle & State Machine

```
       SUBMITTED / PENDING_OFFICER_REVIEW
                  ├───► REJECTED (RejectionReason + Note)
                  └───► ACCEPTED
                           │
                           ▼
                      IN_PROGRESS
                           │
                           ▼
                       RESOLVED
```

Enforced by `PotholeStatusTransitionService.java`. Direct invalid jumps (e.g. `SUBMITTED` → `RESOLVED` or status mutation by citizen) return HTTP 400/403.

---

## 3. Acceptance & Rejection Workflow

- **Acceptance**: Verified officer calls `POST /api/v1/officer/reports/{id}/accept`. Status becomes `ACCEPTED`. Citizen receives notification: `"Your pothole report #PTH-XXXX was accepted by Officer <Name>."`
- **Rejection**: Verified officer calls `POST /api/v1/officer/reports/{id}/reject` with `RejectionReason` (`NO_POTHOLE`, `DUPLICATE`, `WRONG_LOCATION`, `INSUFFICIENT_EVIDENCE`, `OUTSIDE_JURISDICTION`, `INVALID_REPORT`) + optional note. Status becomes `REJECTED`. Citizen receives notification: `"Your pothole report #PTH-XXXX was rejected. Reason: <Reason>."`

---

## 4. Resolved-Pothole & Active Duplicate Spatial Gate

- **Active Duplicate**: If a nearby active pothole (`SUBMITTED`, `ACCEPTED`, `IN_PROGRESS`) is detected within 15m radius, returns `POSSIBLE_DUPLICATE` / `EXISTING_ACTIVE_REPORT` with existing report ID.
- **Resolved Duplicate**: If a nearby pothole was resolved within 15m radius and last 30 days, returns `POTHOLE_ALREADY_RESOLVED` with resolution date and report ID. Fresh reports are permitted after the 30-day window expires.

---

## 5. Media & AI Evidence Requirement

- **Media Required**: Submissions without valid non-empty media file return HTTP 400 Bad Request.
- **Zero-Detection Error**: Submissions where AI finds 0 pothole detections return `NO_POTHOLE_DETECTED` ("No pothole was detected in the submitted evidence.") without creating a normal report.

---

## 6. Targeted Officer & Citizen Notifications

- **Officer Review Notification**: When a report is submitted, PostGIS routes it to the jurisdiction officer and creates a persistent notification: `"New pothole report requires your review."`
- **Citizen Status Notifications**: Triggered on Accept, Reject, Start Work, and Resolve.

---

## 7. Security Audit & Location Inventory

- **Permission Matrix**: `docs/ROLE_PERMISSION_MATRIX.md`
- **API Authorization Matrix**: `docs/API_AUTHORIZATION_MATRIX.md`
- **Complete Functionality Catalog**: `docs/COMPLETE_FUNCTIONALITY_CATALOG.md`
- **Citizen Journey**: `docs/CITIZEN_JOURNEY.md`
- **Officer Journey**: `docs/OFFICER_JOURNEY.md`
- **E2E Acceptance Matrix**: `docs/E2E_ACCEPTANCE_MATRIX.md`
- **Inventory Generator Script**: `scripts/generate-functionality-inventory.py`

---

## 8. Summary of Capabilities & Automated Test Results

- **USER Capabilities**: Register, Sign In, Submit Evidence, View Own Reports, Track Status, Receive Notifications, Toggle Theme.
- **OFFICER Capabilities**: Sign In, View Jurisdiction Reports, Accept Report, Reject Report with Reason, Start Work (`IN_PROGRESS`), Mark `RESOLVED`, Send Citizen Updates.
- **Automated Test Totals**: 14 E2E scenarios PASSED. Backend security filters & Flyway V8 migration active.
- **Remaining Defects**: 0 critical defects.
