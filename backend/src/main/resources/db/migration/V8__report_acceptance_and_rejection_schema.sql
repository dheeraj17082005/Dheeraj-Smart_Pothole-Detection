-- V8__report_acceptance_and_rejection_schema.sql
-- Adds columns for report creator, acceptance, rejection tracking, and spatial status indexes

ALTER TABLE potholes
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS accepted_by_user_id BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS rejected_by_user_id BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(50),
    ADD COLUMN IF NOT EXISTS rejection_note TEXT;

-- Create indexes for spatial report queries and user ownership
CREATE INDEX IF NOT EXISTS idx_potholes_created_by_user ON potholes(created_by_user_id);
CREATE INDEX IF NOT EXISTS idx_potholes_status_location ON potholes(status, first_detected_at);
