# Video & Dashcam Pipeline Validation Report

## 1. Executive Summary & Final Verdict

**FINAL VERDICT: READY — VIDEO PIPELINE VERIFIED**

The asynchronous video and dashcam ingestion pipeline has been verified end-to-end with the newly integrated computer vision model (`peterhdd/pothole-detection-yolov8`). The pipeline successfully ingests MP4 video streams, extracts and samples frames at the configured 2 FPS rate, performs object detection and bounding-box localization on all frames, executes Type A intra-video spatial-temporal aggregation, extracts optimal representative keyframe evidence to MinIO, resolves civic authority jurisdictions via PostGIS, and renders live status updates through the browser UI.

---

## 2. Video Input Characteristics

- **Test Fixture**: `test-data/videos/sample_dashcam.mp4`
- **Container / Codec**: MP4 / MPEG-4 Video (`mpeg4`)
- **Duration**: `2.0` seconds
- **Frame Count**: `60` total frames
- **Frame Rate**: `30.0` FPS
- **Resolution**: `1920x1920` pixels
- **File Size**: `2.53 MB` (2,532,980 bytes)
- **Visual Content**: Road-facing perspective with pavement surface damage and asphalt distress.

---

## 3. Direct AI Service Video Test (FastAPI)

- **Endpoint**: `POST http://localhost:8000/detect/video?sample_fps=2.0&conf_threshold=0.25`
- **Model**: `peterhdd/pothole-detection-yolov8` (YOLOv8s FP32 ONNX)
- **Sampling Configuration**: `2.0 FPS` (Sample step = every 15 frames)
- **Total Frames Analyzed**: `4` frames

### Frame-by-Frame Detection Results:
1. **Frame 0 (t = 0.00s)**:
   - Box 1: `[1749, 489, 1908, 664]`, Confidence: `0.7624` (76.24%), Area: `0.75%`
   - Box 2: `[2, 1676, 137, 1834]`, Confidence: `0.5361` (53.61%), Area: `0.57%`
   - Box 3: `[1352, 1629, 1439, 1669]`, Confidence: `0.4557` (45.57%), Area: `0.09%`
2. **Frame 15 (t = 0.50s)**:
   - Box 1: `[1748, 490, 1908, 666]`, Confidence: `0.8090` (80.90%), Area: `0.76%`
   - Box 2: `[139, 209, 277, 372]`, Confidence: `0.4383` (43.83%), Area: `0.61%`
   - Box 3: `[1350, 1630, 1438, 1667]`, Confidence: `0.3092` (30.92%), Area: `0.09%`
3. **Frame 30 (t = 1.00s)**:
   - Box 1: `[1748, 490, 1908, 666]`, Confidence: `0.8090` (80.90%), Area: `0.76%`
   - Box 2: `[139, 209, 277, 372]`, Confidence: `0.4367` (43.67%), Area: `0.61%`
   - Box 3: `[1350, 1630, 1438, 1667]`, Confidence: `0.3092` (30.92%), Area: `0.09%`
4. **Frame 45 (t = 1.50s)**:
   - Box 1: `[1748, 490, 1908, 666]`, Confidence: `0.8090` (80.90%), Area: `0.76%`
   - Box 2: `[139, 209, 277, 372]`, Confidence: `0.4366` (43.66%), Area: `0.61%`
   - Box 3: `[1350, 1630, 1438, 1667]`, Confidence: `0.3092` (30.92%), Area: `0.09%`
- **Representative Keyframe Selected**: Frame Index `15` at `t = 0.50s` (`MAX_VISUAL_AREA`).

---

## 4. Spring Boot Asynchronous Ingestion & Job Execution

- **Ingestion Endpoint**: `POST /api/v1/potholes/detect-video`
- **Response**: `HTTP 202 Accepted`
- **Job ID**: `333f29fa-6086-4335-b0f9-407b3e5a526d`
- **Job Lifecycle Progression**:
  - `PENDING` $\to$ Job initialized, raw MP4 uploaded to MinIO `pothole-raw`.
  - `PROCESSING` (Progress: 0.5) $\to$ AI FastAPI service sampled 4 frames, extracted detections.
  - `COMPLETED` (Progress: 1.0) $\to$ Intra-video aggregation finalized, database records committed.
- **Processing Time**: ~9.78 seconds total async pipeline turnaround.

---

## 5. Type A Intra-Video Aggregation & Database Persistence

The 4 sampled frames containing consecutive observations of the same physical road damage were aggregated according to spatial-temporal proximity:
- **Primary Pothole Record**: ID `19b47acb-82ee-43cc-9dfb-d005c3c91fe8`
  - Max Confidence: `0.8090`
  - Status: `REPORTED`
  - `is_duplicate`: `false`
  - `civic_authority_id`: `a0000000-0000-0000-0000-000000000001` (`DEMO_NDMC_CENTRAL`)
  - Representative Key: `annotated/2026/09/ce4a967e-727d-46fb-9c71-1c19f7a53222.jpg`
- **Aggregated Associated Frames**:
  - Pothole `ec5bff52-43b8-4620-93d4-ab7f41f5d7df`: `is_duplicate = true`, `duplicate_of_id = 19b47acb...`
  - Pothole `9ef6f6eb-1429-4ead-aa3e-e3ca965daecd`: `is_duplicate = true`, `duplicate_of_id = 19b47acb...`
  - Pothole `6acd2a3c-1dde-4cc2-aa1b-5843bfa135e1`: `is_duplicate = true`, `duplicate_of_id = 19b47acb...`

---

## 6. Representative Evidence & Object Storage Verification

- **MinIO Bucket**: `pothole-annotated`
- **Object Key**: `annotated/2026/09/ce4a967e-727d-46fb-9c71-1c19f7a53222.jpg`
- **Direct HTTP Check**: `GET http://localhost:9000/pothole-annotated/annotated/2026/09/ce4a967e-727d-46fb-9c71-1c19f7a53222.jpg`
- **HTTP Status**: `200 OK` (717,007 bytes JPEG with annotated red bounding boxes)
- **Browser Accessibility**: Direct link renders without authentication or signature errors due to public read bucket policy.

---

## 7. Frontend User Workflow & Failure Handling

1. **Frontend Integration**:
   - `UploadPage.tsx` handles video upload via `useDetectionJob` hook.
   - Non-blocking client polling (every 1000ms) with animated progress indicators.
   - Automatically displays aggregated pothole count, severity summary, and direct links to pothole detail view upon job completion.
2. **Invalid Input Handling**:
   - Submitting non-video text payload: `HTTP 400 Bad Request` (`errorCode: "INVALID_MEDIA"`, message: `"Unsupported video format. Only MP4 and WebM videos are supported."`).
   - Submitting 0-byte empty file: `HTTP 400 Bad Request` (`errorCode: "INVALID_MEDIA"`, message: `"Video file is required and cannot be empty."`).
   - No stack traces leaked; no dangling job records created.

---

## 8. Throughput & Performance Observations

- **Video Clip Duration**: 2.0 seconds
- **Frames Sampled**: 4 frames (@ 2.0 FPS)
- **AI Raw Inference Latency**: ~3.8 seconds (~950ms per sampled frame on CPU execution provider)
- **Full Async Job Turnaround**: ~9.78 seconds
- **Characterization**: **Asynchronous Video Processing (Batch/Polling)**. The system is designed for dashcam upload and survey processing rather than low-latency real-time video streaming.

---

## 9. Full Regression Test Status

| Suite | Framework | Total Tests | Passed | Failed |
| :--- | :--- | :---: | :---: | :---: |
| Backend | JUnit 5 + MockMvc + PostGIS | 93 | 93 | 0 |
| AI Service | Pytest | 17 | 17 | 0 |
| Frontend | Vitest + React Testing Library | 28 | 28 | 0 |
| Build | TypeScript / Vite Production Build | — | Success | — |
| **TOTAL** | | **138** | **138** | **0** |
