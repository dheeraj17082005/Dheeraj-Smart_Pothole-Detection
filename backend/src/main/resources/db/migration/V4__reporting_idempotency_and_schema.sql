-- V4: Add idempotency and constraints for reports and report attempts

-- Add idempotency_key and external_reference to reports
ALTER TABLE reports ADD COLUMN idempotency_key VARCHAR(255) UNIQUE;
ALTER TABLE reports ADD COLUMN external_reference VARCHAR(255);
ALTER TABLE reports ADD CONSTRAINT uq_reports_pothole_authority UNIQUE (pothole_id, authority_id);

-- Drop the old unique constraint on report_attempts.idempotency_key
-- because all retry attempts for the same report share the logical idempotency key
ALTER TABLE report_attempts DROP CONSTRAINT IF EXISTS report_attempts_idempotency_key_key;

-- Add attempt_number to report_attempts
ALTER TABLE report_attempts ADD COLUMN attempt_number INT NOT NULL DEFAULT 1;
ALTER TABLE report_attempts ADD CONSTRAINT uq_report_attempts_number UNIQUE (report_id, attempt_number);
