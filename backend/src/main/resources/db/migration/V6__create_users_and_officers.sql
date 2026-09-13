-- V6: Create users, officer profiles, officer jurisdictions, and link to potholes

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_email ON users(email);

CREATE TABLE officer_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    department VARCHAR(255) NOT NULL,
    officer_id_code VARCHAR(100) NOT NULL,
    id_card_object_key VARCHAR(500) NOT NULL,
    verification_status VARCHAR(50) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    verified_at TIMESTAMP WITH TIME ZONE,
    verified_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_officer_profiles_status ON officer_profiles(verification_status);

CREATE TABLE officer_jurisdictions (
    id BIGSERIAL PRIMARY KEY,
    officer_profile_id BIGINT NOT NULL REFERENCES officer_profiles(id) ON DELETE CASCADE,
    jurisdiction_name VARCHAR(255) NOT NULL,
    office_location GEOMETRY(Point, 4326) NOT NULL,
    radius_km DOUBLE PRECISION NOT NULL DEFAULT 5.0,
    boundary_polygon GEOMETRY(Polygon, 4326),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_officer_jurisdictions_office ON officer_jurisdictions USING GIST (office_location);
CREATE INDEX idx_officer_jurisdictions_poly ON officer_jurisdictions USING GIST (boundary_polygon);

ALTER TABLE potholes ADD COLUMN user_id BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE potholes ADD COLUMN assigned_officer_id BIGINT REFERENCES officer_profiles(id) ON DELETE SET NULL;

CREATE INDEX idx_potholes_user_id ON potholes(user_id);
CREATE INDEX idx_potholes_assigned_officer_id ON potholes(assigned_officer_id);
