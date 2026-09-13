import React from 'react';
import { useDetectionJob } from '../../hooks/useDetectionJob';
import { JobStatusBadge } from '../common/Badge';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { Alert } from '../common/Alert';
import { formatDuration } from '../../utils/formatters';
import { Link } from 'react-router-dom';

export interface VideoJobTrackerProps {
  jobId: string;
  onReset?: () => void;
}

export const VideoJobTracker: React.FC<VideoJobTrackerProps> = ({ jobId, onReset }) => {
  const { job, loading, error, isPolling, pollCount } = useDetectionJob(jobId);

  const status = job?.status || 'PENDING';
  const progressPercent = job?.progress !== undefined && job?.progress !== null
    ? Math.min(100, Math.max(0, Math.round(job.progress * 100)))
    : status === 'COMPLETED' ? 100 : 25;

  const durationText = job?.startedAt && job?.completedAt
    ? formatDuration(new Date(job.completedAt).getTime() - new Date(job.startedAt).getTime())
    : null;

  return (
    <div className="card" style={{ marginTop: '2rem', padding: '1.75rem' }} data-testid="video-job-tracker">
      {/* Header */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '1.5rem',
          borderBottom: '1px solid var(--border)',
          paddingBottom: '1rem',
          flexWrap: 'wrap',
          gap: '1rem'
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <span style={{ fontSize: '0.72rem', fontWeight: 800, color: 'var(--brand-accent)', textTransform: 'uppercase', letterSpacing: '0.08em' }}>
              VIDEO SURVEY
            </span>
            <span style={{ color: 'var(--border)' }}>&bull;</span>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>ASYNC VIDEO PROCESSING</span>
          </div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-main)', margin: '0.2rem 0 0 0', letterSpacing: '-0.02em' }}>
            DASHCAM ROAD INSPECTION
          </h2>
          <p style={{ margin: '0.25rem 0 0 0', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            Analyze uploaded road footage for repeated pothole observations and track defect locations.
          </p>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-subtle)', marginTop: '0.35rem' }}>
            Job Reference: <code style={{ background: 'var(--surface-elevated)', padding: '0.15rem 0.35rem', borderRadius: '4px', fontFamily: 'var(--font-mono)', border: '1px solid var(--border)' }} data-testid="job-id">{jobId}</code>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <JobStatusBadge status={status} />
          {onReset && (
            <button type="button" className="btn btn-secondary btn-sm" onClick={onReset}>
              Submit Another Video
            </button>
          )}
        </div>
      </div>

      {/* 3-Step Human-Readable Workflow Progress */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: '0.75rem',
          backgroundColor: 'var(--surface-elevated)',
          padding: '0.75rem 1rem',
          borderRadius: '8px',
          border: '1px solid var(--border)',
          marginBottom: '1.5rem'
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <div style={{ width: '20px', height: '20px', borderRadius: '50%', backgroundColor: '#10B981', color: '#0B0D0F', fontSize: '0.7rem', fontWeight: 800, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>✓</div>
          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-main)' }}>1. Video Uploaded</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <div style={{ width: '20px', height: '20px', borderRadius: '50%', backgroundColor: status === 'COMPLETED' ? '#10B981' : isPolling ? 'var(--brand-accent)' : 'var(--surface)', color: '#0B0D0F', fontSize: '0.7rem', fontWeight: 800, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            {status === 'COMPLETED' ? '✓' : '2'}
          </div>
          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: isPolling ? 'var(--brand-accent)' : 'var(--text-main)' }}>2. Frame Sampling &amp; Analysis</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <div style={{ width: '20px', height: '20px', borderRadius: '50%', backgroundColor: status === 'COMPLETED' ? '#10B981' : 'var(--surface)', color: status === 'COMPLETED' ? '#0B0D0F' : 'var(--text-muted)', fontSize: '0.7rem', fontWeight: 800, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            {status === 'COMPLETED' ? '✓' : '3'}
          </div>
          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: status === 'COMPLETED' ? '#10B981' : 'var(--text-muted)' }}>3. Inspection Complete</span>
        </div>
      </div>

      {error && (
        <Alert
          variant="error"
          title="Video Processing Error"
          message={error}
        />
      )}

      {/* Live Polling / Progress Section */}
      {isPolling && (
        <div
          style={{
            backgroundColor: 'var(--surface-elevated)',
            color: 'var(--text-main)',
            padding: '2rem 1.5rem',
            borderRadius: '12px',
            marginBottom: '1.5rem',
            textAlign: 'center',
            border: '1px solid var(--border)'
          }}
        >
          <LoadingSpinner
            size={36}
            label={
              status === 'PENDING'
                ? 'Video job queued in background...'
                : `Sampling frames at 2.0 FPS & detecting road surface hazards... (Update #${pollCount})`
            }
          />

          <div
            style={{
              width: '100%',
              maxWidth: '440px',
              margin: '1.25rem auto 0 auto',
              backgroundColor: 'var(--surface)',
              borderRadius: '9999px',
              height: '8px',
              overflow: 'hidden',
              border: '1px solid var(--border)'
            }}
          >
            <div
              style={{
                width: `${progressPercent}%`,
                backgroundColor: 'var(--brand-accent)',
                height: '100%',
                transition: 'width 0.4s ease',
                boxShadow: 'var(--shadow-glow)'
              }}
            />
          </div>

          <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: '0.75rem' }}>
            Status: <strong style={{ color: 'var(--text-main)' }}>{status}</strong> &bull; Sampling rate: 2.0 FPS &bull; Asynchronous video processing
          </div>

          <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', marginTop: '0.5rem', fontStyle: 'italic' }}>
            📍 Survey starting location: Detections in this video survey are associated with the initial GPS coordinates provided during upload.
          </div>
        </div>
      )}

      {/* Completed Results View */}
      {status === 'COMPLETED' && job?.result && (
        <div>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
              gap: '1rem',
              marginBottom: '1.5rem'
            }}
          >
            <div style={{ backgroundColor: 'var(--surface-elevated)', border: '1px solid var(--border)', padding: '1rem', borderRadius: '10px' }}>
              <div style={{ fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                Frames Analyzed
              </div>
              <div style={{ fontSize: '1.95rem', fontWeight: 800, color: 'var(--text-main)', margin: '0.2rem 0' }} data-testid="sampled-frames">
                {job.result.totalFramesSampled}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                Sample Rate: 2.0 FPS
              </div>
            </div>

            <div style={{ backgroundColor: 'var(--surface-elevated)', border: '1px solid var(--border)', padding: '1rem', borderRadius: '10px' }}>
              <div style={{ fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                Frames With Defects
              </div>
              <div style={{ fontSize: '1.95rem', fontWeight: 800, color: 'var(--brand-accent)', margin: '0.2rem 0' }} data-testid="frames-with-potholes">
                {job.result.framesWithPotholes}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                Defect sightings identified
              </div>
            </div>

            <div style={{ backgroundColor: 'var(--surface-elevated)', border: '1px solid var(--border)', padding: '1rem', borderRadius: '10px' }}>
              <div style={{ fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                Potholes Logged
              </div>
              <div style={{ fontSize: '1.95rem', fontWeight: 800, color: '#10B981', margin: '0.2rem 0' }} data-testid="potholes-created">
                {job.result.potholesCreated}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                Aggregated road defect records
              </div>
            </div>

            <div style={{ backgroundColor: 'var(--surface-elevated)', border: '1px solid var(--border)', padding: '1rem', borderRadius: '10px' }}>
              <div style={{ fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                Duplicates Filtered
              </div>
              <div style={{ fontSize: '1.95rem', fontWeight: 800, color: '#0284C7', margin: '0.2rem 0' }} data-testid="duplicates-detected">
                {job.result.duplicatesDetected}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                Matching existing sightings
              </div>
            </div>
          </div>

          {/* Location note */}
          <div style={{ backgroundColor: 'var(--surface)', padding: '0.75rem 1rem', borderRadius: '8px', border: '1px solid var(--border)', fontSize: '0.78rem', color: 'var(--text-muted)', marginBottom: '1.25rem' }}>
            📍 <strong>Survey Location Note:</strong> Detections from this video survey inherit the starting survey GPS coordinates provided during upload.
          </div>

          <div
            style={{
              backgroundColor: 'var(--surface-elevated)',
              border: '1px solid var(--border)',
              borderRadius: '10px',
              padding: '1rem 1.25rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              flexWrap: 'wrap',
              gap: '1rem'
            }}
          >
            <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              <strong>Execution Time:</strong> {durationText || 'Completed'} &bull; Started:{' '}
              {job.startedAt ? new Date(job.startedAt).toLocaleTimeString() : 'N/A'} &bull; Finished:{' '}
              {job.completedAt ? new Date(job.completedAt).toLocaleTimeString() : 'N/A'}
            </div>

            <div style={{ display: 'flex', gap: '0.75rem' }}>
              <Link
                to="/map"
                className="btn btn-secondary btn-sm"
              >
                📍 View on Map
              </Link>
              <Link
                to="/potholes"
                className="btn btn-primary btn-sm"
              >
                View Road Defect Registry →
              </Link>
            </div>
          </div>
        </div>
      )}

      {status === 'FAILED' && (
        <div style={{ padding: '1.25rem', backgroundColor: 'rgba(239, 68, 68, 0.15)', borderRadius: '10px', border: '1px solid rgba(239, 68, 68, 0.4)', color: '#FCA5A5' }} data-testid="job-failed-banner">
          <h3 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '0.35rem', color: '#ffffff' }}>Dashcam Inspection Failed</h3>
          <p style={{ fontSize: '0.875rem' }}>
            {job?.error || error || 'The video processing job encountered an unexpected execution error.'}
          </p>
        </div>
      )}
    </div>
  );
};

