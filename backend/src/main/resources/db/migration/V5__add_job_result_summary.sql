-- V5: Add result_summary column to detection_jobs table

ALTER TABLE detection_jobs ADD COLUMN result_summary TEXT;
