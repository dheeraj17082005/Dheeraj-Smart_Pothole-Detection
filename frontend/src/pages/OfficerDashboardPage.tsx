import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { apiClient, ApiError } from '../services/api';
import { OfficerProfileResponse, PotholeResponse, PotholeStatus, RejectionReason } from '../types';
import { Alert } from '../components/common/Alert';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { PotholeMap } from '../components/map/PotholeMap';
import { StatusUpdateModal } from '../components/potholes/StatusUpdateModal';

export const OfficerDashboardPage: React.FC = () => {
  const { user } = useAuth();

  const [profile, setProfile] = useState<OfficerProfileResponse | null>(null);
  const [reports, setReports] = useState<PotholeResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const [selectedPothole, setSelectedPothole] = useState<PotholeResponse | null>(null);
  const [isUpdateModalOpen, setIsUpdateModalOpen] = useState(false);

  // Rejection Modal state
  const [rejectingPothole, setRejectingPothole] = useState<PotholeResponse | null>(null);
  const [rejectionReason, setRejectionReason] = useState<RejectionReason>('INVALID_REPORT');
  const [rejectionNote, setRejectionNote] = useState<string>('');
  const [rejectSubmitting, setRejectSubmitting] = useState<boolean>(false);

  const loadOfficerData = async () => {
    setLoading(true);
    setError(null);
    try {
      const prof = await apiClient.getOfficerProfile();
      setProfile(prof);

      if (prof.verificationStatus === 'VERIFIED') {
        const reps = await apiClient.getOfficerReports();
        setReports(reps);
      }
    } catch (err: any) {
      if (err instanceof ApiError && err.status === 403) {
        setError('Officer verification pending or access denied.');
      } else {
        setError(err.message || 'Failed to load officer dashboard data.');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadOfficerData();
  }, []);

  const handleVerifySelfDemo = async () => {
    if (!profile) return;
    try {
      await fetch(`/api/v1/officer/admin/verify/${profile.id}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ verificationStatus: 'VERIFIED', verifiedBy: 'DEMO_AUTO_ADMIN' })
      });
      setActionSuccess('Officer status updated to VERIFIED! Loading jurisdiction reports...');
      localStorage.setItem('authVerificationStatus', 'VERIFIED');
      await loadOfficerData();
    } catch (err: any) {
      setError('Verification simulation failed: ' + err.message);
    }
  };

  const handleOpenStatusModal = (pothole: PotholeResponse) => {
    setSelectedPothole(pothole);
    setIsUpdateModalOpen(true);
  };

  const handleAcceptReport = async (pothole: PotholeResponse) => {
    try {
      await apiClient.acceptReport(pothole.id, 'Report accepted by officer');
      setActionSuccess(`Report #${pothole.id.substring(0, 8)} accepted successfully.`);
      await loadOfficerData();
    } catch (err: any) {
      setError(err.message || 'Failed to accept report.');
    }
  };

  const handleOpenRejectModal = (pothole: PotholeResponse) => {
    setRejectingPothole(pothole);
    setRejectionReason('INVALID_REPORT');
    setRejectionNote('');
  };

  const handleConfirmReject = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rejectingPothole) return;
    setRejectSubmitting(true);
    try {
      await apiClient.rejectReport(rejectingPothole.id, {
        reason: rejectionReason,
        notes: rejectionNote
      });
      setActionSuccess(`Report #${rejectingPothole.id.substring(0, 8)} rejected. Reason: ${rejectionReason}`);
      setRejectingPothole(null);
      await loadOfficerData();
    } catch (err: any) {
      setError(err.message || 'Failed to reject report.');
    } finally {
      setRejectSubmitting(false);
    }
  };

  const handleStatusUpdateSubmit = async (newStatus: PotholeStatus, changedBy: string, notes: string) => {
    if (!selectedPothole) return;
    try {
      await apiClient.updateOfficerReportStatus(selectedPothole.id, {
        newStatus,
        changedBy: changedBy || user?.fullName || 'Officer',
        notes
      });
      setActionSuccess(`Report ${selectedPothole.id.substring(0, 8)} status updated to ${newStatus}.`);
      setIsUpdateModalOpen(false);
      await loadOfficerData();
    } catch (err: any) {
      throw err;
    }
  };

  if (loading) {
    return <LoadingSpinner text="Loading Municipal Officer Workspace..." />;
  }

  return (
    <div className="officer-dashboard-page" style={{ padding: '1rem 0' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--text-main)', margin: 0 }}>
            Municipal Officer Operations Dashboard
          </h1>
          <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>
            Spatial Jurisdiction: {profile?.jurisdictionName || 'Central Division'} ({profile?.radiusKm || 10}km Radius)
          </p>
        </div>

        {profile && profile.verificationStatus === 'PENDING_VERIFICATION' && (
          <button
            type="button"
            className="btn btn-primary btn-sm"
            onClick={handleVerifySelfDemo}
            title="Demo shortcut to verify officer account instantly"
          >
            ⚡ Verify Officer Account (Demo)
          </button>
        )}
      </div>

      {actionSuccess && <Alert variant="success" message={actionSuccess} onDismiss={() => setActionSuccess(null)} />}
      {error && <Alert variant="error" message={error} onDismiss={() => setError(null)} />}

      {profile && profile.verificationStatus !== 'VERIFIED' && (
        <Alert
          variant="warning"
          title="Verification Required"
          message={`Officer profile for ${profile.fullName} (${profile.department}, Badge ${profile.officerIdCode}) is currently ${profile.verificationStatus}. Spatial report management is locked until verified.`}
        />
      )}

      {/* Officer Jurisdiction Profile Card */}
      {profile && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', marginBottom: '1.5rem' }}>
          <div style={{ backgroundColor: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '10px', padding: '1rem' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)' }}>DEPARTMENT</span>
            <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-main)', marginTop: '0.2rem' }}>{profile.department}</div>
          </div>
          <div style={{ backgroundColor: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '10px', padding: '1rem' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)' }}>OFFICER ID CODE</span>
            <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--brand-accent)', marginTop: '0.2rem' }}>{profile.officerIdCode}</div>
          </div>
          <div style={{ backgroundColor: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '10px', padding: '1rem' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)' }}>VERIFICATION STATUS</span>
            <div style={{ fontSize: '1.1rem', fontWeight: 700, color: profile.verificationStatus === 'VERIFIED' ? '#10B981' : '#0284C7', marginTop: '0.2rem' }}>
              {profile.verificationStatus}
            </div>
          </div>
          <div style={{ backgroundColor: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '10px', padding: '1rem' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)' }}>REPORTS IN JURISDICTION</span>
            <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-main)', marginTop: '0.2rem' }}>{reports.length} Reports</div>
          </div>
        </div>
      )}

      {/* Jurisdiction Map & Reports Table */}
      {profile?.verificationStatus === 'VERIFIED' && (
        <>
          <div style={{ marginBottom: '2rem' }}>
            <h2 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '0.75rem' }}>
              Spatial Jurisdiction Interactive Map
            </h2>
            <PotholeMap
              initialCenter={[profile.officeLatitude || 28.6200, profile.officeLongitude || 77.2200]}
              initialZoom={13}
              onSelectPothole={(id) => {
                const found = reports.find(r => r.id === id);
                if (found) handleOpenStatusModal(found);
              }}
            />
          </div>

          <div>
            <h2 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '0.75rem' }}>
              Pothole Reports within Spatial Jurisdiction
            </h2>

            {reports.length === 0 ? (
              <div style={{ padding: '2rem', textAlign: 'center', backgroundColor: 'var(--surface)', borderRadius: '10px', border: '1px solid var(--border)', color: 'var(--text-secondary)' }}>
                No active pothole reports found within your assigned jurisdiction radius.
              </div>
            ) : (
              <div style={{ overflowX: 'auto', backgroundColor: 'var(--surface)', borderRadius: '10px', border: '1px solid var(--border)' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--border)', backgroundColor: 'var(--surface-elevated)' }}>
                      <th style={{ padding: '0.75rem 1rem' }}>ID</th>
                      <th style={{ padding: '0.75rem 1rem' }}>Location / Address</th>
                      <th style={{ padding: '0.75rem 1rem' }}>Severity</th>
                      <th style={{ padding: '0.75rem 1rem' }}>Confidence</th>
                      <th style={{ padding: '0.75rem 1rem' }}>Status</th>
                      <th style={{ padding: '0.75rem 1rem' }}>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reports.map((report) => (
                      <tr key={report.id} style={{ borderBottom: '1px solid var(--border)' }}>
                        <td style={{ padding: '0.75rem 1rem', fontFamily: 'monospace', fontWeight: 600 }}>{report.id.substring(0, 8)}</td>
                        <td style={{ padding: '0.75rem 1rem' }}>
                          <div>{report.address_text || `${report.latitude.toFixed(4)}, ${report.longitude.toFixed(4)}`}</div>
                        </td>
                        <td style={{ padding: '0.75rem 1rem' }}>
                          <span style={{
                            padding: '0.2rem 0.5rem',
                            borderRadius: '4px',
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            backgroundColor: report.severity_class === 'HIGH' ? 'rgba(239, 68, 68, 0.2)' : report.severity_class === 'MEDIUM' ? 'rgba(2, 132, 199, 0.2)' : 'rgba(16, 185, 129, 0.2)',
                            color: report.severity_class === 'HIGH' ? '#EF4444' : report.severity_class === 'MEDIUM' ? '#0284C7' : '#10B981'
                          }}>
                            {report.severity_class} ({report.severity_score.toFixed(1)})
                          </span>
                        </td>
                        <td style={{ padding: '0.75rem 1rem' }}>{(report.max_confidence * 100).toFixed(1)}%</td>
                        <td style={{ padding: '0.75rem 1rem' }}>
                          <span style={{
                            padding: '0.2rem 0.5rem',
                            borderRadius: '4px',
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            backgroundColor: report.status === 'RESOLVED' ? 'rgba(16, 185, 129, 0.2)' : report.status === 'REJECTED' ? 'rgba(239, 68, 68, 0.2)' : report.status === 'IN_PROGRESS' ? 'rgba(37, 99, 235, 0.2)' : 'rgba(2, 132, 199, 0.2)',
                            color: report.status === 'RESOLVED' ? '#10B981' : report.status === 'REJECTED' ? '#EF4444' : report.status === 'IN_PROGRESS' ? '#2563EB' : '#0284C7'
                          }}>
                            {report.status}
                          </span>
                        </td>
                        <td style={{ padding: '0.75rem 1rem' }}>
                          <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap' }}>
                            {(report.status === 'SUBMITTED' || report.status === 'PENDING_OFFICER_REVIEW' || report.status === 'REPORTED') && (
                              <>
                                <button
                                  type="button"
                                  className="btn btn-sm"
                                  style={{ backgroundColor: '#10B981', color: '#fff', border: 'none', fontWeight: 600 }}
                                  onClick={() => handleAcceptReport(report)}
                                >
                                  ✓ Accept
                                </button>
                                <button
                                  type="button"
                                  className="btn btn-sm"
                                  style={{ backgroundColor: '#EF4444', color: '#fff', border: 'none', fontWeight: 600 }}
                                  onClick={() => handleOpenRejectModal(report)}
                                >
                                  ✕ Reject
                                </button>
                              </>
                            )}

                            {(report.status === 'ACCEPTED' || report.status === 'ACKNOWLEDGED') && (
                              <>
                                <button
                                  type="button"
                                  className="btn btn-primary btn-sm"
                                  onClick={() => handleOpenStatusModal(report)}
                                >
                                  ▶ Start Work
                                </button>
                                <button
                                  type="button"
                                  className="btn btn-sm"
                                  style={{ backgroundColor: 'transparent', color: '#EF4444', border: '1px solid #EF4444' }}
                                  onClick={() => handleOpenRejectModal(report)}
                                >
                                  ✕ Reject
                                </button>
                              </>
                            )}

                            {report.status === 'IN_PROGRESS' && (
                              <button
                                type="button"
                                className="btn btn-sm"
                                style={{ backgroundColor: '#10B981', color: '#fff', border: 'none', fontWeight: 600 }}
                                onClick={() => handleOpenStatusModal(report)}
                              >
                                ✓ Mark Resolved
                              </button>
                            )}

                            {(report.status === 'RESOLVED' || report.status === 'REJECTED') && (
                              <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                                {report.status === 'REJECTED' ? `Rejected (${report.rejection_reason || 'Invalid'})` : 'Completed'}
                              </span>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}

      {/* Status Transition Modal */}
      {selectedPothole && (
        <StatusUpdateModal
          isOpen={isUpdateModalOpen}
          potholeId={selectedPothole.id}
          currentStatus={selectedPothole.status}
          onClose={() => setIsUpdateModalOpen(false)}
          onSubmit={handleStatusUpdateSubmit}
        />
      )}

      {/* Rejection Modal */}
      {rejectingPothole && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            backdropFilter: 'blur(4px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 2000,
            padding: '1rem'
          }}
        >
          <div
            style={{
              backgroundColor: 'var(--surface)',
              borderRadius: '12px',
              width: '100%',
              maxWidth: '480px',
              padding: '1.75rem',
              border: '1px solid var(--border)',
              boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.7)'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <h3 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 800, color: 'var(--text-main)' }}>
                Reject Pothole Report #{rejectingPothole.id.substring(0, 8)}
              </h3>
              <button
                type="button"
                onClick={() => setRejectingPothole(null)}
                style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', fontSize: '1.25rem', cursor: 'pointer' }}
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleConfirmReject} style={{ display: 'flex', flexDirection: 'column', gap: '1.15rem' }}>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" style={{ fontWeight: 700 }}>
                  Rejection Reason <span style={{ color: '#EF4444' }}>*</span>
                </label>
                <select
                  className="form-input"
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value as RejectionReason)}
                >
                  <option value="NO_POTHOLE">No Pothole Present in Media</option>
                  <option value="DUPLICATE">Duplicate Submission</option>
                  <option value="WRONG_LOCATION">Incorrect Geographic Location</option>
                  <option value="INSUFFICIENT_EVIDENCE">Insufficient Evidence / Blurry Media</option>
                  <option value="OUTSIDE_JURISDICTION">Outside Municipal Jurisdiction</option>
                  <option value="INVALID_REPORT">Invalid / Test Submission</option>
                </select>
              </div>

              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" style={{ fontWeight: 700 }}>
                  Officer Rejection Note (Optional)
                </label>
                <textarea
                  className="form-input"
                  rows={3}
                  value={rejectionNote}
                  onChange={(e) => setRejectionNote(e.target.value)}
                  placeholder="Provide additional details or official reference note..."
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setRejectingPothole(null)}
                  disabled={rejectSubmitting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-sm"
                  style={{ backgroundColor: '#EF4444', color: '#fff', border: 'none', padding: '0.6rem 1.2rem', borderRadius: '6px', fontWeight: 700 }}
                  disabled={rejectSubmitting}
                >
                  {rejectSubmitting ? 'Rejecting...' : 'Confirm Rejection'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
