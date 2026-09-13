import React from 'react';

interface LoadingSpinnerProps {
  size?: number;
  text?: string;
  label?: string;
  inline?: boolean;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  size = 28,
  text,
  label,
  inline = false
}) => {
  const displayText = label || text;

  return (
    <div
      role="status"
      style={{
        display: inline ? 'inline-flex' : 'flex',
        flexDirection: inline ? 'row' : 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '0.75rem',
        color: 'var(--text-secondary)',
        padding: inline ? '0' : '1.5rem 0'
      }}
    >
      <div
        style={{
          width: `${size}px`,
          height: `${size}px`,
          border: '2.5px solid var(--border)',
          borderTopColor: 'var(--brand-accent)',
          borderRadius: '50%',
          animation: 'spin 0.75s linear infinite',
          flexShrink: 0
        }}
      />
      {displayText && (
        <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
          {displayText}
        </span>
      )}
    </div>
  );
};
