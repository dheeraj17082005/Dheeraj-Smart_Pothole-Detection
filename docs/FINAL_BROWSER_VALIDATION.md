# Final Browser & End-to-End Validation Report

## 1. Environment & Architecture Overview

- **Operating System**: macOS (Darwin 24.6.0) / Docker Compose v2 Multi-Container Stack
- **AI Model Repository**: [`peterhdd/pothole-detection-yolov8`](https://huggingface.co/peterhdd/pothole-detection-yolov8)
- **Model Architecture**: YOLOv8s (11.1M parameters, FP32 ONNX runtime, CPUExecutionProvider)
- **Input Tensor**: `images` shape `[1, 3, 640, 640]`
- **Output Tensor**: `output0` shape `[1, 5, 8400]` (Single-class pothole detector, Class ID `0`)
- **Default Confidence Threshold**: `0.25`
- **Application Services**:
  - `pothole_frontend`: React 18 + Vite + TailwindCSS + Leaflet via Nginx (Port 80)
  - `pothole_backend`: Spring Boot 3.2.3 + Java 21 (Port 8080)
  - `pothole_ai_service`: FastAPI + ONNX Runtime (Port 8000)
  - `pothole_postgres`: PostgreSQL 16 + PostGIS 3.4 (Port 5432)
  - `pothole_minio`: MinIO S3-compatible Object Store (Ports 9000/9001)

---

## 2. Direct AI Service Verification

### Health Endpoint Check
- **Endpoint**: `GET http://localhost:8000/health`
- **Response**: `HTTP 200 OK`
- **Model Loaded**: `true`
- **Reported Model Name**: `peterhdd/pothole-detection-yolov8`
- **Model Version**: `YOLOv8s`
- **Pothole Class ID**: `0`

### Direct AI Inference (`test-data/images/pothole_sample.jpg`)
- **Potholes Detected**: 3
- **Max Confidence**: `0.6711` (67.11%)
- **Bounding Boxes**:
  - Box 1: `[xmin: 318, ymin: 134, xmax: 453, ymax: 171]`, Confidence: `0.6711`, Visual Area Ratio: `3.62%`
  - Box 2: `[xmin: 49, ymin: 224, xmax: 214, ymax: 294]`, Confidence: `0.5427`, Visual Area Ratio: `8.38%`
  - Box 3: `[xmin: 87, ymin: 168, xmax: 219, ymax: 206]`, Confidence: `0.3987`, Visual Area Ratio: `3.70%`

---

## 3. Browser & API End-to-End Test Results

### Test 1: Primary Pothole Image Upload (`pothole_sample.jpg`)
- **Location**: Latitude `28.6200`, Longitude `77.2200` (Barakhamba Road, New Delhi)
- **HTTP Status**: `200 OK`
- **Pothole Count**: 3
- **Severity Score / Class**: `84.58` / `HIGH`
- **Authority Resolved**: `DEMO Delhi Public Works Department (Arterial Roads Division)` (`DEMO_PWD_ARTERIAL`)
- **Dispatch Status**: `DISPATCHED` (Ticket: `PWD-DEMO-2026-000001`)
- **Annotated Image URL**: `http://localhost:9000/pothole-annotated/annotated/2026/09/dfaab1ac-e79d-43b1-a391-de36f46da2f1.jpg`
- **Visual Image Verification**: Accessible, `HTTP 200 OK` (58,011 bytes JPEG)
- **Persisted Pothole ID**: `a5dddf28-98ea-4fa4-a76c-b0f6abb9057a`

### Test 2: Previously Failing Real Photo (`istockphoto-502561495-612x612.jpg`)
*Note: This image produced 0 detections (0.00 confidence) under the baseline model.*
- **Location**: Latitude `28.6139`, Longitude `77.2090` (Janpath Road, New Delhi)
- **HTTP Status**: `200 OK`
- **Pothole Count**: 2
- **Max Confidence**: **`0.8559` (85.59%)**
- **Primary Box**: `[xmin: 0, ymin: 198, xmax: 386, ymax: 376]`, Visual Area Ratio: `27.59%`
- **Severity Score / Class**: `100.0` / `HIGH`
- **Authority Resolved**: `DEMO New Delhi Municipal Council (NDMC)` (`DEMO_NDMC_CENTRAL`)
- **Annotated Image URL**: `http://localhost:9000/pothole-annotated/annotated/2026/09/ddca5a2e-ba08-4e95-a208-b672b7f74f66.jpg`
- **Visual Image Verification**: Accessible, `HTTP 200 OK` (167,585 bytes JPEG)
- **Persisted Pothole ID**: `3d79d276-031c-4c7a-b86c-1bcf88b168fa`

### Test 3: Clean Road Negative Control (`clean_road.jpg`)
- **Location**: Latitude `28.6100`, Longitude `77.2100`
- **HTTP Status**: `200 OK`
- **Pothole Count**: 0
- **Pothole Created**: `false` (`pothole: null`)
- **UI Message**: `"No potholes detected in the uploaded image."`
- **Authority Dispatch**: Suppressed (No false positive record created)

### Test 4: Real Random Road Image (`pothole_rdd_298.jpg`)
- **Location**: Latitude `28.6300`, Longitude `77.2150` (Connaught Circus)
- **HTTP Status**: `200 OK`
- **Pothole Count**: 2
- **Max Confidence**: `0.8164` (81.64%)
- **Secondary Box Confidence**: `0.7112` (71.12%)
- **Severity Score / Class**: `71.46` / `HIGH`
- **Authority Resolved**: `DEMO New Delhi Municipal Council (NDMC)` (`DEMO_NDMC_CENTRAL`)
- **Annotated Image URL**: `http://localhost:9000/pothole-annotated/annotated/2026/09/9a231f78-54a4-4981-a82c-fcca81d49ec0.jpg`
- **Visual Image Verification**: Accessible, `HTTP 200 OK` (71,489 bytes JPEG)
- **Persisted Pothole ID**: `7fd06447-818c-40fc-b572-ac98f0baacc4`

### Test 5: Spatial Deduplication Verification
- **Second Submission**: Uploaded `pothole_sample.jpg` at exact same coordinates (`28.6200`, `77.2200`)
- **Result**:
  - `is_duplicate`: `true`
  - `duplicate_of_id`: `a5dddf28-98ea-4fa4-a76c-b0f6abb9057a` (matches primary record ID)
  - Authority dispatch ticket suppressed (preventing redundant work orders)

### Test 6: Status Lifecycle & Audit Trail
- **Pothole ID**: `a5dddf28-98ea-4fa4-a76c-b0f6abb9057a`
- **Transitions Verified**:
  1. Initial Creation $\to$ `REPORTED` (actor: `SYSTEM`, note: `"Initial detection from image upload"`)
  2. `REPORTED` $\to$ `ACKNOWLEDGED` (actor: `dispatcher_operator`)
  3. `ACKNOWLEDGED` $\to$ `IN_PROGRESS` (actor: `crew_lead`)
  4. `IN_PROGRESS` $\to$ `RESOLVED` (actor: `road_inspector`)
- **Audit History**: All 4 state transitions chronologically recorded and verified via `/api/v1/potholes/{id}/history`.

### Test 7: Video Stream Detection & Background Job Processing (`sample_dashcam.mp4`)
- **Submission**: `POST /api/v1/potholes/detect-video`
- **Initial Response**: `HTTP 202 Accepted`, Job ID `a62a984c-65b2-45d1-860a-33ced0075ed6`
- **Job Status Transition**: `PENDING` $\to$ `PROCESSING` (progress: 0.5) $\to$ `COMPLETED` (progress: 1.0)
- **Aggregation Metrics**:
  - `totalFramesSampled`: 4
  - `framesWithPotholes`: 4
  - `potholesCreated`: 4
  - `duplicatesDetected`: 3 (intra-video consecutive frame aggregation working as designed)

---

## 4. Automated Regression Test Suite Results

| Test Suite | Framework | Tests Run | Passed | Failed | Errors |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Backend** | JUnit 5 + MockMvc + DataJpaTest | 93 | 93 | 0 | 0 |
| **AI Service** | Pytest + Starlette TestClient | 17 | 17 | 0 | 0 |
| **Frontend** | Vitest + React Testing Library | 28 | 28 | 0 | 0 |
| **Frontend Build** | Vite + TypeScript compiler | — | Success | — | — |
| **TOTAL** | | **138** | **138** | **0** | **0** |

---

## 5. Known Limitations & Operating Guidance

1. **Domain Alignment**: The computer vision model is optimized for vehicle dashcam, forward-facing smartphone, and paved asphalt road viewpoints. Extreme close-ups, top-down macro shots, and unpaved dirt/gravel tracks fall outside the training distribution.
2. **2D Visual Severity**: Severity scoring is computed from 2D bounding-box area percentage and model confidence, serving as an operational proxy rather than a physical 3D volumetric laser measurement.
3. **Synthetic / Demo Video**: The sample video represents a brief demo sequence; real-world continuous deployment would benefit from streaming GPS synchronization.

---

## 6. Final Verdict

**`READY — NEW MODEL VERIFIED THROUGH BROWSER`**
