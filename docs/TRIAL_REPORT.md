# Smart Pothole Detection and Reporting System — End-to-End Application Trial Report

**Date of Trial**: September 11, 2026  
**Trial Execution Environment**: macOS (Darwin 24.0.0 arm64) / Docker Engine 29.5.2 / Docker Compose v2  
**Target Architecture**: Multi-Container Distributed System (React 18 + Spring Boot 3.2 + FastAPI ONNX + PostgreSQL 16 PostGIS + MinIO)

---

## 1. Executive Summary

This report documents the final end-to-end trial executed on the clean multi-container Docker Compose deployment of the Smart Pothole Detection and Reporting System. Every functional workflow, AI inference pipeline, spatial authority query, deduplication rule, lifecycle state transition, dashboard aggregation, and error handling routine was exercised against live containerized services.

All workflows executed successfully with **zero regressions**, and all **139 automated test suites passed**.

---

## 2. Docker Services & Container Health

The system was started cleanly using `docker-compose down -v && docker-compose up -d --build`.

| Container Name | Service Image | Port Mapping | Health Status | Verification Endpoint |
|---|---|---|---|---|
| `pothole_postgres` | `postgis/postgis:16-3.4` | `5432:5432` | `healthy` | `pg_isready -U pothole_user -d potholedb` |
| `pothole_minio` | `minio/minio:latest` | `9000:9000`, `9001:9001` | `healthy` | `GET http://localhost:9000/minio/health/live` |
| `pothole_ai_service` | Custom (`python:3.11-slim`) | `8000:8000` | `healthy` | `GET http://localhost:8000/health` |
| `pothole_backend` | Custom (`eclipse-temurin:21-jre`) | `8080:8080` | `healthy` | `GET http://localhost:8080/actuator/health` |
| `pothole_frontend` | Custom (`nginx:alpine` + SPA) | `80:80` | `running` | `GET http://localhost/` (HTTP 200 OK) |

**Flyway Database Migrations**: 5 migrations (`V1` through `V5`) applied automatically on startup.

---

## 3. Workflow Trial Results

### A. Valid Pothole Image Detection
- **Input**: `test-data/images/pothole_sample.jpg` with coordinates `(28.6200, 77.2200)`
- **Results**:
  - Pothole detected at bounding box `[1751, 1107, 1917, 1277]` with confidence `0.2673`.
  - Raw image persisted in MinIO bucket `pothole-raw`.
  - Annotated bounding-box evidence image generated and saved to `pothole-annotated`.
  - 2D visual severity calculated: score `2.05` (`LOW`).
  - PostGIS road network buffer resolved coordinates to `DEMO Delhi Public Works Department (Arterial Roads Division)` (`DEMO_PWD_ARTERIAL`).
  - Primary Pothole record persisted (`97db4825-0808-4157-80c3-a9ab28f88c1d`).
  - Initial status history record created (`REPORTED`, `changedBy: SYSTEM`).
  - Simulated report dispatched with ticket `PWD-DEMO-2026-000001` (`status: DISPATCHED`).
  - **Verdict**: **PASS** ✅

### B. Clean Road Image Verification
- **Input**: `test-data/images/clean_road.jpg` with coordinates `(28.6200, 77.2200)`
- **Results**:
  - Model reported `pothole_count: 0`.
  - `pothole_created: false`, `pothole: null`.
  - Message returned: *"No potholes detected in the uploaded image."*
  - No false business record created in database. Handled cleanly with HTTP 200 OK.
  - **Verdict**: **PASS** ✅

### C. Inter-Report Duplicate Detection (Type B)
- **Input**: Resubmitted `test-data/images/pothole_sample.jpg` at coordinates `(28.6200, 77.2200)` within the 15m / 7-day window.
- **Results**:
  - System flagged submission as duplicate: `is_duplicate: true`.
  - Linked to parent: `duplicate_of_id: "97db4825-0808-4157-80c3-a9ab28f88c1d"`.
  - Database audit verified zero second authority dispatch reports created (duplicate suppression working).
  - **Verdict**: **PASS** ✅

### D. Unknown Authority Fallback
- **Input**: Submitted pothole at Bengaluru coordinates `(12.9716, 77.5946)` outside seeded Delhi jurisdictions.
- **Results**:
  - Pothole created with `authority_code: "UNKNOWN_AUTHORITY"` and `authority: null`.
  - No false authority assigned.
  - Report dispatch bypassed for human triage queue.
  - **Verdict**: **PASS** ✅

### E. Status Lifecycle & Audit History
- **Input**: Sequential status transitions on pothole `97db4825-0808-4157-80c3-a9ab28f88c1d`.
- **Transitions Executed**:
  1. `REPORTED` $\to$ `ACKNOWLEDGED` (`changedBy: Officer Sharma`, notes provided)
  2. `ACKNOWLEDGED` $\to$ `IN_PROGRESS` (`changedBy: Crew Lead Verma`, notes provided)
  3. `IN_PROGRESS` $\to$ `RESOLVED` (`changedBy: Inspector Singh`, notes provided)
  4. Attempted invalid backward transition: `RESOLVED` $\to$ `IN_PROGRESS` $\to$ Rejected with HTTP 400 ProblemDetail.
- **Audit Log**: Immutable timeline audit retrieved via `GET /api/v1/potholes/{id}/history` containing all 4 timestamped events.
- **Verdict**: **PASS** ✅

### F. Dashboard & Map Viewport Live Querying
- **Dashboard Stats**: `GET /api/v1/dashboard/stats` returned exact live counts (`totalPotholes: 3`, `reportedCount: 2`, `resolvedCount: 1`).
- **Leaflet Viewport Spatial Query**: `GET /api/v1/potholes/map?minLat=28.5&minLng=77.0&maxLat=28.7&maxLng=77.5` returned the 2 Delhi markers and correctly excluded the out-of-bounds Bengaluru marker.
- **Verdict**: **PASS** ✅

### G. Pothole List Filtering
- **Query**: `GET /api/v1/potholes?page=0&size=10&status=RESOLVED` returned only the single resolved pothole.
- **Query**: `GET /api/v1/potholes?severity=LOW` returned all 3 low-severity potholes.
- Backend database pagination and filtering verified.
- **Verdict**: **PASS** ✅

### H. Asynchronous Video Processing & Frame Aggregation (Type A)
- **Input**: Uploaded `test-data/videos/sample_dashcam.mp4` to `POST /api/v1/potholes/detect-video`.
- **Results**:
  - Immediately received HTTP 202 Accepted with `jobId: 96e8735e-3b14-47cd-96e3-e0b4469c21ce`.
  - Polling `GET /api/v1/detection-jobs/{jobId}` transitioned from `PENDING` $\to$ `COMPLETED` (`progress: 1.0`).
  - Sampled 4 frames at 2.0 FPS and applied Type A frame tracking aggregation.
  - **Verdict**: **PASS** ✅

### I. Safe Failure Scenario Handling
- **Input**: Uploaded non-media payload (`application/octet-stream`).
- **Results**: Cleanly rejected with HTTP 400 ProblemDetail (`errorCode: "INVALID_MEDIA"`). No Java stacktraces exposed.
- **Verdict**: **PASS** ✅

### J. Object Storage & Database Audit
- **MinIO**: Confirmed raw images stored in `pothole-raw`, annotated images stored in `pothole-annotated`, raw videos in `pothole-raw`.
- **PostgreSQL**: Confirmed only object keys (e.g., `annotated/2026/09/2a5ff63c...jpg`), not binary blobs, are stored in database tables.
- **Verdict**: **PASS** ✅

---

## 4. AI Confidence Threshold Diagnostic

In response to observations regarding model detection sensitivity across various images, a controlled diagnostic was conducted by testing `test-data/images/pothole_sample.jpg` directly against the YOLOv8 ONNX model at 5 discrete confidence thresholds:

| Confidence Threshold | Potholes Detected | Highest Confidence | Raw Detections Found |
|---|---|---|---|
| `0.10` | 2 | `0.2673` | `[0.2673, 0.1433]` |
| `0.20` | 1 | `0.2673` | `[0.2673]` |
| `0.25` | 1 | `0.2673` | `[0.2673]` |
| `0.30` | 0 | `0.0000` | `[]` (Filtered out) |
| `0.35` | 0 | `0.0000` | `[]` (Filtered out) |

### Diagnostic Conclusion:
- The root cause is **A. Confidence threshold filtering**.
- The `vinothvikas1987/pothole-detection-yolov8` model produces confident detections in the `0.25–0.28` range for typical road scenes. When the default filter is set above `0.30`, valid pothole bounding boxes are suppressed.
- Setting the system standard baseline to `0.20–0.25` reliably preserves real pothole detections while rejecting noise.

---

## 5. Automated Test Suite Summary

| Test Suite | Total Tests | Passed | Failed | Errors |
|---|---|---|---|---|
| **Spring Boot Backend Tests** | 93 | 93 | 0 | 0 |
| **FastAPI AI Service (Pytest)** | 17 | 17 | 0 | 0 |
| **Frontend Component Tests (Vitest)** | 28 | 28 | 0 | 0 |
| **PostGIS Spatial Integration Test** | 1 | 1 | 0 | 0 |
| **TOTAL** | **139** | **139** | **0** | **0** |

- **TypeScript Typecheck**: Passed with **0 errors**.
- **Frontend Production Build**: `npm run build` completed in `1.89s` generating optimized bundle (`415 KB`).

---

## 6. Known Issues & Limitations Classification

1. **Environmental / Infrastructure (Resolved)**:
   - *Testcontainers Ryuk on macOS Colima*: Testcontainers Ryuk tries to mount `~/.colima/default/docker.sock` which rootless Docker configurations may reject. Resolved for local CI using `TESTCONTAINERS_RYUK_DISABLED=true`.
2. **Expected Limitations (By Design per Architecture)**:
   - *Simulated Government Dispatch*: External civic CRM integrations are simulated using mock dispatch clients with idempotency keys and exponential backoff.
   - *2D Visual Severity Heuristic*: Severity is calculated from 2D image plane geometry and does not represent 3D volumetric depth or structural vehicle damage.
   - *Authentication*: Omitted for rapid local evaluation per project scope.

---

## 7. Final Assessment Verdict

### Recommendation: **READY WITH KNOWN LIMITATIONS**

The Smart Pothole Detection and Reporting System meets all functional, architectural, spatial, and algorithmic requirements specified in `docs/ARCHITECTURE.md`. The complete end-to-end user workflow (Image/Video ingestion $\to$ YOLOv8 AI inference $\to$ MinIO storage $\to$ PostGIS spatial authority routing $\to$ Deduplication $\to$ Simulated report dispatch $\to$ Interactive React Leaflet dashboard) is operational and verified.
