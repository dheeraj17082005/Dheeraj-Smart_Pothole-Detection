import React from 'react';
import { PotholeResponse, PageResponse } from '../../types';
import { SeverityBadge, PotholeStatusBadge, DuplicateBadge } from '../common/Badge';
import { formatConfidence, formatCoordinates, formatDateTime } from '../../utils/formatters';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { useNavigate } from 'react-router-dom';

interface PotholeTableProps {
  data: PageResponse<PotholeResponse> | null;
  loading: boolean;
  onPageChange: (newPage: number) => void;
  onPageSizeChange: (newSize: number) => void;
}

export const PotholeTable: React.FC<PotholeTableProps> = ({
  data,
  loading,
  onPageChange,
  onPageSizeChange
}) => {
  const navigate = useNavigate();

  if (loading) {
    return (
      <div style={{ padding: '3.5rem', textAlign: 'center', backgroundColor: 'var(--surface)', borderRadius: '12px', border: '1px solid var(--border)' }}>
        <LoadingSpinner text="Loading pothole records from spatial database..." />
      </div>
    );
  }

  if (!data || data.content.length === 0) {
    return (
      <div
        data-testid="potholes-empty-state"
        style={{
          padding: '3.5rem 1.5rem',
          textAlign: 'center',
          backgroundColor: 'var(--surface)',
          borderRadius: '12px',
          border: '1px solid var(--border)',
          color: 'var(--text-muted)'
        }}
      >
        <div style={{ fontSize: '2.5rem', marginBottom: '0.5rem' }}>🛣️</div>
        <p style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '0.25rem' }}>No potholes found</p>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Try clearing active filters or inspecting another road segment.</p>
      </div>
    );
  }

  const { content, page, size, totalElements, totalPages } = data;

  return (
    <div style={{ backgroundColor: 'var(--surface)', borderRadius: '12px', border: '1px solid var(--border)', overflow: 'hidden', boxShadow: 'var(--shadow-card)' }}>
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
          <thead>
            <tr style={{ background: 'var(--surface-elevated)', borderBottom: '1px solid var(--border)', color: 'var(--text-muted)', fontSize: '0.725rem', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              <th style={{ padding: '0.85rem 1rem' }}>ID</th>
              <th style={{ padding: '0.85rem 1rem' }}>SEVERITY</th>
              <th style={{ padding: '0.85rem 1rem' }}>CONFIDENCE</th>
              <th style={{ padding: '0.85rem 1rem' }}>AUTHORITY</th>
              <th style={{ padding: '0.85rem 1rem' }}>REPORTED</th>
              <th style={{ padding: '0.85rem 1rem' }}>STATUS</th>
              <th style={{ padding: '0.85rem 1rem' }}>DUPLICATE</th>
              <th style={{ padding: '0.85rem 1rem' }}>LOCATION</th>
              <th style={{ padding: '0.85rem 1rem', textAlign: 'right' }}>ACTION</th>
            </tr>
          </thead>
          <tbody>
            {content.map((pothole) => {
              const authorityText = pothole.authority?.name || (pothole.authority_code === 'UNKNOWN_AUTHORITY' ? 'Authority requires triage' : 'Unassigned');
              const isTriage = !pothole.authority || pothole.authority_code === 'UNKNOWN_AUTHORITY';

              return (
                <tr
                  key={pothole.id}
                  data-testid={`pothole-row-${pothole.id}`}
                  onClick={() => navigate(`/potholes/${pothole.id}`)}
                  style={{
                    borderBottom: '1px solid var(--border)',
                    cursor: 'pointer',
                    transition: 'background-color 0.15s ease'
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--surface-hover)')}
                  onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
                >
                  <td style={{ padding: '0.85rem 1rem', fontFamily: 'var(--font-mono)', fontWeight: 700, color: 'var(--text-primary)' }}>
                    #{pothole.id.slice(0, 8)}
                  </td>
                  <td style={{ padding: '0.85rem 1rem' }}>
                    <SeverityBadge severity={pothole.severity_class} score={pothole.severity_score} />
                  </td>
                  <td style={{ padding: '0.85rem 1rem', color: 'var(--brand-accent)', fontWeight: 600 }}>
                    {formatConfidence(pothole.max_confidence)}
                  </td>
                  <td style={{ padding: '0.85rem 1rem', color: isTriage ? '#0284C7' : 'var(--text-primary)', fontWeight: isTriage ? 700 : 500 }}>
                    {authorityText}
                  </td>
                  <td style={{ padding: '0.85rem 1rem', color: 'var(--text-muted)', fontSize: '0.8125rem' }}>
                    {formatDateTime(pothole.first_detected_at)}
                  </td>
                  <td style={{ padding: '0.85rem 1rem' }}>
                    <PotholeStatusBadge status={pothole.status} />
                  </td>
                  <td style={{ padding: '0.85rem 1rem' }}>
                    <DuplicateBadge isDuplicate={pothole.is_duplicate} duplicateOfId={pothole.duplicate_of_id} />
                  </td>
                  <td style={{ padding: '0.85rem 1rem', fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                    {formatCoordinates(pothole.latitude, pothole.longitude)}
                  </td>
                  <td style={{ padding: '0.85rem 1rem', textAlign: 'right' }}>
                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          navigate(`/map?lat=${pothole.latitude}&lng=${pothole.longitude}&id=${pothole.id}`);
                        }}
                        className="btn btn-secondary btn-sm"
                        title="View defect on GIS Map"
                        style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem', padding: '0.25rem 0.5rem' }}
                      >
                        📍 Map
                      </button>
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          navigate(`/potholes/${pothole.id}`);
                        }}
                        data-testid={`view-btn-${pothole.id}`}
                        className="btn btn-secondary btn-sm"
                      >
                        View
                      </button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Pagination Bar */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '0.85rem 1rem',
          backgroundColor: 'var(--surface-elevated)',
          borderTop: '1px solid var(--border)',
          fontSize: '0.8125rem',
          color: 'var(--text-secondary)',
          flexWrap: 'wrap',
          gap: '0.75rem'
        }}
      >
        <div>
          Showing <strong>{content.length > 0 ? page * size + 1 : 0}</strong> to{' '}
          <strong>{Math.min((page + 1) * size, totalElements)}</strong> of{' '}
          <strong>{totalElements}</strong> defects
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <label htmlFor="page-size-select" style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)' }}>Per page:</label>
            <select
              id="page-size-select"
              value={size}
              onChange={(e) => onPageSizeChange(Number(e.target.value))}
              className="form-input"
              style={{ padding: '0.25rem 0.5rem', width: 'auto', fontSize: '0.8rem' }}
            >
              <option value={10}>10</option>
              <option value={25}>25</option>
              <option value={50}>50</option>
            </select>
          </div>

          <div style={{ display: 'flex', gap: '0.35rem' }}>
            <button
              type="button"
              disabled={page === 0}
              onClick={() => onPageChange(page - 1)}
              data-testid="pagination-prev"
              className="btn btn-secondary btn-sm"
            >
              Previous
            </button>
            <span style={{ display: 'flex', alignItems: 'center', padding: '0 0.5rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              Page {page + 1} of {Math.max(1, totalPages)}
            </span>
            <button
              type="button"
              disabled={page >= totalPages - 1}
              onClick={() => onPageChange(page + 1)}
              data-testid="pagination-next"
              className="btn btn-secondary btn-sm"
            >
              Next
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
