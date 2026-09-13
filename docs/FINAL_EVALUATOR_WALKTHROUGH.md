# Final Evaluator Walkthrough & System Guide

## 1. Project Purpose
The **Smart Pothole Detection and Reporting System** is an end-to-end civic intelligence platform designed to automate the lifecycle of road hazard remediation. By ingesting citizen-submitted dashcam images and video streams, the system automatically detects road distress, quantifies visual severity, resolves geographic jurisdictions via PostGIS spatial analysis, suppresses redundant reports through multi-tiered deduplication, and dispatches mock work orders to municipal authorities.

---

## 2. System Architecture

The architecture consists of five containerized services orchestrated via Docker Compose:

```
[ Browser / Client ]
        │
        ▼ (Port 80)
┌──────────────────────────────────────────────┐
│  React 18 + Vite + Tailwind CSS (Nginx)      │
└──────────────────────┬───────────────────────┘
                       │ Reverse Proxy / API Routing
                       ▼ (Port 8080)
┌──────────────────────────────────────────────┐
│  Spring Boot 3.2.3 Backend (Java 21)         │
│  - Spatial Deduplication Engine              │
│  - Authority Resolver Service                │
│  - Asynchronous Video Job Coordinator        │
│  - Work Order Dispatch & Audit Service       │
└───────┬──────────────┬───────────────┬───────┘
        │              │               │
        ▼ (Port 8000)  ▼ (Port 5432)   ▼ (Ports 9000/9001)
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ FastAPI AI   │ │ PostgreSQL16 │ │ MinIO Object │
│ - ONNX Run-  │ │ + PostGIS3.4 │ │ Storage      │
│   time CPU   │ │ - Spatial    │ │ - Raw Media  │
│ - YOLOv8s    │ │   Polygons   │ │ - Annotated  │
│ - NMS / Box  │ │ - Potholes   │ │   Images     │
└──────────────┘ └──────────────┘ └──────────────┘
```

---

## 3. Technology Stack

- **Frontend**: React 18, TypeScript, Tailwind CSS, Leaflet / React-Leaflet, Lucide Icons, Vite, Nginx.
- **Backend**: Spring Boot 3.2.3, Java 21, Spring Data JPA, Hibernate Spatial, Flyway, Jackson.
- **AI Inference Service**: Python 3.11, FastAPI, ONNX Runtime (CPU Execution Provider), OpenCV-headless, NumPy.
- **Spatial Database**: PostgreSQL 16 + PostGIS 3.4 (`postgis/postgis:16-3.4`).
- **Object Storage**: MinIO (S3-compatible API, public-read bucket policy for annotated evidence).
- **Containerization**: Docker Compose v2.

---

## 4. Machine Learning Model & Hugging Face Source

- **Model Identifier**: [`peterhdd/pothole-detection-yolov8`](https://huggingface.co/peterhdd/pothole-detection-yolov8)
- **Model Architecture**: YOLOv8s (Small Object Detector, 11.1M parameters)
- **Deployment Format**: FP32 ONNX Runtime (`best.onnx`, ~44.7 MB)
- **Input Tensor**: `images` shape `[1, 3, 640, 640]` RGB normalized to $[0.0, 1.0]$
- **Output Tensor**: `output0` shape `[1, 5, 8400]` (Bounding box coordinates + single-class pothole confidence score)
- **Production Confidence Threshold**: `0.25`
- **Domain Suitability**: Forward-facing vehicle dashcam, smartphone road photographs, and paved asphalt road surveys.

---

## 5. Image Ingestion Workflow

1. **Upload**: User submits image through React UI (`POST /api/v1/potholes/detect-image`).
2. **Storage**: Raw image saved to MinIO `pothole-raw` bucket.
3. **Inference**: Backend forwards image to FastAPI `/detect/image`.
4. **Localization & NMS**: ONNX runtime identifies bounding boxes and applies Non-Maximum Suppression.
5. **Annotation**: OpenCV draws red bounding boxes and confidence/area labels, saving the result to MinIO `pothole-annotated`.
6. **Persistence & Dispatch**: Backend calculates severity score, resolves civic authority, checks deduplication, and persists the record.

---

## 6. Video & Dashcam Ingestion Workflow

1. **Submission**: User uploads MP4/WebM video (`POST /api/v1/potholes/detect-video`).
2. **Asynchronous Handshake**: Backend immediately returns `HTTP 202 Accepted` with a tracking `jobId`.
3. **Frame Sampling**: FastAPI extracts frames at `2.0 FPS` (sampling every 15 frames from 30 FPS video).
4. **Frame Analysis**: Each sampled frame is evaluated with the YOLOv8s ONNX model.
5. **Keyframe Selection**: The frame with maximum visual pothole area is extracted and annotated as representative evidence.
6. **Intra-Video Aggregation (Type A)**: Consecutive frame observations of the same pothole are linked to a single primary pothole record.

---

## 7. Visual Severity Calculation Heuristic

Severity is computed deterministically using a 2D bounding-box area percentage and confidence score:

$$\text{Severity Score} = \min(100.0, (\text{Area Ratio} \times 700.0) + (\text{Confidence} \times 30.0))$$

- **`LOW`**: Score $< 35.0$
- **`MEDIUM`**: $35.0 \le \text{Score} < 70.0$
- **`HIGH`**: Score $\ge 70.0$

---

## 8. Civic Authority Resolution (PostGIS)

Authorities are resolved using hierarchical spatial queries:
1. **Dedicated Road Network Buffer**: Checks if the pothole point falls within a 25-meter buffer of an arterial road or national highway polygon (e.g. `DEMO_PWD_ARTERIAL`, `DEMO_NHAI_ZONE_1`).
2. **Municipal Boundary**: Checks if point falls within municipal corporation boundary (e.g. `DEMO_NDMC_CENTRAL`, `DEMO_MCD_SOUTH`).
3. **Unknown Fallback**: If coordinates lie outside all registered jurisdictions, assigns `UNKNOWN_AUTHORITY` / `null` and routes to triage.

---

## 9. Deduplication Strategy

- **Type A (Intra-Video Aggregation)**: Groups consecutive video keyframes into one primary record and marks remaining frames as duplicates (`is_duplicate = true`).
- **Type B (Inter-Report Deduplication)**: Matches newly reported coordinates against existing non-resolved potholes within a **15-meter radius** and **7-day time window** using PostGIS `ST_DWithin`. Redundant reports are marked `is_duplicate = true` with `duplicate_of_id` populated, suppressing duplicate work order dispatch.

---

## 10. Automated Reporting & Audit Lifecycle

- **Simulated Work Order Dispatch**: Automated dispatch with exponential backoff retries and idempotent dispatch keys (`report-{potholeId}-{authorityId}`).
- **Status Lifecycle**: `REPORTED` $\to$ `ACKNOWLEDGED` $\to$ `IN_PROGRESS` $\to$ `RESOLVED`.
- **Audit Timeline**: Every status update is immutably logged with timestamp, actor (`changed_by`), and notes.

---

## 11. Operational Dashboard & Interactive Map

- **Real-Time KPIs**: Total potholes, reported count, in-progress count, resolved count, and high-severity metrics.
- **Interactive Leaflet Map**: Dynamic viewport querying (`/api/v1/potholes/map?minLat=...&maxLat=...&minLng=...&maxLng=...`) rendering severity-colored markers.
- **Pothole Detail View**: High-resolution annotated evidence viewing, spatial metadata, and interactive status update controls.

---

## 12. Object Storage Configuration (MinIO)

- **Buckets**: `pothole-raw` (original uploads) and `pothole-annotated` (detection evidence).
- **Public Read Access**: Annotated evidence is served directly to browser clients via standard HTTP URLs without expiring presigned signature mismatches.

---

## 13. Automated Test Suites

| Suite | Framework | Total Tests | Status |
| :--- | :--- | :---: | :---: |
| **Backend** | JUnit 5 + Spring Boot + PostGIS | 93 | **93/93 Passed** |
| **AI Service** | Pytest + Starlette TestClient | 17 | **17/17 Passed** |
| **Frontend** | Vitest + React Testing Library | 28 | **28/28 Passed** |
| **Frontend Build** | Vite + TypeScript Build | — | **Success** |
| **TOTAL** | | **138** | **138/138 Passed** |

---

## 14. Known Limitations & Operating Guidance

1. **Domain Alignment**: Model is optimized for forward-facing vehicle/dashcam and paved road perspectives. Extreme top-down macro close-ups or unpaved dirt/gravel surfaces may yield low confidence or zero proposals.
2. **2D Visual Proxy**: Severity scoring represents a 2D optical bounding-box area proxy rather than physical 3D laser cavity depth.
3. **Asynchronous Video**: Video processing operates via non-blocking batch queueing and polling rather than sub-second real-time streaming.

---

## 15. Exact Evaluator Demo Sequence

### Step 1: Open Application
Navigate to **[http://localhost](http://localhost)**.

### Step 2: Upload Primary Sample Image
- Go to `/upload`.
- File: `test-data/images/pothole_sample.jpg`
- Coordinates: `Latitude: 28.6200`, `Longitude: 77.2200`
- Click **Analyze & Report Pothole**.
- Verify: 3 potholes detected, `HIGH` severity (84.58), `DEMO_PWD_ARTERIAL` authority, and red bounding boxes visible.

### Step 3: Test Clean Road Negative Control
- File: `test-data/images/clean_road.jpg`
- Verify: 0 potholes detected, no record created.

### Step 4: Test Duplicate Detection
- Re-upload `pothole_sample.jpg` at `28.6200, 77.2200`.
- Verify: `is_duplicate = true`, linked to first pothole ID, work order dispatch suppressed.

### Step 5: Test Status Lifecycle
- Open the primary pothole detail view.
- Update status: `REPORTED` $\to$ `ACKNOWLEDGED` $\to$ `IN_PROGRESS` $\to$ `RESOLVED`.
- Verify audit history table records all transitions.

### Step 6: Test Video Upload
- Upload `test-data/videos/sample_dashcam.mp4`.
- Verify: `202 Accepted` $\to$ animated progress bar $\to$ `COMPLETED` state with aggregated keyframe evidence.

---

## 16. Exact Docker Startup Commands

To start from a clean slate:

```bash
docker-compose down -v
docker-compose up -d --build
```

Access the UI at:
```
http://localhost
```
