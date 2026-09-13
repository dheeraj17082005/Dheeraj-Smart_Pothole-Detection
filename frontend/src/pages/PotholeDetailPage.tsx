import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { PotholeDetailResponse, PotholeStatusHistoryResponse, PotholeStatus } from '../types';
import { apiClient } from '../services/api';
import { SeverityBadge, PotholeStatusBadge, DuplicateBadge } from '../components/common/Badge';
import { formatCoordinates, formatConfidence, formatDateTime } from '../utils/formatters';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { Alert } from '../components/common/Alert';
import { StatusTimeline } from '../components/potholes/StatusTimeline';
import { StatusUpdateModal } from '../components/potholes/StatusUpdateModal';
import { ImageLightboxModal } from '../components/common/ImageLightboxModal';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';

export const PotholeDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const isOfficer = user?.role === 'ROLE_OFFICER';

  const [pothole, setPothole] = useState<PotholeDetailResponse | null>(null);
  const [history, setHistory] = useState<PotholeStatusHistoryResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [isUpdateModalOpen, setIsUpdateModalOpen] = useState<boolean>(false);
  const [isLightboxOpen, setIsLightboxOpen] = useState<boolean>(false);
  const [actionSuccessMessage, setActionSuccessMessage] = useState<string | null>(null);

  const fetchDetails = useCallback(async (potholeId: string) => {
    setLoading(true);
    setError(null);
    try {
      const [detailData, historyData] = await Promise.all([
        apiClient.getPothole(potholeId),
        apiClient.getPotholeHistory(potholeId)
      ]);
      setPothole(detailData);
      setHistory(historyData);
    } catch (err: any) {
      setError(err.message || 'Failed to load pothole details.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (id) {
      fetchDetails(id);
    }
  }, [id, fetchDetails]);

  const handleStatusUpdate = async (newStatus: PotholeStatus, changedBy: string, notes: string) => {
    if (!id) return;
    await apiClient.updateOfficerReportStatus(id, { newStatus, changedBy: changedBy || user?.fullName || 'Officer', notes });
    await fetchDetails(id);
    setActionSuccessMessage(`Successfully updated status to ${newStatus.replace('_', ' ')}.`);
    setTimeout(() => setActionSuccessMessage(null), 5000);
  };

  if (loading) {
    return (
      <div style={{ maxWidth: '1000px', margin: '4rem auto', textAlign: 'center' }}>
        <LoadingSpinner text="Loading pothole details and audit timeline..." />
      </div>
    );
  }

  if (error || !pothole) {
    return (
      <div style={{ maxWidth: '1000px', margin: '2rem auto' }}>
        <Alert variant="error" title="Pothole Not Found">
          {error || 'The requested pothole could not be located in the spatial database.'}
        </Alert>
        <div style={{ marginTop: '1rem' }}>
          <button
            type="button"
            onClick={() => navigate('/potholes')}
            className="btn btn-primary"
          >
            ← Back to Potholes List
          </button>
        </div>
      </div>
    );
  }

  const isUnknownAuthority = !pothole.authority || pothole.authorityCode === 'UNKNOWN_AUTHORITY';
  const hasReport = !!pothole.report;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
      {/* Top Breadcrumb & Status Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '0.25rem', fontFamily: 'var(--font-mono)' }}>
            <Link to="/potholes" style={{ color: 'var(--brand-accent)', textDecoration: 'none' }}>Defect Registry</Link> / #{pothole.id.slice(0, 8)}
          </div>
          <h1 style={{ fontSize: '1.95rem', fontWeight: 800, color: 'var(--text-primary)', margin: 0, letterSpacing: '-0.025em' }}>
            Pothole Inspection Report #{pothole.id.slice(0, 8)}
          </h1>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <PotholeStatusBadge status={pothole.status} />
          {isOfficer && pothole.status !== 'RESOLVED' && pothole.status !== 'REJECTED' && (
            <button
              type="button"
              onClick={() => setIsUpdateModalOpen(true)}
              data-testid="open-status-modal-button"
              className="btn btn-primary btn-sm"
            >
              Update Status
            </button>
          )}
        </div>
      </div>

      {/* Rejection Details Alert */}
      {pothole.status === 'REJECTED' && (
        <Alert variant="error" title="Report Rejected by Municipal Authority">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem', marginTop: '0.25rem' }}>
            <div><strong>Rejection Reason:</strong> {pothole.rejectionReason || 'INVALID_REPORT'}</div>
            {pothole.rejectionNote && <div><strong>Officer Note:</strong> {pothole.rejectionNote}</div>}
            {pothole.rejectedAt && <div><strong>Rejected At:</strong> {formatDateTime(pothole.rejectedAt)}</div>}
          </div>
        </Alert>
      )}

      {/* Success Notification Banner */}
      {actionSuccessMessage && (
        <Alert variant="success" title="Status Updated">
          {actionSuccessMessage}
        </Alert>
      )}

      {/* Duplicate Sighting Alert */}
      {pothole.isDuplicate && (
        <Alert variant="warning" title="Duplicate Submission">
          This defect has been identified as a duplicate submission of existing parent defect{' '}
          {pothole.duplicateOfId ? (
            <Link to={`/potholes/${pothole.duplicateOfId}`} style={{ fontWeight: 700, color: '#0284C7' }}>
              #{pothole.duplicateOfId.slice(0, 8)}
            </Link>
          ) : (
            'in the same area'
          )}
          . Automated civic dispatches are suppressed to prevent duplicate municipal tickets.
        </Alert>
      )}

      {/* Unknown Authority / Triage Banner */}
      {isUnknownAuthority && (
        <Alert variant="warning" title="Authority Requires Triage">
          Defect coordinates lie outside all recognized municipal and state road network boundaries.
          Civic jurisdiction is unassigned and requires administrative triage before dispatch.
        </Alert>
      )}

      {/* 2-Column Main Layout: Evidence & Metrics Left, Location & Details Right */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '1.75rem' }}>
        {/* Left Column: Visual Evidence & Detection Metrics */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* Visual Evidence Card */}
          <div className="card" style={{ padding: '1.5rem' }}>
            <div className="card-header">
              <h2 className="card-title">Visual Evidence</h2>
              {pothole.evidence?.representativeImageUrl && (
                <button
                  type="button"
                  onClick={() => setIsLightboxOpen(true)}
                  style={{
                    backgroundColor: 'transparent',
                    border: 'none',
                    color: 'var(--brand-accent)',
                    fontSize: '0.8125rem',
                    fontWeight: 700,
                    cursor: 'pointer'
                  }}
                >
                  🔍 Inspect Zoom
                </button>
              )}
            </div>

            {pothole.evidence?.representativeImageUrl ? (
              <div>
                <div
                  style={{
                    position: 'relative',
                    borderRadius: '10px',
                    overflow: 'hidden',
                    backgroundColor: '#050709',
                    border: '1px solid var(--border)',
                    cursor: 'pointer'
                  }}
                  onClick={() => setIsLightboxOpen(true)}
                  title="Click to zoom in full resolution"
                >
                  <img
                    src={pothole.evidence.representativeImageUrl}
                    alt="AI Annotated Pothole"
                    style={{ width: '100%', maxHeight: '420px', objectFit: 'contain', display: 'block' }}
                  />
                  <div
                    style={{
                      position: 'absolute',
                      bottom: '10px',
                      right: '10px',
                      backgroundColor: 'rgba(11, 13, 15, 0.85)',
                      color: 'var(--brand-accent)',
                      fontSize: '0.725rem',
                      fontWeight: 700,
                      padding: '0.25rem 0.5rem',
                      borderRadius: '4px',
                      backdropFilter: 'blur(3px)',
                      border: '1px solid var(--border)'
                    }}
                  >
                    Click to zoom
                  </div>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.65rem' }}>
                  <span>Frame Key: {pothole.evidence.representativeKey || '—'}</span>
                  <a
                    href={pothole.evidence.representativeImageUrl}
                    target="_blank"
                    rel="noreferrer"
                    style={{ color: 'var(--brand-accent)', textDecoration: 'none', fontWeight: 600 }}
                  >
                    Open Full Image ↗
                  </a>
                </div>
              </div>
            ) : (
              <div style={{ padding: '3.5rem 1rem', textAlign: 'center', backgroundColor: 'var(--surface-elevated)', borderRadius: '8px', color: 'var(--text-muted)', border: '1px dashed var(--border)' }}>
                No representative annotated image available
              </div>
            )}

            {pothole.evidence?.rawMediaUrl && (
              <div style={{ marginTop: '1rem', paddingTop: '1rem', borderTop: '1px solid var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.8125rem' }}>
                <span style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Original Source Media:</span>
                <a
                  href={pothole.evidence.rawMediaUrl}
                  target="_blank"
                  rel="noreferrer"
                  style={{ color: 'var(--brand-accent)', textDecoration: 'none', fontWeight: 600 }}
                >
                  Download Raw {pothole.evidence.mediaType || 'Media'} ↗
                </a>
              </div>
            )}
          </div>

          {/* Defect Metrics Card */}
          <div className="card" style={{ padding: '1.5rem' }}>
            <h2 className="card-title" style={{ marginBottom: '1rem' }}>
              Defect Metrics & Surface Scoring
            </h2>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div>
                <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700, letterSpacing: '0.05em' }}>Visual Severity</div>
                <div style={{ marginTop: '0.35rem' }}>
                  <SeverityBadge severity={pothole.severityClass} score={pothole.severityScore} />
                </div>
              </div>

              <div>
                <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700, letterSpacing: '0.05em' }}>Detection Confidence</div>
                <div style={{ fontSize: '1.35rem', fontWeight: 800, color: 'var(--brand-accent)', marginTop: '0.2rem' }}>
                  {formatConfidence(pothole.maxConfidence)}
                </div>
              </div>

              <div>
                <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700, letterSpacing: '0.05em' }}>Detected Timestamp</div>
                <div style={{ fontSize: '0.875rem', color: 'var(--text-primary)', marginTop: '0.25rem' }}>
                  {formatDateTime(pothole.firstDetectedAt)}
                </div>
              </div>

              <div>
                <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700, letterSpacing: '0.05em' }}>Source Ingestion</div>
                <div style={{ fontSize: '0.875rem', color: 'var(--text-primary)', marginTop: '0.25rem' }}>
                  {pothole.evidence?.mediaType || 'IMAGE'}
                </div>
              </div>
            </div>

            <div style={{ marginTop: '1.25rem', padding: '0.75rem 1rem', backgroundColor: 'var(--surface-elevated)', borderRadius: '8px', border: '1px solid var(--border)', fontSize: '0.775rem', color: 'var(--text-muted)' }}>
              <strong>Heuristic Note:</strong> Severity score ({pothole.severityScore.toFixed(1)}) represents the 2D visual surface footprint calculation from camera pixels and does not measure subsurface structural integrity or physical depth.
            </div>
          </div>
        </div>

        {/* Right Column: Location, Authority, Reporting & Status History */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* Location & Mini Map Preview */}
          <div className="card" style={{ padding: '1.5rem' }}>
            <h2 className="card-title" style={{ marginBottom: '0.5rem' }}>
              Geographic Location
            </h2>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.85rem' }}>
              <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, color: 'var(--text-primary)' }}>{formatCoordinates(pothole.latitude, pothole.longitude)}</span>
              {pothole.addressText && (
                <div style={{ fontSize: '0.825rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                  <span aria-hidden="true">📍 </span>
                  <span>{pothole.addressText}</span>
                </div>
              )}
            </div>

            {/* Embedded Mini Leaflet Map Preview */}
            <div style={{ width: '100%', height: '220px', borderRadius: '8px', overflow: 'hidden', border: '1px solid var(--border)', backgroundColor: '#0B0D0F' }}>
              <MapContainer
                center={[pothole.latitude, pothole.longitude]}
                zoom={15}
                style={{ width: '100%', height: '100%', backgroundColor: '#0B0D0F' }}
                scrollWheelZoom={false}
              >
                <TileLayer
                  attribution='&copy; <a href="https://www.openstreetmap.org/copyright" style="color:#737B82">OpenStreetMap</a>'
                  url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />
                <Marker position={[pothole.latitude, pothole.longitude]}>
                  <Popup>#{pothole.id.slice(0, 8)}</Popup>
                </Marker>
              </MapContainer>
            </div>

            <button
              type="button"
              onClick={() => navigate(`/map?lat=${pothole.latitude}&lng=${pothole.longitude}&id=${pothole.id}`)}
              className="btn btn-secondary btn-sm"
              style={{ marginTop: '0.85rem', width: '100%', display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '0.35rem' }}
            >
              📍 Open In Full GIS Map
            </button>
          </div>

          {/* Civic Authority Card */}
          <div className="card" style={{ padding: '1.5rem' }}>
            <h2 className="card-title" style={{ marginBottom: '0.75rem' }}>
              Assigned Civic Authority
            </h2>

            {isUnknownAuthority ? (
              <div style={{ padding: '0.85rem', backgroundColor: 'rgba(245, 158, 11, 0.15)', border: '1px solid rgba(245, 158, 11, 0.4)', borderRadius: '8px', color: '#FDE68A', fontSize: '0.875rem' }}>
                <strong>Authority requires triage:</strong> No matching municipal or highway polygon covers this location.
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
                <div style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                  {pothole.authority?.name}
                </div>
                <div style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
                  <strong>Code:</strong> <code>{pothole.authority?.code}</code> &nbsp;|&nbsp; <strong>Type:</strong> {pothole.authority?.department_type}
                </div>
              </div>
            )}
          </div>

          {/* Automated Civic Dispatch Card */}
          <div className="card" style={{ padding: '1.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
              <h2 className="card-title" style={{ margin: 0 }}>
                Automated Civic Dispatch
              </h2>
              <span style={{ fontSize: '0.7rem', padding: '0.2rem 0.5rem', backgroundColor: 'var(--surface-elevated)', color: 'var(--text-secondary)', borderRadius: '4px', fontWeight: 600, border: '1px solid var(--border)' }}>
                Simulated Ticket
              </span>
            </div>

            {hasReport && pothole.report ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem', fontSize: '0.875rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Dispatch Status:</span>
                  <span style={{ fontWeight: 700, color: pothole.report.status === 'DISPATCHED' ? '#10B981' : '#EF4444' }}>
                    {pothole.report.status}
                  </span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--text-muted)' }}>External Ticket Reference:</span>
                  <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, color: 'var(--text-primary)' }}>
                    {pothole.report.external_reference || 'N/A'}
                  </span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Idempotency Key:</span>
                  <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                    {pothole.report.idempotency_key}
                  </span>
                </div>
                {pothole.report.attempts && pothole.report.attempts.length > 0 && (
                  <div style={{ marginTop: '0.5rem', paddingTop: '0.5rem', borderTop: '1px solid var(--border)' }}>
                    <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-secondary)' }}>Dispatch Attempts ({pothole.report.attempts.length}):</span>
                    {pothole.report.attempts.map((att) => (
                      <div key={att.id} style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                        Attempt #{att.attempt_number} ({att.channel}) - Status: {att.status} ({formatDateTime(att.attempt_timestamp)})
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ) : (
              <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                No dispatch ticket issued {pothole.isDuplicate ? '(Suppressed for duplicate submission)' : isUnknownAuthority ? '(Pending jurisdiction triage)' : ''}.
              </div>
            )}
          </div>

          {/* Status Transition History Timeline Card */}
          <div className="card" style={{ padding: '1.5rem' }}>
            <h2 className="card-title" style={{ marginBottom: '1rem' }}>
              Status Audit Trail
            </h2>
            <StatusTimeline history={history} />
          </div>
        </div>
      </div>

      {/* Status Update Confirmation Modal */}
      <StatusUpdateModal
        currentStatus={pothole.status}
        potholeId={pothole.id}
        isOpen={isUpdateModalOpen}
        onClose={() => setIsUpdateModalOpen(false)}
        onSubmit={handleStatusUpdate}
      />

      {/* Lightbox Modal */}
      {pothole.evidence?.representativeImageUrl && (
        <ImageLightboxModal
          isOpen={isLightboxOpen}
          imageUrl={pothole.evidence.representativeImageUrl}
          title={`Annotated Pothole Evidence (#${pothole.id.slice(0, 8)})`}
          subtitle={`${pothole.severityClass} Severity • ${formatConfidence(pothole.maxConfidence)} Confidence`}
          onClose={() => setIsLightboxOpen(false)}
        />
      )}
    </div>
  );
};
