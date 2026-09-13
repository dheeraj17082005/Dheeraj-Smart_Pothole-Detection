import React from 'react';

interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description?: string;
  actionText?: string;
  onAction?: () => void;
  testId?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon,
  title,
  description,
  actionText,
  onAction,
  testId
}) => {
  return (
    <div
      data-testid={testId}
      style={{
        padding: '3.5rem 1.5rem',
        textAlign: 'center',
        backgroundColor: 'var(--surface)',
        borderRadius: '12px',
        border: '1px solid var(--border)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '0.75rem'
      }}
    >
      {icon ? (
        <div style={{ fontSize: '2.5rem', color: 'var(--text-muted)' }}>{icon}</div>
      ) : (
        <div
          style={{
            width: '48px',
            height: '48px',
            borderRadius: '50%',
            backgroundColor: 'var(--surface-elevated)',
            border: '1px solid var(--border)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'var(--text-secondary)'
          }}
        >
          <svg
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
        </div>
      )}

      <div style={{ maxWidth: '440px' }}>
        <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-primary)', margin: '0 0 0.25rem 0' }}>
          {title}
        </h3>
        {description && (
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', margin: 0 }}>
            {description}
          </p>
        )}
      </div>

      {actionText && onAction && (
        <button
          type="button"
          onClick={onAction}
          className="btn btn-secondary btn-sm"
          style={{ marginTop: '0.5rem' }}
        >
          {actionText}
        </button>
      )}
    </div>
  );
};
