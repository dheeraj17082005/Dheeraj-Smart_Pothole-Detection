import React from 'react';
import { PotholeStatusHistoryResponse, PotholeStatus } from '../../types';
import { PotholeStatusBadge } from '../common/Badge';
import { formatDateTime } from '../../utils/formatters';

interface StatusTimelineProps {
  history: PotholeStatusHistoryResponse[];
  loading?: boolean;
}

const ORDERED_STEPS: PotholeStatus[] = ['REPORTED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'RESOLVED'];

export const StatusTimeline: React.FC<StatusTimelineProps> = ({ history, loading = false }) => {
  if (loading) {
    return <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>Loading audit timeline...</div>;
  }

  // Determine latest status reached
  const latestStatus = history && history.length > 0 ? history[history.length - 1].new_status : 'REPORTED';
  const latestIndex = ORDERED_STEPS.indexOf(latestStatus);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
      {/* Visual Step Progression Bar */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(4, 1fr)',
          gap: '0.5rem',
          backgroundColor: 'var(--surface-elevated)',
          padding: '0.85rem',
          borderRadius: '8px',
          border: '1px solid var(--border)'
        }}
      >
        {ORDERED_STEPS.map((step, idx) => {
          const isPassed = idx <= latestIndex;
          const isCurrent = idx === latestIndex;
          return (
            <div
              key={step}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                textAlign: 'center',
                gap: '0.25rem'
              }}
            >
              <div
                style={{
                  width: '24px',
                  height: '24px',
                  borderRadius: '50%',
                  backgroundColor: isCurrent ? 'var(--brand-accent)' : isPassed ? '#10B981' : 'var(--surface)',
                  color: isCurrent ? 'var(--text-inverse)' : isPassed ? '#ffffff' : 'var(--text-muted)',
                  fontSize: '0.7rem',
                  fontWeight: 800,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  boxShadow: isCurrent ? 'var(--shadow-glow)' : 'none',
                  border: isCurrent || isPassed ? 'none' : '1px solid var(--border)'
                }}
              >
                {isPassed && !isCurrent ? '✓' : idx + 1}
              </div>
              <span
                style={{
                  fontSize: '0.68rem',
                  fontWeight: 700,
                  color: isCurrent ? 'var(--brand-accent)' : isPassed ? 'var(--text-primary)' : 'var(--text-muted)',
                  textTransform: 'uppercase',
                  letterSpacing: '0.04em'
                }}
              >
                {step.replace('_', ' ')}
              </span>
            </div>
          );
        })}
      </div>

      {/* Audit Log Chronological Entries */}
      {!history || history.length === 0 ? (
        <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontStyle: 'italic', padding: '0.5rem 0' }}>
          No status transitions recorded yet. Initial registration logged as REPORTED.
        </div>
      ) : (
        <div
          data-testid="status-timeline"
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem',
            position: 'relative',
            paddingLeft: '1.5rem',
            marginTop: '0.5rem'
          }}
        >
          {/* Vertical timeline line */}
          <div
            style={{
              position: 'absolute',
              top: '8px',
              bottom: '8px',
              left: '7px',
              width: '2px',
              backgroundColor: 'var(--border)'
            }}
          />

          {history.map((item, index) => (
            <div
              key={item.id || index}
              data-testid={`timeline-item-${index}`}
              style={{ position: 'relative', display: 'flex', flexDirection: 'column', gap: '0.35rem' }}
            >
              {/* Bullet dot */}
              <div
                style={{
                  position: 'absolute',
                  top: '4px',
                  left: '-1.5rem',
                  width: '12px',
                  height: '12px',
                  borderRadius: '50%',
                  backgroundColor: 'var(--brand-accent)',
                  border: '2px solid var(--surface)',
                  boxShadow: '0 0 6px var(--brand-accent-glow)'
                }}
              />

              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                {item.previous_status && (
                  <>
                    <PotholeStatusBadge status={item.previous_status} />
                    <span style={{ color: 'var(--text-muted)' }}>→</span>
                  </>
                )}
                <PotholeStatusBadge status={item.new_status} />
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginLeft: 'auto', fontFamily: 'var(--font-mono)' }}>
                  {formatDateTime(item.changed_at)}
                </span>
              </div>

              <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                <strong style={{ color: 'var(--text-primary)' }}>Changed by:</strong> {item.changed_by || 'SYSTEM'}
              </div>

              {item.notes && (
                <div
                  style={{
                    fontSize: '0.8125rem',
                    color: 'var(--text-primary)',
                    backgroundColor: 'var(--surface-elevated)',
                    padding: '0.5rem 0.75rem',
                    borderRadius: '6px',
                    border: '1px solid var(--border)',
                    marginTop: '0.2rem'
                  }}
                >
                  "{item.notes}"
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
