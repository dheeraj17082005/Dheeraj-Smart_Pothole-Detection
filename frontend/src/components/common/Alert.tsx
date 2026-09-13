import React from 'react';

export type AlertVariant = 'error' | 'warning' | 'success' | 'info';

interface AlertProps {
  variant?: AlertVariant;
  title?: string;
  message?: string;
  children?: React.ReactNode;
  onDismiss?: () => void;
}

export const Alert: React.FC<AlertProps> = ({
  variant = 'info',
  title,
  message,
  children,
  onDismiss
}) => {
  let bg = 'rgba(2, 132, 199, 0.12)';
  let border = 'rgba(2, 132, 199, 0.35)';
  let color = '#7dd3fc';
  let icon = 'ℹ️';

  if (variant === 'error') {
    bg = 'rgba(239, 68, 68, 0.15)';
    border = 'rgba(239, 68, 68, 0.4)';
    color = '#fca5a5';
    icon = '⚠️';
  } else if (variant === 'warning') {
    bg = 'rgba(245, 158, 11, 0.15)';
    border = 'rgba(245, 158, 11, 0.4)';
    color = '#fde68a';
    icon = '⚠️';
  } else if (variant === 'success') {
    bg = 'rgba(16, 185, 129, 0.15)';
    border = 'rgba(16, 185, 129, 0.4)';
    color = '#86efac';
    icon = '✓';
  }

  return (
    <div
      role="alert"
      style={{
        backgroundColor: bg,
        border: `1px solid ${border}`,
        borderRadius: '10px',
        padding: '0.85rem 1.15rem',
        marginBottom: '1.25rem',
        display: 'flex',
        alignItems: 'flex-start',
        gap: '0.75rem',
        fontSize: '0.875rem',
        color
      }}
    >
      <span aria-hidden="true" style={{ fontSize: '1rem', flexShrink: 0, marginTop: '1px' }}>
        {icon}
      </span>

      <div style={{ flex: 1 }}>
        {title && (
          <div style={{ fontWeight: 700, color: '#ffffff', marginBottom: '0.2rem' }}>
            {title}
          </div>
        )}
        <div>{message || children}</div>
      </div>

      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          aria-label="Dismiss alert"
          style={{
            background: 'transparent',
            border: 'none',
            color: 'inherit',
            opacity: 0.75,
            cursor: 'pointer',
            fontSize: '1rem',
            lineHeight: 1,
            padding: '0.15rem'
          }}
        >
          ✕
        </button>
      )}
    </div>
  );
};
