# Municipal Officer Role Journey — PotholeX

This document traces the complete end-to-end user experience and security boundary for a **Municipal Officer (`ROLE_OFFICER`)** in **PotholeX**.

---

## Workflow Steps

```
Register Officer Account (Select Department, Officer Code, Office Coordinates & Radius)
        │
        ▼
Verification Gate (Status: PENDING_VERIFICATION)
        │
        ├───► Unverified Officer ──► Lock Spatial Reports Inbox & Map (HTTP 403 on API)
        └───► Verified Officer (Status: VERIFIED)
                    │
                    ▼
          Sign In & Access Officer Portal (/officer/dashboard)
                    │
                    ▼
          Receive Notification: "New pothole report requires your review."
                    │
                    ▼
          Inspect Citizen Report (Evidence Image, AI Detections, GPS Location, Severity)
                    │
                    ├───► [REJECT REPORT] ──► Select RejectionReason (e.g. INSUFFICIENT_EVIDENCE)
                    │                         + Note ──► Citizen Notified ──► Status: REJECTED
                    │
                    └───► [ACCEPT REPORT] ──► Status: ACCEPTED ──► Citizen Notified
                                                   │
                                                   ▼
                                            [START WORK] ──► Status: IN_PROGRESS ──► Citizen Notified
                                                   │
                                                   ▼
                                            [MARK RESOLVED] ──► Status: RESOLVED ──► Citizen Notified
```

---

## Officer Permissions & Boundaries

### Allowed Actions
- View reports located strictly within assigned spatial jurisdiction (`ST_DWithin`).
- Receive persistent in-app review notifications when a new report is routed to jurisdiction.
- Inspect original evidence image, bounding boxes, severity score, and spatial map location.
- **Accept Report**: Move status from `SUBMITTED` / `PENDING_OFFICER_REVIEW` to `ACCEPTED`.
- **Reject Report**: Move status to `REJECTED` with mandatory `RejectionReason` enum and optional note.
- **Start Work**: Move status from `ACCEPTED` to `IN_PROGRESS`.
- **Mark Resolved**: Move status from `IN_PROGRESS` to `RESOLVED`.
- Toggle between Light Mode and Dark Mode.

### Restricted Actions (Blocked by Security Context)
- Cannot access or view citizen reports outside assigned spatial jurisdiction (HTTP 403 Forbidden).
- Cannot perform officer operations if status is `PENDING_VERIFICATION` (HTTP 403 Forbidden).
- Cannot submit reports as a citizen while in officer role (`/upload` route restricted to `ROLE_USER`).
- Cannot modify original citizen media, capture timestamp, or submitted GPS coordinates.
- Cannot jump directly from `SUBMITTED` to `RESOLVED` (enforced by canonical transition service).
