# PotholeX — End-to-End Workflow GIF Storyboard

**Target Duration**: 10–12 Seconds (Infinite Loop)  
**Target Dimensions**: 1280px Width x 720px Height  
**Format**: Optimized GIF / WebP Animation  
**Output Path**: `demo-assets/potholex-workflow.gif`  

---

## 12-Frame Storyboard Specifications

| Frame # | Step Title | Visual Component / Card | Technical Detail |
|---|---|---|---|
| **Frame 1** | **PotholeX Platform** | Brand Banner & System Portal Overview | `React 18 + TypeScript UI` |
| **Frame 2** | **Citizen Evidence Submission** | Upload Form with GPS Coordinates (`28.6139° N, 77.2090° E`) | `ROLE_USER` Authorization |
| **Frame 3** | **YOLOv8 AI Defect Detection** | Annotated Pothole Image with Bounding Boxes & Confidence | `FastAPI + ONNX Runtime (85.6% Conf)` |
| **Frame 4** | **PostGIS Spatial Resolution** | Geographic Polygon Mapping to Municipal Authority | `ST_Contains / ST_DWithin Indexing` |
| **Frame 5** | **Deduplication Check** | Spatial 15m Proximity & 30-Day Resolution Gate | `PostGIS Spatial Protection` |
| **Frame 6** | **Officer Notification** | In-App Targeted Notification Alert | `Persistent Notification System` |
| **Frame 7** | **Officer Review Queue** | Verified Officer Jurisdiction Workspace | `ROLE_OFFICER` & `VERIFIED` Gate |
| **Frame 8** | **Officer Decision** | Accept (`SUBMITTED` → `ACCEPTED`) or Reject Workflow | `Official Jurisdiction Control` |
| **Frame 9** | **Work Dispatched** | Status Updated to `IN_PROGRESS` | `Canonical State Machine` |
| **Frame 10** | **Remediation Complete** | Status Updated to `RESOLVED` | `Lifecycle Audit History` |
| **Frame 11** | **Citizen Notified** | Notification Bell Alert (`Pothole Resolved`) | `Real-Time Progress Feedback` |
| **Frame 12** | **Full-Stack Architecture** | Architecture & Microservice Technology Stack | `React • Spring Boot • FastAPI • PostGIS • MinIO • Docker` |

---

## Technical Architecture Visual

```
                     ┌────────────────────────┐
                     │   Citizen Evidence     │
                     └───────────┬────────────┘
                                 │ (Image / Video)
                                 ▼
                     ┌────────────────────────┐
                     │  FastAPI AI Inference  │
                     │  (YOLOv8 ONNX Model)   │
                     └───────────┬────────────┘
                                 │
                                 ▼
                     ┌────────────────────────┐
                     │   Spring Boot Backend  │
                     │   PostgreSQL / PostGIS │
                     └───────────┬────────────┘
                                 │
                                 ▼
                     ┌────────────────────────┐
                     │ Verified Officer Review│
                     │  (Accept/Work/Resolve) │
                     └───────────┬────────────┘
                                 │
                                 ▼
                     ┌────────────────────────┐
                     │ Citizen Notification   │
                     │  (Real-Time Milestone) │
                     └────────────────────────┘
```

---

## Role Authority Boundaries

- **Citizen (`ROLE_USER`)**: Submits defect media evidence and coordinates; views personal reports, map markers, and notifications.
- **Officer (`ROLE_OFFICER`)**: Verified spatial jurisdiction authority responsible for report review, acceptance/rejection, dispatch, and marking status `RESOLVED`.
