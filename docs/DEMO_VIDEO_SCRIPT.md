# PotholeX — Product Demonstration Video Script

**Target Duration**: 60 Seconds  
**Format**: 1080p MP4 (H.264, 30fps)  
**Target Audience**: Technical Evaluators & System Reviewers  
**Application URL**: `http://localhost`  

---

## Technical Tagline & Positioning

> **"PotholeX empowers ordinary citizens to capture road surface evidence while giving municipal authorities strict spatial review and lifecycle control over official road defect remediation."**

---

## Scene Sequence & Storyboard

### **Scene 1: Landing & Authentication (0:00 – 0:05)**
- **Visual**: Application entry on `http://localhost/login`. Clean dark theme UI showing brand header (`PotholeX`), mode toggle (Light/Dark), and login options for Citizens and Officers.
- **Actions**:
  - Hover over Citizen Sign In.
  - Enter citizen credentials (`citizen_smoke@test.com`).
  - Click `Sign In`.
- **UI Elements Highlighted**: Topbar logo, System Status beacon (`System Operational`), role badge (`CITIZEN`).
- **Narrator Overlay Text**: *"PotholeX — Civic Road Maintenance & Telematics Portal"*

---

### **Scene 2: Citizen Dashboard Overview (0:05 – 0:15)**
- **Visual**: Citizen Home Dashboard (`/`).
- **Actions**:
  - View summary metric cards (Open Hazards, In Progress, Resolved).
  - Scroll past priority hazard alert card.
  - Hover over primary navigation header button: `Report Pothole`.
- **UI Elements Highlighted**: Title `MY REPORTS & ROAD OVERVIEW`, Citizen KPI summary cards, Live Map preview.
- **Narrator Overlay Text**: *"Citizens can track personal defect submissions, monitor remediation progress, and report new road hazards."*

---

### **Scene 3: Pothole Evidence Upload & AI Detection (0:15 – 0:25)**
- **Visual**: Defect Upload Page (`/upload`).
- **Actions**:
  - Select image evidence mode.
  - Attach road evidence image (`istockphoto-502561495-612x612.jpg`).
  - Set GPS coordinates (`28.6139° N, 77.2090° E`).
  - Click `Report Defect`.
  - View real-time AI inference results.
- **UI Elements Highlighted**: YOLOv8 ONNX bounding boxes, confidence score (`85.6%`), severity calculation (`HIGH`), assigned PostGIS civic authority (`NDMC Central`).
- **Narrator Overlay Text**: *"AI inference detects defect bounding boxes, computes visual severity scores, and maps spatial jurisdiction."*

---

### **Scene 4: Interactive GIS Defect Map (0:25 – 0:35)**
- **Visual**: Interactive Defect Map Page (`/map`).
- **Actions**:
  - Pan across municipal spatial boundary.
  - Zoom into NDMC jurisdiction markers.
  - Click pothole marker to view popup details (`HIGH` severity, `NDMC Central`).
  - Click `View on Map` / `Inspect Detail`.
- **UI Elements Highlighted**: Leaflet spatial markers, PostGIS bounding box queries, severity color encoding.
- **Narrator Overlay Text**: *"PostGIS spatial queries index defect markers dynamically across municipal road networks."*

---

### **Scene 5: Municipal Officer Review & Action Workflow (0:35 – 0:45)**
- **Visual**: Switch to Verified Officer Workspace (`/officer/dashboard`).
- **Actions**:
  - Sign in as verified municipal officer (`officer_smoke@test.com`).
  - View spatial jurisdiction inbox (`Central Division - 10km Radius`).
  - Open incoming report `#94d2aea9`.
  - Click `[✓ Accept]` → Status transitions from `SUBMITTED` to `ACCEPTED`.
  - Click `[▶ Start Work]` → Status transitions to `IN_PROGRESS`.
  - Click `[✓ Mark Resolved]` → Status transitions to `RESOLVED`.
- **UI Elements Highlighted**: Officer badge (`OFFICER`), jurisdiction profile, verification status (`VERIFIED`), canonical state machine controls.
- **Narrator Overlay Text**: *"Verified jurisdiction officers retain total authority to review, accept, dispatch crews, and resolve reports."*

---

### **Scene 6: Real-Time Citizen Notification & Verification (0:45 – 0:55)**
- **Visual**: Return to Citizen Session (`citizen_smoke@test.com`).
- **Actions**:
  - Click topbar Notification Bell (`🔔 3`).
  - View notifications dropdown:
    1. `Report Accepted` (by Officer)
    2. `Work Started` (`IN_PROGRESS`)
    3. `Pothole Resolved` (`RESOLVED`)
  - Navigate to defect detail page to confirm updated audit history timeline.
- **UI Elements Highlighted**: Notification bell badge count, notification items, audit history timeline.
- **Narrator Overlay Text**: *"Citizens receive persistent real-time notifications as official repair milestones are reached."*

---

### **Scene 7: Architecture Summary & Closing Tagline (0:55 – 1:00)**
- **Visual**: Clean full-screen architecture banner displaying full technology stack.
- **Graphic Elements**:
  - Logo: **PotholeX**
  - Workflow pipeline: `Citizen Evidence` → `YOLOv8 AI Inference` → `PostGIS Resolution` → `Officer Review` → `Remediation`
  - Stack banner: `React 18 | Spring Boot 3.2 | FastAPI | PostgreSQL / PostGIS 16 | MinIO S3 | Docker Compose`
- **Narrator Overlay Text**: *"PotholeX — Production-Ready AI & Spatial Telematics Road Maintenance Platform."*

---

## Narration Script (< 60 Seconds)

```text
"PotholeX allows citizens to report road surface defects with image or video evidence.

Upon submission, an embedded YOLOv8 AI model detects pothole bounding boxes, calculates visual severity scores, and assigns geographic jurisdiction via PostGIS spatial indexing.

Citizens can pan, zoom, and query defects across interactive municipal road maps.

Verified jurisdiction officers receive reports inside their spatial review queue, inspect evidence, accept or reject submissions, and manage repair dispatches through a strict state machine.

Citizens receive real-time notifications at every milestone through full repair resolution."
```
