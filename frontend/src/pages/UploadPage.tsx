import React, { useState } from 'react';
import { UploadModeSwitch, UploadMode } from '../components/upload/UploadModeSwitch';
import { FileDropzone } from '../components/upload/FileDropzone';
import { LocationPicker } from '../components/upload/LocationPicker';
import { ImageDetectionResult } from '../components/detection/ImageDetectionResult';
import { VideoJobTracker } from '../components/detection/VideoJobTracker';
import { Alert } from '../components/common/Alert';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { apiClient } from '../services/api';
import { DetectImageResponse } from '../types';
import { validateCoordinates } from '../utils/validation';

export const UploadPage: React.FC = () => {
  const [mode, setMode] = useState<UploadMode>('IMAGE');
  const [file, setFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);

  const [latitude, setLatitude] = useState<string>('');
  const [longitude, setLongitude] = useState<string>('');
  const [addressText, setAddressText] = useState<string>('');
  const [capturedAt, setCapturedAt] = useState<string>('');

  const [submitting, setSubmitting] = useState<boolean>(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const [imageResult, setImageResult] = useState<DetectImageResponse | null>(null);
  const [activeJobId, setActiveJobId] = useState<string | null>(null);

  const latNum = parseFloat(latitude);
  const lonNum = parseFloat(longitude);
  const coordsValidation = validateCoordinates(
    latitude.trim() !== '' ? latNum : null,
    longitude.trim() !== '' ? lonNum : null
  );

  const isFormValid =
    file !== null &&
    !fileError &&
    latitude.trim() !== '' &&
    longitude.trim() !== '' &&
    coordsValidation.valid;

  const handleReset = () => {
    setFile(null);
    setFileError(null);
    setImageResult(null);
    setActiveJobId(null);
    setSubmitError(null);
  };

  const handleModeChange = (newMode: UploadMode) => {
    setMode(newMode);
    handleReset();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isFormValid || !file) return;

    setSubmitting(true);
    setSubmitError(null);

    const isoCapturedAt = capturedAt ? new Date(capturedAt).toISOString() : undefined;

    try {
      if (mode === 'IMAGE') {
        const response = await apiClient.detectImage({
          file,
          latitude: latNum,
          longitude: lonNum,
          capturedAt: isoCapturedAt,
          addressText: addressText.trim() || undefined
        });
        setImageResult(response);
      } else {
        const response = await apiClient.detectVideo({
          file,
          latitude: latNum,
          longitude: lonNum,
          capturedAt: isoCapturedAt,
          addressText: addressText.trim() || undefined
        });
        setActiveJobId(response.jobId);
      }
    } catch (err: any) {
      setSubmitError(err.message || 'An error occurred during submission. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ maxWidth: '960px', margin: '0 auto' }}>
      {/* Page Header */}
      <div style={{ marginBottom: '1.75rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
          <span style={{ fontSize: '0.72rem', fontWeight: 800, color: 'var(--brand-accent)', textTransform: 'uppercase', letterSpacing: '0.08em' }}>
            ROAD DEFECT REPORT
          </span>
          <span style={{ color: 'var(--border)' }}>&bull;</span>
          <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>CITIZEN EVIDENCE SUBMISSION</span>
        </div>
        <h1
          style={{ fontSize: '1.95rem', fontWeight: 800, color: 'var(--text-main)', letterSpacing: '-0.025em', margin: '0 0 0.35rem 0' }}
          aria-label="Report Road Surface Defect"
        >
          REPORT ROAD DEFECT
        </h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem', margin: 0 }}>
          Upload a road image or dashcam video to report visible potholes for municipal verification and remediation.
        </p>
      </div>

      <UploadModeSwitch
        mode={mode}
        onChange={handleModeChange}
        disabled={submitting || activeJobId !== null}
      />

      {/* Model Domain Guidance */}
      <div
        style={{
          backgroundColor: 'var(--surface)',
          color: 'var(--text-main)',
          borderRadius: '10px',
          padding: '0.85rem 1.25rem',
          fontSize: '0.85rem',
          marginBottom: '1.5rem',
          display: 'flex',
          alignItems: 'center',
          gap: '0.75rem',
          border: '1px solid var(--border)'
        }}
        data-testid="upload-guidance"
      >
        <span style={{ fontSize: '1.25rem' }}>💡</span>
        <span>
          <strong style={{ color: 'var(--brand-accent)' }}>Optimal Accuracy:</strong> Best results come from road-facing mobile or dashcam views where the asphalt road surface and pothole are clearly visible.
        </span>
      </div>

      {submitError && (
        <Alert
          variant="error"
          title="Submission Failed"
          message={submitError}
          onDismiss={() => setSubmitError(null)}
        />
      )}

      {/* Active Results */}
      {imageResult && (
        <ImageDetectionResult
          response={imageResult}
          onReset={handleReset}
        />
      )}

      {activeJobId && (
        <VideoJobTracker
          jobId={activeJobId}
          onReset={handleReset}
        />
      )}

      {/* Upload Form */}
      {!imageResult && !activeJobId && (
        <form onSubmit={handleSubmit} className="card" data-testid="upload-form">
          <FileDropzone
            mode={mode}
            file={file}
            onFileSelect={setFile}
            onError={setFileError}
            disabled={submitting}
          />

          {fileError && (
            <Alert
              variant="error"
              message={fileError}
              onDismiss={() => setFileError(null)}
            />
          )}

          <hr style={{ border: 'none', borderTop: '1px solid var(--border)', margin: '1.5rem 0' }} />

          <LocationPicker
            latitude={latitude}
            longitude={longitude}
            addressText={addressText}
            capturedAt={capturedAt}
            onLatitudeChange={setLatitude}
            onLongitudeChange={setLongitude}
            onAddressChange={setAddressText}
            onCapturedAtChange={setCapturedAt}
            disabled={submitting}
          />

          {!coordsValidation.valid && (latitude !== '' || longitude !== '') && (
            <div style={{ fontSize: '0.825rem', color: '#EF4444', marginTop: '0.5rem', fontWeight: 600 }}>
              ⚠️ {coordsValidation.error}
            </div>
          )}

          <div style={{ marginTop: '2rem', display: 'flex', justifyContent: 'flex-end', gap: '0.85rem' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={handleReset}
              disabled={submitting || (!file && !latitude && !longitude)}
            >
              Reset
            </button>

            <button
              type="submit"
              className="btn btn-primary btn-lg"
              disabled={!isFormValid || submitting}
              data-testid="submit-button"
            >
              {submitting ? (
                <LoadingSpinner
                  size={16}
                  label={mode === 'IMAGE' ? 'Analyzing road image...' : 'Uploading dashcam video survey...'}
                  inline
                />
              ) : (
                mode === 'IMAGE' ? 'ANALYZE ROAD' : 'SUBMIT DASHCAM VIDEO'
              )}
            </button>
          </div>
        </form>
      )}
    </div>
  );
};

export default UploadPage;
