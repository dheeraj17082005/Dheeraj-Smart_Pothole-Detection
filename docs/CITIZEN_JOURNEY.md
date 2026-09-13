# Citizen Role Journey — PotholeX

This document traces the complete end-to-end user experience and security boundary for a **Citizen (`ROLE_USER`)** in **PotholeX**.

---

## Workflow Steps

```
Register Citizen Account
        │
        ▼
Sign In with Email & Password
        │
        ▼
Navigate to "Report Defect" (/upload)
        │
        ▼
Upload Road Image/Dashcam Video + Enter Location Coordinates
        │
        ▼
Backend Evidence Validation & AI Detection Inference
        │
        ├───► [No Media] ──► HTTP 400 Validation Error
        ├───► [No Pothole Detected] ──► "No pothole detected in evidence" Notice
        ├───► [Resolved Duplicate <= 30d] ──► "Pothole already resolved on <date>" Notice
        ├───► [Active Duplicate] ──► "Existing active report is already being handled"
        └───► [Valid Pothole] ──► Report Created (Status: SUBMITTED / PENDING_OFFICER_REVIEW)
                                        │
                                        ▼
                         Jurisdiction Assigned & Notification Sent to Responsible Officer
                                        │
                                        ├───► Officer Accepts ──► Citizen Receives Acceptance Notification
                                        └───► Officer Rejects ──► Citizen Receives Rejection Notification (Reason displayed)
                                        │
                                        ▼
                         Work Progresses (IN_PROGRESS → RESOLVED)
                                        │
                                        ▼
                         Citizen Receives Resolution Notification & Views Resolved Map Record
```

---

## Citizen Permissions & Boundaries

### Allowed Actions
- Submit road defect image or dashcam video.
- Provide GPS coordinates and location address text.
- Review AI detections, confidence score, visual severity, and assigned civic authority.
- Track own submitted reports in `/potholes` registry.
- View assigned officer's public identity and department.
- Receive persistent in-app notifications on report status updates.
- Toggle between Light Mode and Dark Mode.

### Restricted Actions (Blocked by Security Context)
- Cannot change official report status (no "Accept", "Reject", "Start Work", or "Mark Resolved" buttons).
- Cannot access Officer Portal (`/officer/dashboard`). Attempting access redirects or returns HTTP 403.
- Cannot modify original media, capture timestamp, or submitted GPS coordinates.
