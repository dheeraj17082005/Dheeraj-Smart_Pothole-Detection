# Complete Functionality Catalog — PotholeX

This catalog provides an end-to-end inventory of every capability in **PotholeX**, specifying the actor, entry point, REST endpoint, data model, security rule, status, and E2E validation status.

| Feature | Actor | UI Entry | API Endpoint | Data Model | Permission Rule | Status | E2E Tested |
|---|---|---|---|---|---|---|---|
| Registration (Citizen) | PUBLIC | `/register` | `POST /api/v1/auth/register` | `User` | Unauthenticated | Active | YES |
| Registration (Officer) | PUBLIC | `/officer/register` | `POST /api/v1/auth/officer/register` | `User`, `OfficerProfile` | Unauthenticated (`PENDING_VERIFICATION`) | Active | YES |
| Authentication (Login) | PUBLIC | `/login` | `POST /api/v1/auth/login` | `User` | Unauthenticated (Issues JWT) | Active | YES |
| User Profile | USER / OFFICER | Topbar Pill | `GET /api/v1/auth/me` | `User` | Authenticated principal | Active | YES |
| Image Report Evidence Upload | USER | `/upload` | `POST /api/v1/potholes/detect-image` | `Pothole`, `MediaAsset`, `Detection` | `ROLE_USER` / Authenticated | Active | YES |
| Dashcam Survey Video Upload | USER | `/upload` | `POST /api/v1/potholes/detect-video` | `DetectionJob`, `Pothole` | `ROLE_USER` / Authenticated | Active | YES |
| AI Detection Inference | SYSTEM | Background Service | FastAPI `/detect-image`, `/detect-video` | YOLOv8 ONNX | Internal microservice | Active | YES |
| Visual Severity Scoring | SYSTEM | ImageDetectionService | Internal Service | `SeverityClass` | Internal algorithm | Active | YES |
| PostGIS Authority Resolution | SYSTEM | AuthorityResolverService | `ST_Contains` / `ST_DWithin` | `CivicAuthority`, `Polygon` | PostGIS spatial query | Active | YES |
| Active Duplicate Detection | SYSTEM | DeduplicationService | `ST_DWithin` (15m radius) | `potholes` | Returns `POSSIBLE_DUPLICATE` | Active | YES |
| Resolved Duplicate Gate | SYSTEM | DeduplicationService | `ST_DWithin` (15m radius, 30d window) | `potholes` | Returns `POTHOLE_ALREADY_RESOLVED` | Active | YES |
| No-Media Evidence Rejection | SERVER | Upload Controller | Validation Filter | N/A | HTTP 400 Bad Request | Active | YES |
| Zero-Detection Error Notice | SERVER | ImageDetectionService | AI Inference Check | N/A | Returns `NO_POTHOLE_DETECTED` | Active | YES |
| Officer Jurisdiction Inbox | OFFICER | `/officer/dashboard` | `GET /api/v1/officer/reports` | `Pothole`, `OfficerJurisdiction` | `ROLE_OFFICER` & `VERIFIED` & Spatial | Active | YES |
| Officer Report Review Notification | OFFICER | Header Bell | `GET /api/v1/notifications/unread` | `Notification` | Persistent targeted notification | Active | YES |
| Officer Accept Report | OFFICER | `[Accept Report]` | `POST /api/v1/officer/reports/:id/accept` | `Pothole` | `ROLE_OFFICER` & `VERIFIED` & Spatial | Active | YES |
| Officer Reject Report | OFFICER | `[Reject Report]` | `POST /api/v1/officer/reports/:id/reject` | `Pothole`, `RejectionReason` | `ROLE_OFFICER` & `VERIFIED` & Spatial | Active | YES |
| Officer Start Work | OFFICER | `[Start Work]` | `PATCH /api/v1/officer/reports/:id/status` | `Pothole` | `ROLE_OFFICER` & `VERIFIED` & State Machine | Active | YES |
| Officer Mark Resolved | OFFICER | `[Mark Resolved]` | `PATCH /api/v1/officer/reports/:id/status` | `Pothole` | `ROLE_OFFICER` & `VERIFIED` & State Machine | Active | YES |
| Citizen Progress Notifications | USER | Header Bell | `GET /api/v1/notifications/unread` | `Notification` | Triggered on Accept/Reject/Start/Resolve | Active | YES |
| Interactive Live Map | ALL | `/map` | `GET /api/v1/potholes/map` | `Pothole` (PostGIS Envelope) | Viewport bounding box query | Active | YES |
| MinIO Storage Engine | SYSTEM | StorageService | S3 SDK (`pothole-raw`, `pothole-annotated`) | Object Keys | Presigned URL generation | Active | YES |
| Light Mode / Dark Mode Theme | ALL | Header Toggle | Local Storage Preference | Theme Context | Frontend theme tokens | Active | YES |
