import React, { useState } from 'react';
import { DetectImageResponse } from '../../types';
import { SeverityBadge, DuplicateBadge, PotholeStatusBadge } from '../common/Badge';
import { formatConfidence, formatCoordinates } from '../../utils/formatters';
import { ImageLightboxModal } from '../common/ImageLightboxModal';
import { useNavigate, Link } from 'react-router-dom';

export interface ImageDetectionResultProps {
  response: DetectImageResponse;
  onReset?: () => void;
}

export const ImageDetectionResult: React.FC<ImageDetectionResultProps> = ({
  response,
  onReset
}) => {
  const navigate = useNavigate();
  const [isLightboxOpen, setIsLightboxOpen] = useState(false);
  const { pothole, pothole_count, pothole_created, message } = response;

  const hasPothole = pothole_count > 0 && !!pothole;

  return (
    <div className="card" style={{ marginTop: '2rem', padding: '1.75rem' }} data-testid="image-detection-result">
      {/* Top Result Header */}
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
            <span
              style={{
                fontSize: '0.72rem',
                fontWeight: 800,
                color: hasPothole ? 'var(--brand-accent)' : '#10B981',
                textTransform: 'uppercase',
                letterSpacing: '0.08em'
              }}
            >
              {hasPothole ? 'DEFECT DETECTED' : 'INSPECTION COMPLETE'}
            </span>
            <span style={{ color: 'var(--border)' }}>&bull;</span>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
              RECORD #{response.job?.id?.slice(0, 8) || 'N/A'}
            </span>
          </div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-main)', margin: '0.2rem 0 0 0', letterSpacing: '-0.02em' }}>
            {hasPothole ? 'ROAD DEFECT IDENTIFIED' : 'ROAD SURFACE CLEAR'}
          </h2>
        </div>

        <div style={{ display: 'flex', gap: '0.6rem', flexWrap: 'wrap' }}>
          {hasPothole && pothole && (
            <>
              <Link
                to={`/map?lat=${pothole.latitude}&lng=${pothole.longitude}&id=${pothole.id}`}
                className="btn btn-secondary btn-sm"
              >
                📍 View on Map
              </Link>
              <button
                type="button"
                className="btn btn-primary btn-sm"
                onClick={() => navigate(`/potholes/${pothole.id}`)}
              >
                View in Registry →
              </button>
            </>
          )}
          {onReset && (
            <button type="button" className="btn btn-secondary btn-sm" onClick={onReset}>
              Inspect Another Image
            </button>
          )}
        </div>
      </div>

      {!hasPothole ? (
        /* Reassurance State on Dark Surface */
        <div
          style={{
            backgroundColor: 'var(--surface-elevated)',
            border: '1px solid rgba(16, 185, 129, 0.3)',
            borderRadius: '12px',
            padding: '2.5rem 1.5rem',
            textAlign: 'center',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '0.85rem'
          }}
        >
          <div
            style={{
              width: '56px',
              height: '56px',
              borderRadius: '50%',
              backgroundColor: 'rgba(16, 185, 129, 0.15)',
              border: '1px solid rgba(16, 185, 129, 0.4)',
              color: '#10B981',
              fontSize: '1.75rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            ✓
          </div>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-main)', margin: 0 }}>
            No Potholes Detected
          </h3>
          <p style={{ maxWidth: '520px', color: 'var(--text-muted)', fontSize: '0.9rem', margin: 0 }}>
            {message ||
              'The road surface inspection completed and found no defects exceeding the detection threshold. The roadway surface appears clear of major structural damage.'}
          </p>
          <div style={{ marginTop: '0.5rem' }}>
            <button type="button" className="btn btn-secondary btn-sm" onClick={onReset}>
              Upload Another Photo
            </button>
          </div>
        </div>
      ) : (
        /* 2-Column Dark Result Layout: Left Image Evidence, Right Summary Cards */
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '1.75rem' }}>
          {/* LEFT: Annotated Evidence Image */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }} data-testid="annotated-image-section">
            <div
              style={{
                position: 'relative',
                borderRadius: '12px',
                overflow: 'hidden',
                backgroundColor: '#050709',
                border: '1px solid var(--border)',
                boxShadow: 'var(--shadow-md)',
                cursor: pothole.representative_image_url ? 'pointer' : 'default'
              }}
              onClick={() => pothole.representative_image_url && setIsLightboxOpen(true)}
              title="Click to zoom in full resolution"
            >
              {pothole.representative_image_url ? (
                <>
                  <img
                    src={pothole.representative_image_url}
                    alt="Annotated Pothole Evidence"
                    data-testid="annotated-image"
                    style={{
                      width: '100%',
                      maxHeight: '420px',
                      objectFit: 'contain',
                      display: 'block'
                    }}
                  />
                  <div
                    style={{
                      position: 'absolute',
                      bottom: '12px',
                      right: '12px',
                      backgroundColor: 'rgba(11, 13, 15, 0.85)',
                      color: 'var(--brand-accent)',
                      fontSize: '0.75rem',
                      fontWeight: 700,
                      padding: '0.35rem 0.65rem',
                      borderRadius: '6px',
                      backdropFilter: 'blur(4px)',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.4rem',
                      border: '1px solid var(--border)'
                    }}
                  >
                    <span>🔍</span> Click to inspect full image
                  </div>
                </>
              ) : (
                <div style={{ padding: '4rem 1rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                  No representative image generated
                </div>
              )}
            </div>

            {/* Bounding Box Table */}
            {pothole.detections && pothole.detections.length > 0 && (
              <div style={{ backgroundColor: 'var(--surface-elevated)', borderRadius: '10px', border: '1px solid var(--border)', padding: '1rem' }}>
                <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '0.5rem' }}>
                  Observed Defect Locations ({pothole.detections.length})
                </div>
                <div style={{ overflowX: 'auto' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8rem', textAlign: 'left' }}>
                    <thead>
                      <tr style={{ background: 'var(--surface)', borderBottom: '1px solid var(--border)', color: 'var(--text-muted)', textTransform: 'uppercase', fontSize: '0.7rem' }}>
                        <th style={{ padding: '0.45rem 0.5rem' }}>#</th>
                        <th style={{ padding: '0.45rem 0.5rem' }}>Bounding Box [xmin, ymin, xmax, ymax]</th>
                        <th style={{ padding: '0.45rem 0.5rem' }}>Confidence</th>
                        <th style={{ padding: '0.45rem 0.5rem' }}>Area %</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pothole.detections.map((det, idx) => (
                        <tr key={det.id || idx} style={{ borderBottom: '1px solid var(--border)' }}>
                          <td style={{ padding: '0.5rem 0.5rem', fontWeight: 600, color: 'var(--text-main)' }}>{idx + 1}</td>
                          <td style={{ padding: '0.5rem 0.5rem', fontFamily: 'var(--font-mono)', color: 'var(--text-muted)' }}>
                            [{det.box.xmin}, {det.box.ymin}, {det.box.xmax}, {det.box.ymax}]
                          </td>
                          <td style={{ padding: '0.5rem 0.5rem', color: 'var(--brand-accent)', fontWeight: 600 }}>{formatConfidence(det.confidence)}</td>
                          <td style={{ padding: '0.5rem 0.5rem', color: 'var(--text-muted)' }}>{(det.visual_area_ratio * 100).toFixed(2)}%</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>

          {/* RIGHT: Detection Summary Cards & Context */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {/* Top 4 KPI Metrics Grid */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: '0.85rem'
              }}
            >
              {/* Potholes Detected Card */}
              <div
                style={{
                  backgroundColor: 'var(--surface-elevated)',
                  border: '1px solid var(--border)',
                  borderRadius: '10px',
                  padding: '1rem',
                  boxShadow: 'var(--shadow-sm)'
                }}
              >
                <div style={{ fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                  Potholes Detected
                </div>
                <div
                  style={{ fontSize: '1.95rem', fontWeight: 800, color: 'var(--text-main)', margin: '0.2rem 0', fontFamily: 'var(--font-sans)' }}
                  data-testid="pothole-count"
                >
                  {pothole_count}
                </div>
                <div style={{ fontSize: '0.75rem', color: pothole_created ? '#10B981' : 'var(--text-muted)', fontWeight: 600 }}>
                  {pothole_created ? '✓ Logged to Registry' : 'Matched Existing'}
                </div>
              </div>

              {/* Status Card */}
              <div
                style={{
                  backgroundColor: 'var(--surface-elevated)',
                  border: '1px solid var(--border)',
                  borderRadius: '10px',
                  padding: '1rem',
                  boxShadow: 'var(--shadow-sm)'
                }}
              >
                <div style={{ fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                  Remediation Status
                </div>
                <div style={{ margin: '0.4rem 0' }}>
                  <PotholeStatusBadge status={pothole.status} />
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  {pothole.status === 'REPORTED' ? 'Pending Crew Assignment' : pothole.status}
                </div>
              </div>

              {/* Severity Card */}
              <div
                style={{
                  backgroundColor: 'var(--surface-elevated)',
                  border: '1px solid var(--border)',
                  borderRadius: '10px',
                  padding: '1rem',
                  boxShadow: 'var(--shadow-sm)'
                }}
              >
                <div style={{ fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                  Visual Severity
                </div>
                <div style={{ margin: '0.4rem 0' }} data-testid="severity-container">
                  <SeverityBadge severity={pothole.severity_class} score={pothole.severity_score} />
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  Score: {pothole.severity_score?.toFixed(1) || '0.0'} / 100
                </div>
              </div>

              {/* Duplicate Status Card */}
              <div
                style={{
                  backgroundColor: 'var(--surface-elevated)',
                  border: '1px solid var(--border)',
                  borderRadius: '10px',
                  padding: '1rem',
                  boxShadow: 'var(--shadow-sm)'
                }}
              >
                <div style={{ fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                  Duplicate Status
                </div>
                <div style={{ margin: '0.4rem 0' }} data-testid="duplicate-container">
                  <DuplicateBadge isDuplicate={pothole.is_duplicate} duplicateOfId={pothole.duplicate_of_id} />
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  {pothole.is_duplicate ? 'Duplicate Report' : 'Unique Road Defect'}
                </div>
              </div>
            </div>

            {/* Civic Authority Card */}
            <div
              style={{
                backgroundColor: 'var(--surface-elevated)',
                border: '1px solid var(--border)',
                borderRadius: '10px',
                padding: '1.15rem'
              }}
            >
              <div style={{ fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: '0.25rem' }}>
                Responsible Road Authority
              </div>
              <div style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-main)' }} data-testid="authority-name">
                {pothole.authority ? pothole.authority.name : 'Unknown / Unassigned Authority'}
              </div>
              {pothole.authority ? (
                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                  Code: <code style={{ backgroundColor: 'var(--surface)', padding: '0.15rem 0.35rem', borderRadius: '4px', border: '1px solid var(--border)' }}>{pothole.authority.code}</code> &bull; Dept: {pothole.authority.department_type}
                </div>
              ) : (
                <div style={{ fontSize: '0.8rem', color: '#0284C7', marginTop: '0.25rem' }}>
                  Coordinates outside standard municipal polygons &bull; Flagged for jurisdiction triage.
                </div>
              )}
            </div>

            {/* Location & Coordinates Card */}
            <div
              style={{
                backgroundColor: 'var(--surface-elevated)',
                border: '1px solid var(--border)',
                borderRadius: '10px',
                padding: '1.15rem'
              }}
            >
              <div style={{ fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: '0.25rem' }}>
                Defect GPS Location
              </div>
              <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-main)', fontFamily: 'var(--font-mono)' }} data-testid="coordinates-text">
                {formatCoordinates(pothole.latitude, pothole.longitude)}
              </div>
              {pothole.address_text && (
                <div style={{ fontSize: '0.825rem', color: 'var(--text-muted)', marginTop: '0.35rem' }}>
                  📍 {pothole.address_text}
                </div>
              )}
            </div>

            {/* Technical Confidence & Debug Metadata */}
            <div style={{ padding: '0.75rem 1rem', backgroundColor: 'var(--surface)', borderRadius: '8px', border: '1px solid var(--border)', fontSize: '0.75rem', color: 'var(--text-subtle)', display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span>Detection Confidence: <strong style={{ color: 'var(--brand-accent)' }} data-testid="max-confidence">{formatConfidence(pothole.max_confidence)}</strong></span>
                <span>Model: <span style={{ fontFamily: 'var(--font-mono)' }}>custom-yolov8s-pothole</span></span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '0.35rem', borderTop: '1px solid var(--border)', color: 'var(--text-muted)' }} data-testid="debug-diagnostic-panel">
                <span>AI returned: <strong>{pothole_count}</strong> | Backend returned: <strong>{pothole.detections ? pothole.detections.length : 0}</strong> | UI displayed: <strong>{pothole.detections ? pothole.detections.length : 0}</strong></span>
                <span>Conf Thresh: <strong>0.15</strong> | NMS IoU: <strong>0.45</strong></span>
              </div>
            </div>

            {/* Quick Actions Footer */}
            <div style={{ display: 'flex', gap: '0.75rem', marginTop: 'auto' }}>
              <Link
                to={`/potholes/${pothole.id}`}
                className="btn btn-primary"
                style={{ flex: 1 }}
              >
                Inspection Work Order &amp; Audit Trail →
              </Link>
              <Link
                to={`/map?lat=${pothole.latitude}&lng=${pothole.longitude}&id=${pothole.id}`}
                className="btn btn-secondary"
                style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}
              >
                <span>📍</span> View on Map
              </Link>
            </div>
          </div>
        </div>
      )}

      {/* Lightbox Modal for Zooming */}
      {pothole?.representative_image_url && (
        <ImageLightboxModal
          isOpen={isLightboxOpen}
          imageUrl={pothole.representative_image_url}
          title={`Annotated Pothole Evidence (#${pothole.id.slice(0, 8)})`}
          subtitle={`${pothole.severity_class} Severity • ${formatConfidence(pothole.max_confidence)} Confidence`}
          onClose={() => setIsLightboxOpen(false)}
        />
      )}
    </div>
  );
};

