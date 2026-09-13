# Smart Pothole Detection and Reporting System — Demonstration Walkthrough

This document outlines the step-by-step 2–5 minute demonstration sequence for verifying the complete full-stack civic pothole detection and reporting workflow.

---

## Prerequisites

1. Docker and Docker Compose installed and running.
2. System ports `80`, `8080`, `8000`, `9000`, `9001`, `5432` free and available.
3. Test media files located in `test-data/images/` and `test-data/videos/`.

---

## 14-Step Demo Sequence (2–5 Minutes)

### Step 1: Start the Multi-Container Stack
Start all services cleanly using Docker Compose:
```bash
docker compose up -d
```
Verify that all 5 containers reach `healthy` status:
```bash
docker compose ps
```
- `pothole_postgres` (PostGIS spatial database)
- `pothole_minio` (Object storage for raw & annotated imagery)
- `pothole_ai_service` (FastAPI with YOLOv8 ONNX model loaded)
- `pothole_backend` (Spring Boot REST API system of record)
- `pothole_frontend` (React + Leaflet web dashboard on port 80)

---

### Step 2: Open Frontend Application
Navigate in your web browser to:
[http://localhost](http://localhost) (or [http://localhost:5173](http://localhost:5173) in local Vite dev mode).

The Operational Dashboard loads, displaying:
- Top statistical summary cards (Total Potholes, Reported, Acknowledged, In Progress, Resolved, High Severity).
- Interactive Leaflet civic map showing existing potholes with severity-colored markers.
- Recent Potholes table with pagination and status badges.

---

### Step 3: Navigate to Upload & Submit Image
1. Click **"Upload / Report"** in the navigation header (`/upload`).
2. Select **"Image Upload"** mode.
3. Choose sample image: `test-data/images/pothole_sample.jpg`.
4. Enter test coordinates located within New Delhi NDMC jurisdiction:
   - **Latitude**: `28.6200`
   - **Longitude**: `77.2200`
   - **Address**: `Connaught Place Outer Circle, New Delhi`
5. Click **"Analyze & Submit Pothole"**.

---

### Step 4: Inspect AI Detection Results
Upon submission, inspect the immediate AI inference output:
- **Detection Confirmation**: "1 Pothole detected".
- **Visual Evidence**: Side-by-side or tabbed view showing raw submission and bounding-box annotated evidence image rendered from MinIO (`http://localhost:9000/pothole-annotated/...`).
- **Model Confidence**: High verified confidence score: **`79.0%` (`0.7897`)** with bounding box `[84, 165, 215, 215]`.
- **Negative Control Verification**: Submitting `test-data/images/clean_road.jpg` returns "No potholes detected in the uploaded image" with zero database persistence.

---

### Step 5: Verify 2D Visual Severity Score
Review the calculated severity rating:
- **Visual Severity Score**: Continuous 0–100 score based on 2D bounding-box area percentage weighted by detection confidence ($S = \min(100.0, R \times 1000 \times C)$).
- **Classification Badge**: `HIGH` ($\ge 50.0$ or count $\ge 3$), `MEDIUM` ($20.0 - 50.0$), or `LOW` ($< 20.0$).
- *(Note: Explanatory disclaimer confirming severity is a 2D camera-frame heuristic, not 3D structural depth).*

---

### Step 6: Verify Geolocation Tagging
- Confirm the GPS coordinates (`28.6200, 77.2200`) and human-readable reverse-geocoded address text are properly associated and displayed.

---

### Step 7: Verify PostGIS Civic Authority Resolution
Inspect the resolved civic authority block:
- **Authority Name**: `DEMO New Delhi Municipal Council (NDMC)`
- **Department**: `MUNICIPAL`
- **Authority Code**: `DEMO_NDMC_CENTRAL`
- Spatial polygon intersection matched the coordinates inside NDMC boundary.

---

### Step 8: Inspect Automated Simulated Report & Ticket
Review the automated dispatch record:
- **Report Status**: `DISPATCHED`
- **Official Ticket ID**: e.g., `MUNICIPAL-DEMO-2026-00000X`
- **Dispatch Channel**: Simulated Webhook / API Dispatch with idempotency key.

---

### Step 9: View Pothole on Map and Details View
1. Click **"View in Dashboard / Map"** or navigate to `/potholes`.
2. Locate the newly created pothole marker on the Leaflet map (color-coded by severity).
3. Click the marker popup or table row to open the **Pothole Detail Page** (`/potholes/{id}`).
4. Verify interactive detail view:
   - Coordinate mini-map with accuracy radius.
   - High-resolution raw and annotated evidence inspection.
   - Assigned authority contact cards.

---

### Step 10: Demonstrate Status Lifecycle & Audit History
1. On the Pothole Detail Page, click **"Update Status"**.
2. Transition status from `REPORTED` -> `ACKNOWLEDGED`.
3. Provide optional officer notes and submit.
4. Transition status from `ACKNOWLEDGED` -> `IN_PROGRESS`.
5. Transition status from `IN_PROGRESS` -> `RESOLVED`.
6. Review the **Status History Timeline** at the bottom of the page showing the immutable audit trail with timestamps, transitions, and user attribution.

---

### Step 11: Upload Dashcam Video Feed
1. Navigate back to **"Upload / Report"** (`/upload`).
2. Select **"Video Stream Upload"** mode.
3. Choose sample video: `test-data/videos/sample_dashcam.mp4`.
4. Enter starting coordinates:
   - **Latitude**: `28.6280`
   - **Longitude**: `77.2150`
   - **Address**: `Barakhamba Road, New Delhi`
5. Click **"Upload & Process Video"**.

---

### Step 12: Observe Asynchronous Job Processing
- The backend immediately returns HTTP `202 Accepted` with a `jobId` and `pollUrl`.
- The frontend initiates smooth polling on `GET /api/v1/detection-jobs/{jobId}`.
- UI displays active progress indicator moving from `PENDING` -> `PROCESSING` -> `COMPLETED`.

---

### Step 13: Verify Type A Intra-Video Frame Aggregation
Once the job reaches `COMPLETED`:
- Inspect the aggregation summary:
  - Total frames sampled at 2.0 FPS.
  - Video tracks detected.
  - Unique physical potholes identified (multi-frame detections aggregated into a single physical pothole record).
  - Best representative frame selected with highest visual confidence for evidence.

---

### Step 14: Inspect Final Pothole Result & Inter-Report Deduplication (Type B)
- Inspect the newly created pothole entry in the table and map.
- If an identical pothole was submitted within 15 meters and 7 days, verify it is marked with `isDuplicate = true` linked to the primary parent pothole, preventing duplicate authority dispatch tickets.

---

## Clean-Up After Demo
To stop and clean the environment:
```bash
docker compose down
```
To wipe all database records and storage volumes:
```bash
docker compose down -v
```
