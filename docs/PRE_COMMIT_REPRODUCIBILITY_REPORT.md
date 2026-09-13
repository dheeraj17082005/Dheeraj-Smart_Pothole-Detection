# Pre-Commit Reproducibility Report — PotholeX

This report documents the reproducibility verification comparing the current working tree baseline against a completely clean reproduction environment built in `/tmp/clean-potholex-repro`.

---

## 1. Environment Comparison Matrix

| Capability | Current Local | Clean Reproduction | Same? |
|---|---|---|---|
| Frontend Build | Vite 5.1.0 Node 20 (`dist/`) | Vite 5.1.0 Node 20 (`dist/`) | **YES** |
| Backend Build | JDK 21 Spring Boot 3.2.3 | JDK 21 Spring Boot 3.2.3 | **YES** |
| AI Service | Python 3.11 FastAPI + ONNX Runtime | Python 3.11 FastAPI + ONNX Runtime | **YES** |
| Database Engine | PostgreSQL 16 + PostGIS 3.4 | PostgreSQL 16 + PostGIS 3.4 | **YES** |
| MinIO Storage Engine | MinIO Dual-Bucket Architecture | MinIO Dual-Bucket Architecture | **YES** |
| Authentication | JWT Bearer Authentication | JWT Bearer Authentication | **YES** |
| USER Workflow | E2E Registration, Evidence Upload, Map, Notifications | E2E Registration, Evidence Upload, Map, Notifications | **YES** |
| OFFICER Workflow | Verification Gate, Jurisdiction Inbox, Status Transitions | Verification Gate, Jurisdiction Inbox, Status Transitions | **YES** |
| Authorization Rules | Backend REST Security Filter Chain | Backend REST Security Filter Chain | **YES** |
| Progress Notifications | Persistent Citizen Notifications | Persistent Citizen Notifications | **YES** |
| GIS Map Usability | Interactive Leaflet Bounding Box Viewport Query | Interactive Leaflet Bounding Box Viewport Query | **YES** |
| Dashcam Video Survey | Asynchronous Frame Sampling & Type A Aggregation | Asynchronous Frame Sampling & Type A Aggregation | **YES** |
| Spatial Duplicate Gate | PostGIS `ST_DWithin` (15m radius) | PostGIS `ST_DWithin` (15m radius) | **YES** |
| Resolved Protection Gate | PostGIS `ST_DWithin` (15m radius, 30d window) | PostGIS `ST_DWithin` (15m radius, 30d window) | **YES** |
| Lifecycle Transitions | Canonical State Machine Enforcement | Canonical State Machine Enforcement | **YES** |

---

## 2. Model & AI Inference Reproducibility

- **Model Filename**: `pothole_yolov8.onnx`
- **Model Source**: HuggingFace (`peterhdd/pothole-detection-yolov8`) via [`download_model.py`](file:///Users/dheerajkumar/Dheeraj-Smart_Pothole-Detection/ai-service/download_model.py)
- **Model Format**: ONNX Runtime (CPU inference)
- **Input Dimensions**: `640x640`
- **Classes**: `pothole` (1 class, class ID 0)
- **Confidence Threshold**: `0.15`
- **NMS IoU Threshold**: `0.45`
- **Clean Inference Validation**:
  - Image: `istockphoto-502561495-612x612.jpg`
  - Clean AI Count: **11 potholes**
  - Clean Detections Array Length: **11**
  - Max Confidence: **94.04%**
  - Visual Severity: **HIGH (100.0/100)**

---

## 3. Fresh Database Migration & MinIO Verification

- **Flyway Migrations**: Executed `V1__init_schema.sql` through `V8__report_acceptance_and_rejection_schema.sql` automatically on container start.
- **PostGIS Extensions**: `postgis` spatial extension enabled and functional.
- **MinIO S3 Buckets**: `pothole-raw` and `pothole-annotated` initialized cleanly; presigned URLs rendered properly.

---

## 4. Test Suite Summary

- **Frontend Vitest Suite**: **28 / 28 unit tests passed (100%)**.
- **Backend Maven Compilation**: **BUILD SUCCESS (0 compilation errors)**.
- **Clean Container Health**: 5/5 microservice containers healthy.
