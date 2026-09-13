# Smart Pothole Detection and Reporting System
## System Architecture & Technical Implementation Plan

---

### Executive Overview
The **Smart Pothole Detection and Reporting System** is an end-to-end civic tech platform designed to ingest dashcam images and videos, run computer-vision detection to identify road potholes, estimate visual severity, resolve responsible civic authorities using spatial road networks and jurisdictions, deduplicate incoming submissions, dispatch automated simulated reports, and track remediation status across an interactive dashboard.

---

### 1. Technology Stack & Standardized Version Strategy

To eliminate version ambiguity, the system enforces a strict, reproducible technology stack across all development and containerized environments:

| Layer | Component | Exact Version | Rationale / Note |
|---|---|---|---|
| **Frontend** | React | `18.2.0` | Standard stable React 18 release |
| | TypeScript | `5.3.3` | Strict type safety across UI components and API DTOs |
| | Vite | `5.1.0` | High-performance build tool & dev server |
| | Leaflet | `1.9.4` | Open-source interactive map engine |
| **Backend Core** | Java | `21` (LTS) | Modern Java language features (Virtual Threads, Records, Pattern Matching) |
| | Spring Boot | `3.2.3` | System of Record, REST APIs, JPA, Flyway, Bounded Task Executor |
| | Flyway | `10.7.0` | Database schema migration management |
| **AI Inference** | Python | `3.11` | Optimized Python runtime for OpenCV and ONNX |
| | FastAPI | `0.110.0` | Lightweight high-performance async REST framework |
| | ONNX Runtime | `1.17.1` | Hardware-agnostic CPU inference engine for YOLOv8 |
| | OpenCV Python | `4.9.0.80` | Image processing, bounding box rendering, video frame sampling |
| **Data & Storage** | PostgreSQL | `16.2` | Relational system of record |
| | PostGIS | `3.4.1` | Geospatial spatial queries, spatial indexing, predicate evaluation |
| | MinIO | `RELEASE.2024-01-31` | S3-compatible local object storage for media binaries |

> **Constraint Enforcement**: No additional infrastructure layers (Kafka, RabbitMQ, Redis, Kubernetes, GraphQL, OAuth2/Keycloak) are introduced. Concurrency is handled via Spring Boot's internal `ThreadPoolTaskExecutor` and database spatial queries handle deduplication.

---

### 2. Domain Model & System Architecture

#### System Architecture Diagram

```
                                  +-----------------------+
                                  |   React + Vite SPA    |
                                  | (TypeScript/Leaflet)  |
                                  +-----------+-----------+
                                              |
                                              | HTTP REST API (Port 8080)
                                              v
                                  +-----------------------+
                                  |   Spring Boot 3.2.3   |
                                  | (System of Record)    |
                                  |  - Job Executor       |
                                  |  - Spatial Engine     |
                                  |  - Dispatch Engine    |
                                  +----+-----+-------+----+
                                       |     |       |
                 +---------------------+     |       +-----------------------+
                 | HTTP REST (Port 8000)     | REST/SQL (Port 5432)          | S3 Protocol (Port 9000)
                 v                           v                               v
+-------------------------------+ +-----------------------+    +-----------------------+
|      FastAPI AI Service       | | PostgreSQL + PostGIS  |    |     MinIO Server      |
| Stateless Computer Vision     | | Metadata, Keys &    |    | Raw & Annotated Media |
| (ONNX YOLOv8 Inference)       | | Geometries           |    | Binary Storage        |
+-------------------------------+ +-----------------------+    +-----------------------+
```

#### Domain Model Entities & Concept Separation

The domain model strictly separates raw media, processing execution, raw CV detections, physical potholes, authorities, and dispatches:

```
+-------------------+           +-----------------------+           +-----------------------+
|    MediaAsset     | 1       1 |     DetectionJob      | 1       * |       Detection       |
+-------------------+-----------+-----------------------+-----------+-----------------------+
| id (UUID)         |           | id (UUID)             |           | id (UUID)             |
| mediaType (Enum)  |           | mediaAssetId (UUID)   |           | detectionJobId (UUID) |
| rawObjectKey      |           | status (Enum)         |           | frameIndex (Int)      |
| mimeType          |           | startedAt (Instant)   |           | frameTimestampSec     |
| fileSize          |           | completedAt (Instant) |           | boxXmin, Ymin, etc.   |
| uploadedAt        |           | errorSummary          |           | confidence (Float)    |
+-------------------+           +-----------------------+           | visualAreaRatio       |
                                                                    | potholeId (UUID, FK)  |
                                                                    +-----------+-----------+
                                                                                | *
                                                                                |
+-------------------+           +-----------------------+                       | 1
|     Authority     | 1       * |        Pothole        |<----------------------+
+-------------------+-----------+-----------------------+
| id (UUID)         |           | id (UUID)             | 1       * +-----------------------+
| name              |           | location (Point)      |-----------| PotholeStatusHistory  |
| code              |           | addressText           |           +-----------------------+
| contactEmail      |           | firstDetectedAt       |           | id (UUID)             |
| contactPhone      |           | severityScore (Float) |           | potholeId (UUID)      |
| departmentType    |           | severityClass (Enum)  |           | previousStatus (Enum) |
+---------+---------+           | maxConfidence (Float) |           | newStatus (Enum)      |
          | 1                   | status (Enum)         |           | changedAt (Instant)   |
          |                     | isDuplicate (Boolean) |           | changedBy             |
          | *                   | duplicateOfId (UUID)  |           | notes                 |
+---------v---------+           | representativeKey     |           +-----------------------+
|AuthorityJurisdict.|           +-----------+-----------+
+-------------------+                       | 1
| id (UUID)         |                       |
| authorityId (UUID)|                       | *
| jurisdictionType  |           +-----------v-----------+           +-----------------------+
| geometry (Geom)   |           |        Report         | 1       * |     ReportAttempt     |
+-------------------+           +-----------------------+-----------+-----------------------+
                                | id (UUID)             |           | id (UUID)             |
                                | potholeId (UUID)      |           | reportId (UUID)       |
                                | authorityId (UUID)    |           | channel (Enum)        |
                                | status (Enum)         |           | idempotencyKey        |
                                | createdTimestamp      |           | attemptTimestamp      |
                                +-----------------------+           | status (Enum)         |
                                                                    | responseSummary       |
                                                                    +-----------------------+
```

##### Entity Definitions
1. **`MediaAsset`**: Stores binary object reference for raw uploaded media (image/video) stored in MinIO bucket `pothole-raw`. Contains MIME type, file size, object key, and upload metadata.
2. **`DetectionJob`**: Tracks execution lifecycle of AI analysis (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`). Enables asynchronous video processing without blocking API endpoints.
3. **`Detection`**: Individual raw computer vision bounding box output produced by the ONNX model for a specific image or video frame. Stores bounding box coordinates `[xmin, ymin, xmax, ymax]`, confidence score, frame timestamp, visual area ratio, and link to parent Pothole.
4. **`Pothole`**: The **business-level physical event** representing a real-world pothole. Multiple adjacent video frame detections are clustered into a single `Pothole` record. Holds aggregated visual severity, max confidence, representative keyframe object key in MinIO, spatial location, and current status.
5. **`PotholeStatusHistory`**: Immutable audit log of status transitions (`REPORTED`, `ACKNOWLEDGED`, `IN_PROGRESS`, `RESOLVED`) detailing who made the change, when, and why.
6. **`Authority`**: Organization metadata for civic bodies (e.g. New Delhi Municipal Council, Delhi PWD, National Highways Authority of India).
7. **`AuthorityJurisdiction`**: Spatial layer linked to `Authority` containing boundary geometries (`GEOMETRY(Polygon/MultiPolygon, 4326)` for municipal zones and `GEOMETRY(MultiLineString, 4326)` for designated road networks).
8. **`Report`**: Business record of a civic notification ticket generated for an un-duplicated `Pothole`.
9. **`ReportAttempt`**: Individual dispatch attempt log detailing communication channel (`MOCK_EMAIL`, `MOCK_SMS`, `MOCK_WEBHOOK`), idempotency key, timestamp, delivery status, and error details.

---

### 3. AI Service Architecture & Video Processing Strategy

#### System-of-Record vs Stateless AI Service
- **Spring Boot**: Remains the sole **System of Record**. Manages DB persistence, job queueing, MinIO object lifecycle, PostGIS spatial queries, authority resolution, deduplication, status state machine, and report dispatches.
- **FastAPI AI Service**: Completely **stateless**. Connects to NO database and retains NO media state. Accepts raw binary media streams, performs ONNX YOLOv8 model inference and OpenCV frame drawing, and returns structured JSON responses.

#### Structured Inter-Service Communication (NO Base64 Primary Payloads)
- Inter-service REST APIs pass structured JSON data (bounding box arrays, confidence scores, visual area ratios, frame indices).
- Base64 encoding is **NOT** used for primary binary transfer between services.
- When an annotated frame is generated by OpenCV:
  1. FastAPI returns structured JSON metadata + optional annotated binary image stream.
  2. Spring Boot receives the binary stream and writes the file directly to MinIO bucket `pothole-annotated`.
  3. Spring Boot records the resulting MinIO object key in PostgreSQL.

```
+---------------+                +---------------+                +---------------+
| Spring Boot   |                | FastAPI AI    |                | MinIO Storage |
+-------+-------+                +-------+-------+                +-------+-------+
        |                                |                                |
        | 1. POST /detect/image (Stream) |                                |
        +------------------------------->|                                |
        |                                | 2. ONNX Inference              |
        |                                |    & OpenCV Annotation         |
        |                                |--------------------------------+
        | 3. Structured JSON +           |                                |
        |    Annotated Image Binary      |                                |
        |<-------------------------------+                                |
        |                                                                 |
        | 4. Put Annotated Binary (Object Key: annotated/img_123.jpg)     |
        +---------------------------------------------------------------->|
        |                                                                 |
        | 5. Save Object Key in PostgreSQL DB                             |
        |-----------------------------------------------------------------+
```

#### Video Frame Detection vs Aggregated Pothole Event
A 10-second video recorded at 30 FPS yields 300 frames. Sampling at 2 FPS produces 20 frames. If a pothole is visible across 6 consecutive sampled frames, the AI service outputs 6 raw `Detection` objects.

**Aggregation Pipeline**:
1. **Frame Sampling**: OpenCV samples video at 2 FPS.
2. **Temporal & Spatial Clustering**: Detections occurring within $\le 3$ seconds of each other in the same video stream are grouped into a single **`Pothole`** event.
3. **Representative Frame Selection**: The frame with the maximum visual area ratio and confidence score is designated as the **Representative Keyframe**.
4. **Aggregated Metrics**:
   - `maxConfidence` = $\max(C_1, C_2, \dots, C_k)$
   - `severityScore` = Weighted peak visual area ratio across cluster.
5. **Full Context Retention**: All 6 underlying `Detection` frame records (with frame index, timestamp offset `00:02.4`, bounding boxes) are saved as child records linked to the single aggregated `Pothole` entity.

#### Asynchronous `DetectionJob` Processing
To prevent HTTP timeouts when processing large video files:
1. Client calls `POST /api/v1/potholes/detect-video` (multipart video file + location).
2. Spring Boot stores raw video in MinIO (`pothole-raw/vid_<uuid>.mp4`), creates `DetectionJob` with status `PENDING`, and immediately returns HTTP `202 Accepted` with `jobId`.
3. Spring Boot submits job to internal `ThreadPoolTaskExecutor`:
   - Executor sends video to FastAPI `/detect/video`.
   - On completion, Spring Boot creates `MediaAsset`, aggregated `Pothole`, frame `Detection` records, performs PostGIS authority lookup and deduplication, and sets job status to `COMPLETED`.
4. Frontend polls `GET /api/v1/detection-jobs/{jobId}` until completed.

---

### 4. Media & Storage Strategy

- **MinIO Object Storage**: All binary files (raw dashcam photos, raw MP4 videos, annotated JPEG keyframes) reside exclusively in MinIO S3 buckets.
  - Bucket `pothole-raw`: Original uploaded media files (`raw/{year}/{month}/{uuid}.jpg`).
  - Bucket `pothole-annotated`: OpenCV processed evidence images with bounding box overlays (`annotated/{year}/{month}/{uuid}.jpg`).
- **PostgreSQL Database**: Contains **zero** byte arrays or BLOB columns. Stores only string-based MinIO object keys (`raw_object_key`, `annotated_object_key`), spatial points, metadata, and JSON bounding box coordinates.
- **URL Resolution**: Spring Boot converts object keys to pre-signed or proxied HTTP URLs (`http://localhost:9000/pothole-annotated/...`) when returning DTOs to the React frontend.

---

### 5. Geospatial & Civic Authority Resolution Strategy

#### Road Ownership vs Administrative Municipal Boundaries
Municipal administrative boundary polygons (e.g. MCD North Zone polygon) do **NOT** automatically imply road maintenance responsibility. Major highways (NHAI) and arterial thoroughfares (PWD) running through municipal districts are maintained by dedicated road authorities.

#### PostGIS Layered Resolution Hierarchy
The resolution engine executes a prioritized spatial evaluation against PostGIS geometry tables:

```
                  +-----------------------------------+
                  | Incoming GPS Coordinate (Point)   |
                  +-----------------+-----------------+
                                    |
                                    v
                  +-----------------------------------+
                  | Layer 1: Road Ownership Check     |
                  | ST_DWithin(road_line, point, 15m) |
                  +-----------------+-----------------+
                                    |
                    +---------------+---------------+
                    | Match Found?                  |
                    +-------+---------------+-------+
                        YES |               | NO
                            v               v
            +-------------------+   +-----------------------------------+
            | Assign Dedicated  |   | Layer 2: Municipal Boundary Check |
            | Road Authority    |   | ST_Covers(jurisdiction_polygon, p)|
            | (e.g., PWD / NHAI)|   +-----------------+-----------------+
            +-------------------+                     |
                                      +---------------+---------------+
                                      | Match Found?                  |
                                      +-------+---------------+-------+
                                          YES |               | NO
                                              v               v
                              +-------------------+   +-------------------+
                              | Assign Municipal  |   | Assign Status     |
                              | Authority         |   | UNKNOWN_AUTHORITY |
                              | (e.g., MCD North) |   | (Requires Triage) |
                              +-------------------+   +-------------------+
```

#### Spatial Predicate Choice: `ST_Covers` vs `ST_Contains`
- **Selection**: `ST_Covers(jurisdiction_boundary, location_point)`
- **Mathematical Rationale**: Under the 9-Intersection Model (DE-9IM), `ST_Contains(A, B)` requires that no point of geometry B lies on the boundary of geometry A. If a pothole GPS coordinate lands precisely on a polygon boundary edge (e.g. boundary along a street center line), `ST_Contains` evaluates to **FALSE**.
- `ST_Covers(A, B)` evaluates to **TRUE** if every point of B lies in the interior or on the boundary of A. Using `ST_Covers` eliminates boundary edge-case failures.

#### Boundary Fallback & `UNKNOWN_AUTHORITY` Rule
- If a point does not match any road network buffer or municipal jurisdiction polygon, the system does **NOT** guess or default to PWD.
- The record is assigned `authority_id = NULL` and authority code `UNKNOWN_AUTHORITY`.
- Reports in `UNKNOWN_AUTHORITY` state trigger an administrative triage flag on the dashboard so municipal operators can manually inspect and assign jurisdiction.

---

### 6. Severity Model & Visual Metrics

#### Metric Distinction
To ensure clarity in technical interviews and code representation, three distinct concepts are enforced:

1. **Model Confidence ($C$)**: The probability ($0.0 \le C \le 1.0$) output by the YOLOv8 model representing certainty that a detected object is a pothole.
2. **Severity Score ($S$)**: A continuous numeric visual heuristic ($0.0 \le S \le 100.0$) calculated from the bounding box surface area ratio relative to image frame dimensions, weighted by confidence:
   $$\text{Visual Area Ratio } (R) = \frac{\text{Bounding Box Width} \times \text{Bounding Box Height}}{\text{Image Frame Width} \times \text{Image Frame Height}}$$
   $$\text{Severity Score } (S) = \min\left(100.0, \, R \times 1000 \times C\right)$$
3. **Severity Classification**: Categorical enum derived from discrete thresholding of Severity Score ($S$):
   - **`LOW`**: $S < 20.0$ (Small/minor surface crack or small pothole occupy $< 2\%$ frame)
   - **`MEDIUM`**: $20.0 \le S < 50.0$ (Moderate road defect occupying $2\% - 5\%$ frame)
   - **`HIGH`**: $S \ge 50.0$ (Large/deep road crater occupying $> 5\%$ frame or multiple potholes $\ge 3$)

> **Visual Heuristic Disclaimer**: The severity score and classification are **2D camera visual surface area heuristics**. They measure the relative optical footprint of dark damaged road surface in camera perspective. They do **NOT** measure actual 3D physical pothole depth, millimeter displacement, or structural vehicle impact risk.

---

### 7. Deduplication Strategy

The system explicitly distinguishes two separate types of deduplication:

#### Type A: Video-Frame Deduplication (Intra-Stream Clustering)
- **Scope**: Applied during asynchronous processing of a single uploaded video file.
- **Mechanism**: Clusters detections across sequential frames occurring within 3 seconds of each other into 1 aggregated `Pothole` entity (as detailed in Section 3).

#### Type B: Citizen & Report Submission Deduplication (Inter-Report Matching)
- **Scope**: Evaluated when a new `Pothole` event is created from an incoming image or video.
- **Deduplication Criteria**:
  1. **Spatial Threshold**: Distance $\le 15.0$ meters using PostGIS geography measurement:
     ```sql
     ST_DWithin(existing_pothole.location::geography, new_point::geography, 15.0)
     ```
  2. **Temporal Window**: Reported within the last **7 days** (`first_detected_at >= NOW() - INTERVAL '7 days'`).
  3. **Eligible Statuses**: Match against unresolved parent potholes (`status IN ('REPORTED', 'ACKNOWLEDGED', 'IN_PROGRESS')`).
- **Domain Relationship & Dispatch Behavior**:
  - If a match is found:
    - New `Pothole` record is created with `is_duplicate = TRUE` and `duplicate_of_id = parent_pothole.id`.
    - **Authority Dispatch Suppression**: Duplicate citizen reports do **NOT** trigger new `Report` dispatches or send repeated alerts to civic authorities.
    - **Vote Accumulation**: The citizen duplicate increments the confirmation count on the parent report, signaling higher urgency to municipal teams.

---

### 8. Automated Reporting & Dispatch Engine

#### Entities: `Report` and `ReportAttempt`

When a new non-duplicate `Pothole` is persisted, Spring Boot triggers the Dispatch Engine:

```
+------------------+           +------------------+           +----------------------+
|  Pothole Entity  |           |   Report Entity  |           |    ReportAttempt     |
| (Non-Duplicate)  |           | (Status: PENDING)|           | (Audit Log Entry)    |
+--------+---------+           +--------+---------+           +----------+-----------+
         |                              |                                |
         | 1. Trigger Dispatch          |                                |
         +----------------------------->|                                |
                                        | 2. Generate Idempotency Key    |
                                        |    report-{id}-{auth_id}-v1    |
                                        |--------------------------------+
                                        |                                |
                                        | 3. Execute Mock Integration    |
                                        |    (Mock Email/SMS/Webhook)    |
                                        |--------------------------------+
                                        |                                |
                                        | 4. Log Attempt Details         |
                                        +------------------------------->|
```

#### Idempotency Key & Retry Architecture
- **Idempotency Key Format**: `report-{potholeId}-{authorityId}-v{attemptCount}`
- **Retry Behavior**: Bounded exponential backoff (Max 3 retries: 2s, 4s, 8s delay).
- **Failure State**: If all 3 attempts fail, `Report.status` is set to `FAILED` and an alert is flagged on the operator dashboard.
- **Audit Trail**: Every execution records an immutable `ReportAttempt` entry storing timestamp, channel, idempotency key, status (`SUCCESS`/`FAILED`), and response payload summary.

#### Simulated / Mock Integration Specification
- **Explicit Scope Notice**: All external communications (Email, SMS, Webhooks) are **simulated mock integrations** designed for assessment execution.
- No live government API credentials or external SMTP servers are required. The Spring Boot backend formats authentic HTML email templates, JSON webhook payloads, and SMS strings, executing real dispatch logging to PostgreSQL and application logs.

---

### 9. Error-Handling Strategy

- **Spring Boot Global Exception Handler**: `@ControllerAdvice` maps domain exceptions to RFC 7807 `ProblemDetails` standard JSON responses:
  - `400 Bad Request`: Invalid spatial coordinates, unsupported MIME type.
  - `404 Not Found`: Pothole ID or DetectionJob ID not found.
  - `422 Unprocessable Entity`: AI service unable to read corrupted media file.
  - `503 Service Unavailable`: FastAPI AI service or MinIO storage unreachable.
- **FastAPI AI Service Resilience**:
  - Validates image header magic bytes.
  - If no potholes are detected, returns `pothole_count: 0` with HTTP `200 OK` and an empty detections array (avoids throwing HTTP 500 exceptions on clean roads).

---

### 10. Testing Strategy

1. **Backend Unit & Integration Tests (JUnit 5 + Testcontainers)**:
   - `AuthorityResolverServiceTest`: Validates `ST_Covers` and road network spatial lookup using PostGIS Testcontainer.
   - `DeduplicationServiceTest`: Verifies 15m spatial radius and 7-day window duplicate matching.
   - `ReportDispatchServiceTest`: Verifies idempotency key generation and retry backoff.
   - `PotholeControllerTest`: Tests REST endpoints using `MockMvc`.
2. **AI Service Tests (Pytest)**:
   - Validates ONNX model loading, tensor preprocessing, and NMS bounding box output formats.
3. **Frontend Integration Tests (Vitest + React Testing Library)**:
   - Tests file upload form validation, Leaflet map marker rendering, and dashboard filter controls.

---

### 11. Docker Architecture

`docker-compose.yml` orchestrates 5 isolated containers:

```yaml
version: '3.8'

services:
  postgres:
    image: postgis/postgis:16-3.4-alpine
    container_name: pothole_postgres
    environment:
      POSTGRES_DB: potholedb
      POSTGRES_USER: pothole_user
      POSTGRES_PASSWORD: pothole_password
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U pothole_user -d potholedb"]
      interval: 5s
      timeout: 5s
      retries: 5

  minio:
    image: minio/minio:RELEASE.2024-01-31T03-17-18Z
    container_name: pothole_minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadminpassword
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - miniodata:/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 5s
      timeout: 5s
      retries: 5

  ai-service:
    build: ./ai-service
    container_name: pothole_ai_service
    ports:
      - "8000:8000"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8000/health"]
      interval: 5s
      timeout: 5s
      retries: 5

  backend:
    build: ./backend
    container_name: pothole_backend
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/potholedb
      SPRING_DATASOURCE_USERNAME: pothole_user
      SPRING_DATASOURCE_PASSWORD: pothole_password
      MINIO_ENDPOINT: http://minio:9000
      AI_SERVICE_URL: http://ai-service:8000
    depends_on:
      postgres:
        condition: service_healthy
      minio:
        condition: service_healthy
      ai-service:
        condition: service_healthy

  frontend:
    build: ./frontend
    container_name: pothole_frontend
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  pgdata:
  miniodata:
```

---

### 12. Database Schema (Flyway Migration SQL)

#### Migration: `V1__init_schema.sql`

```sql
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Media Assets Table (Binary pointers in MinIO)
CREATE TABLE media_assets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    media_type VARCHAR(20) NOT NULL, -- IMAGE, VIDEO
    raw_object_key VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Detection Jobs Table (Async job tracking)
CREATE TABLE detection_jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    media_asset_id UUID NOT NULL REFERENCES media_assets(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL, -- PENDING, PROCESSING, COMPLETED, FAILED
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    error_summary TEXT
);

-- Civic Authorities
CREATE TABLE civic_authorities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    contact_email VARCHAR(150),
    contact_phone VARCHAR(30),
    department_type VARCHAR(50) NOT NULL, -- MUNICIPAL, PWD, HIGHWAY
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Authority Jurisdictions (Spatial boundaries & road lines)
CREATE TABLE authority_jurisdictions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    authority_id UUID NOT NULL REFERENCES civic_authorities(id) ON DELETE CASCADE,
    jurisdiction_type VARCHAR(30) NOT NULL, -- MUNICIPAL_BOUNDARY, ROAD_NETWORK
    geometry GEOMETRY(Geometry, 4326) NOT NULL
);
CREATE INDEX idx_jurisdictions_geometry ON authority_jurisdictions USING GIST (geometry);

-- Business Potholes Table
CREATE TABLE potholes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    location GEOMETRY(Point, 4326) NOT NULL,
    address_text VARCHAR(255),
    first_detected_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    severity_score FLOAT NOT NULL,
    severity_class VARCHAR(20) NOT NULL, -- LOW, MEDIUM, HIGH
    max_confidence FLOAT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REPORTED', -- REPORTED, ACKNOWLEDGED, IN_PROGRESS, RESOLVED
    is_duplicate BOOLEAN DEFAULT FALSE,
    duplicate_of_id UUID REFERENCES potholes(id),
    civic_authority_id UUID REFERENCES civic_authorities(id),
    representative_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_potholes_location ON potholes USING GIST (location);
CREATE INDEX idx_potholes_status ON potholes (status);
CREATE INDEX idx_potholes_severity ON potholes (severity_class);

-- Raw Frame Detections Table
CREATE TABLE detections (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    detection_job_id UUID NOT NULL REFERENCES detection_jobs(id) ON DELETE CASCADE,
    pothole_id UUID REFERENCES potholes(id) ON DELETE SET NULL,
    frame_index INT DEFAULT 0,
    frame_timestamp_sec FLOAT DEFAULT 0.0,
    box_xmin INT NOT NULL,
    box_ymin INT NOT NULL,
    box_xmax INT NOT NULL,
    box_ymax INT NOT NULL,
    confidence FLOAT NOT NULL,
    visual_area_ratio FLOAT NOT NULL
);

-- Status Audit History
CREATE TABLE pothole_status_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    pothole_id UUID NOT NULL REFERENCES potholes(id) ON DELETE CASCADE,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    changed_by VARCHAR(100) DEFAULT 'SYSTEM',
    notes TEXT
);

-- Reports Table
CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    pothole_id UUID NOT NULL REFERENCES potholes(id) ON DELETE CASCADE,
    authority_id UUID REFERENCES civic_authorities(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, DISPATCHED, FAILED
    created_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Report Dispatch Attempts Table
CREATE TABLE report_attempts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_id UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    channel VARCHAR(30) NOT NULL, -- MOCK_EMAIL, MOCK_SMS, MOCK_WEBHOOK
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    attempt_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL, -- SUCCESS, FAILED
    response_summary TEXT
);
```

---

### 13. Service Contracts & API Specifications

#### Spring Boot REST API Contracts

##### 1. Image Pothole Detection & Ingestion
- **`POST /api/v1/potholes/detect-image`** (`multipart/form-data`)
  - Params: `file` (Image binary), `latitude` (Double), `longitude` (Double)
  - Response (`201 Created`):
```json
{
  "potholeId": "c39e4a82-1234-4b55-8910-abcdef123456",
  "location": { "latitude": 28.6139, "longitude": 77.2090 },
  "addressText": "Rajpath Area, Central Delhi",
  "firstDetectedAt": "2026-09-10T18:20:00Z",
  "severityScore": 42.5,
  "severityClass": "MEDIUM",
  "maxConfidence": 0.89,
  "representativeImageUrl": "http://localhost:9000/pothole-annotated/ann_171000.jpg",
  "civicAuthority": {
    "id": "a1b2c3d4-0000-0000-0000-000000000001",
    "name": "New Delhi Municipal Council (NDMC)",
    "code": "NDMC_CENTRAL"
  },
  "status": "REPORTED",
  "isDuplicate": false,
  "duplicateOfId": null
}
```

##### 2. Asynchronous Video Detection Ingestion
- **`POST /api/v1/potholes/detect-video`** (`multipart/form-data`)
  - Params: `file` (MP4 video binary), `latitude` (Double), `longitude` (Double)
  - Response (`202 Accepted`):
```json
{
  "jobId": "e88f99a0-1111-2222-3333-444455556666",
  "status": "PENDING",
  "mediaAssetId": "f77a88b9-0000-1111-2222-333344445555",
  "pollUrl": "/api/v1/detection-jobs/e88f99a0-1111-2222-3333-444455556666"
}
```

##### 3. Poll Detection Job Status
- **`GET /api/v1/detection-jobs/{jobId}`**
  - Response (`200 OK`):
```json
{
  "jobId": "e88f99a0-1111-2222-3333-444455556666",
  "status": "COMPLETED",
  "startedAt": "2026-09-10T18:21:00Z",
  "completedAt": "2026-09-10T18:21:04Z",
  "result": {
    "potholeId": "d49e4a82-5678-4b55-8910-abcdef654321",
    "totalFramesSampled": 20,
    "framesWithPotholes": 6,
    "aggregatedPothole": {
      "severityClass": "HIGH",
      "severityScore": 68.4,
      "maxConfidence": 0.92,
      "representativeImageUrl": "http://localhost:9000/pothole-annotated/peak_99.jpg"
    }
  }
}
```

##### 4. Update Status & Audit Timeline
- **`PATCH /api/v1/potholes/{id}/status`**
  - Body: `{ "newStatus": "IN_PROGRESS", "changedBy": "Inspector Rajesh", "notes": "Work order issued" }`
  - Response (`200 OK`): Updated DTO with appended `PotholeStatusHistory` timeline.

#### FastAPI AI Service Contract

##### 1. Detect Image Potholes
- **`POST /detect/image`** (`multipart/form-data`)
  - Params: `file` (UploadFile stream)
  - Response (`200 OK`): Structured JSON metadata + raw annotated frame binary stream.
```json
{
  "pothole_count": 1,
  "max_confidence": 0.885,
  "max_area_ratio": 0.042,
  "detections": [
    {
      "box": [120, 85, 340, 290],
      "confidence": 0.885,
      "class_id": 0,
      "class_name": "pothole",
      "visual_area_ratio": 0.042
    }
  ]
}
```

##### 2. Detect Video Potholes
- **`POST /detect/video`** (`multipart/form-data`)
  - Params: `file` (UploadFile stream), `fps_sample_rate` (default 2.0)
  - Response (`200 OK`): Structured JSON array of sampled frame detections + representative keyframe binary.

---

### 14. Implementation Milestones

- **Milestone 1: Containerized Infrastructure & Flyway Schema Bootstrap**
  - Configure Docker Compose with PostGIS 16 & MinIO.
  - Create Flyway migrations (`V1__init_schema.sql` and `V2__seed_authorities.sql`).
- **Milestone 2: Stateless AI Inference Service**
  - Implement FastAPI ONNX YOLOv8 loader, structured detection formatting, OpenCV keyframe drawing, and video frame sampling (2 FPS).
- **Milestone 3: Spring Boot Core Services & Asynchronous Execution**
  - MinIO S3 integration, REST controllers, `ThreadPoolTaskExecutor` for `DetectionJob`, `ST_Covers` PostGIS authority resolver, 15m duplicate engine, status audit logger.
- **Milestone 4: Simulated Automated Dispatch Engine**
  - Implement `Report` and `ReportAttempt` logic with idempotency key generation, 3-tier retry backoff, and mock email/SMS logging.
- **Milestone 5: React Dashboard & Leaflet Map UI**
  - Leaflet map view with severity markers, image/video upload modals with job polling, filter controls, and status history timeline.
- **Milestone 6: Verification & Polish**
  - Run full test suite (JUnit 5, Pytest, Vitest), verify complete Docker Compose setup, update README, and prepare demo video.

---

### 15. Architecture Decisions Frozen for Implementation

> [!IMPORTANT]
> **FROZEN ARCHITECTURE DIRECTIVES**: All subsequent implementation tasks MUST strictly adhere to the following 20 architectural rules without deviation:

1. **Technology Versions**: Enforce React `18.2.0`, TypeScript `5.3.3`, Vite `5.1.0`, Leaflet `1.9.4`, Java `21`, Spring Boot `3.2.3`, Python `3.11`, FastAPI `0.110.0`, ONNX Runtime `1.17.1`, OpenCV `4.9.0`, PostgreSQL `16.2`, PostGIS `3.4.1`, MinIO `RELEASE.2024-01-31`.
2. **Domain Concept Separation**: Maintain clean domain entity boundaries: `MediaAsset`, `DetectionJob`, `Pothole`, `Detection`, `PotholeStatusHistory`, `Authority`, `AuthorityJurisdiction`, `Report`, `ReportAttempt`.
3. **Video Frame Aggregation**: Individual video frame detections are CV raw metrics. Multiple frame detections within 3s are aggregated into ONE single business `Pothole` event.
4. **Asynchronous Execution**: Video processing MUST use asynchronous `DetectionJob` execution via Spring's `ThreadPoolTaskExecutor` (returning HTTP 202 Accepted + polling endpoint). No Kafka or RabbitMQ.
5. **Structured Inter-Service Payloads**: AI service returns structured JSON metadata. Primary communication between services MUST NOT rely on Base64 strings.
6. **Media Storage Isolation**: Binary media files reside exclusively in MinIO (`pothole-raw`, `pothole-annotated`). PostgreSQL stores ONLY string object keys, spatial points, and metadata.
7. **Authority Resolution via Spatial Layers**: Authority matching MUST evaluate spatial road networks first (PWD/NHAI line buffers) before falling back to municipal boundary polygons. Municipal boundaries do not automatically imply road ownership.
8. **`UNKNOWN_AUTHORITY` Fallback**: If coordinates lie outside all spatial boundaries, assign `authority_id = NULL` and code `UNKNOWN_AUTHORITY`. Do NOT default to PWD.
9. **`ST_Covers` Spatial Predicate**: Use `ST_Covers(jurisdiction_boundary, location_point)` to ensure boundary edge-case points are correctly covered.
10. **Metric Separation**: Keep Model Confidence ($C$), Severity Score ($S$), and Severity Classification (`LOW`, `MEDIUM`, `HIGH`) strictly distinct.
11. **Visual Heuristic Disclaimer**: Document that severity is a 2D camera visual surface footprint heuristic, NOT a 3D physical depth measurement.
12. **Deduplication Types**: Distinguish Type A (video-frame intra-stream clustering) from Type B (inter-report citizen duplicate submissions).
13. **Citizen Duplicate Strategy**: Type B duplicates use 15m PostGIS radius (`ST_DWithin`), 7-day temporal window, active status filter (`REPORTED`, `ACKNOWLEDGED`, `IN_PROGRESS`), link via `duplicate_of_id`, and SUPPRESS redundant authority report dispatches.
14. **Reporting Mechanism**: Reports use `Report` and `ReportAttempt` entities with unique idempotency keys (`report-{potholeId}-{authorityId}-v{attemptCount}`), bounded 3-tier exponential backoff retries, and full audit logging.
15. **Simulated Dispatches**: External authority communications (Email, SMS, Webhooks) are strictly simulated mock integration drivers for assessment purposes.
16. **No Infrastructure Over-Engineering**: Do NOT introduce Kafka, Redis, Kubernetes, GraphQL, or complex auth frameworks.
17. **Video Context Retention**: Video processing preserves all sampled frame `Detection` entities (timestamps, frame indices, bounding boxes) alongside the aggregated `Pothole` and representative keyframe.
18. **Backend System of Record**: Spring Boot remains the sole System of Record; FastAPI remains 100% stateless and database-free.
19. **Assessment Appropriate**: Maintain a clean, production-minded microservices design that can be executed via `docker-compose up` and clearly explained in a technical interview.
20. **Frozen Architecture Compliance**: No coding or architectural alterations allowed without updating this specification document first.
