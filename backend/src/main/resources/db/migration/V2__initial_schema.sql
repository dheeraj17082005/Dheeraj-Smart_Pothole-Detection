-- V2: Initial Schema for Smart Pothole Detection and Reporting System

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
