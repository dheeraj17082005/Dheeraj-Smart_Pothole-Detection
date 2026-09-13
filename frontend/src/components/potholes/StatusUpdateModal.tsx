import React, { useState, useEffect } from 'react';
import { PotholeStatus } from '../../types';
import { PotholeStatusBadge } from '../common/Badge';
import { useAuth } from '../../context/AuthContext';

interface StatusUpdateModalProps {
  currentStatus: PotholeStatus;
  potholeId: string;
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (newStatus: PotholeStatus, changedBy: string, notes: string) => Promise<void>;
}

const getValidNextStatuses = (status: PotholeStatus): PotholeStatus[] => {
  if (status === 'SUBMITTED' || status === 'PENDING_OFFICER_REVIEW' || status === 'REPORTED') {
    return ['ACCEPTED', 'REJECTED'];
  } else if (status === 'ACCEPTED' || status === 'ACKNOWLEDGED') {
    return ['IN_PROGRESS', 'REJECTED'];
  } else if (status === 'IN_PROGRESS') {
    return ['RESOLVED'];
  }
  return [];
};

export const StatusUpdateModal: React.FC<StatusUpdateModalProps> = ({
  currentStatus,
  potholeId,
  isOpen,
  onClose,
  onSubmit
}) => {
  const { user } = useAuth();
  const validTargets = getValidNextStatuses(currentStatus);
  const defaultNext = validTargets.length > 0 ? validTargets[0] : null;

  const [selectedStatus, setSelectedStatus] = useState<PotholeStatus>(defaultNext || 'IN_PROGRESS');
  const [changedBy, setChangedBy] = useState<string>(user?.fullName || 'Municipal Officer');
  const [notes, setNotes] = useState<string>('');
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      if (defaultNext) {
        setSelectedStatus(defaultNext);
      }
      setChangedBy(user?.fullName || 'Municipal Officer');
      setNotes('');
      setError(null);
    }
  }, [isOpen, potholeId, currentStatus, user?.fullName]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedStatus) return;

    setSubmitting(true);
    setError(null);
    try {
      await onSubmit(selectedStatus, changedBy, notes);
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to update pothole status.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      data-testid="status-update-modal"
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
          maxWidth: '500px',
          padding: '1.75rem',
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.7)',
          border: '1px solid var(--border)'
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
          <h3 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 800, color: 'var(--text-primary)' }}>
            Transition Defect Status
          </h3>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close modal"
            style={{
              background: 'transparent',
              border: 'none',
              fontSize: '1.25rem',
              cursor: 'pointer',
              color: 'var(--text-muted)'
            }}
          >
            ✕
          </button>
        </div>

        {currentStatus === 'RESOLVED' ? (
          <div>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
              This pothole is already marked as <strong>RESOLVED</strong>. No further forward status transitions are permitted.
            </p>
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button
                type="button"
                onClick={onClose}
                className="btn btn-secondary"
              >
                Close
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.15rem' }}>
            {error && (
              <div
                data-testid="modal-error-alert"
                style={{
                  backgroundColor: 'rgba(239, 68, 68, 0.15)',
                  color: '#FCA5A5',
                  padding: '0.65rem 0.85rem',
                  borderRadius: '6px',
                  fontSize: '0.8125rem',
                  border: '1px solid rgba(239, 68, 68, 0.4)'
                }}
              >
                {error}
              </div>
            )}

            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
                fontSize: '0.875rem',
                backgroundColor: 'var(--surface-elevated)',
                padding: '0.75rem',
                borderRadius: '8px',
                border: '1px solid var(--border)'
              }}
            >
              <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem', fontWeight: 700 }}>Current:</span>
              <PotholeStatusBadge status={currentStatus} />
              <span style={{ color: 'var(--border)' }}>→</span>
              <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem', fontWeight: 700 }}>Target:</span>
              {selectedStatus && <PotholeStatusBadge status={selectedStatus} />}
            </div>

            <div className="form-group" style={{ marginBottom: 0 }}>
              <label htmlFor="modal-status-select" className="form-label">
                Target Status <span style={{ color: '#EF4444' }}>*</span>
              </label>
              <select
                id="modal-status-select"
                data-testid="modal-status-select"
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value as PotholeStatus)}
                className="form-input"
              >
                {validTargets.map((st) => (
                  <option key={st} value={st}>
                    {st.replace(/_/g, ' ')} {st === defaultNext ? '(Recommended)' : ''}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group" style={{ marginBottom: 0 }}>
              <label htmlFor="modal-changed-by" className="form-label">
                Operator / Inspector Name
              </label>
              <input
                type="text"
                id="modal-changed-by"
                data-testid="modal-changed-by-input"
                value={changedBy}
                onChange={(e) => setChangedBy(e.target.value)}
                placeholder="e.g., Municipal Officer"
                className="form-input"
              />
            </div>

            <div className="form-group" style={{ marginBottom: 0 }}>
              <label htmlFor="modal-notes" className="form-label">
                Audit Notes / Work Order
              </label>
              <textarea
                id="modal-notes"
                data-testid="modal-notes-input"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                rows={3}
                placeholder="Add operational notes or maintenance work order reference..."
                className="form-input"
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
              <button
                type="button"
                onClick={onClose}
                disabled={submitting}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={submitting}
                data-testid="modal-confirm-button"
                className="btn btn-primary"
              >
                {submitting ? 'Updating...' : 'Confirm Transition'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
