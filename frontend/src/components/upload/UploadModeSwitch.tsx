import React from 'react';

export type UploadMode = 'IMAGE' | 'VIDEO';

interface UploadModeSwitchProps {
  mode: UploadMode;
  onChange: (mode: UploadMode) => void;
  disabled?: boolean;
}

export const UploadModeSwitch: React.FC<UploadModeSwitchProps> = ({
  mode,
  onChange,
  disabled = false
}) => {
  return (
    <div
      style={{
        display: 'inline-flex',
        backgroundColor: 'var(--surface)',
        border: '1px solid var(--border)',
        padding: '0.25rem',
        borderRadius: 'var(--radius-md)',
        marginBottom: '1.5rem'
      }}
      role="tablist"
      aria-label="Upload Mode Selection"
    >
      <button
        type="button"
        role="tab"
        aria-selected={mode === 'IMAGE'}
        disabled={disabled}
        onClick={() => onChange('IMAGE')}
        style={{
          padding: '0.55rem 1.25rem',
          border: '1px solid ' + (mode === 'IMAGE' ? 'var(--brand-accent-border)' : 'transparent'),
          borderRadius: 'var(--radius-sm)',
          fontSize: '0.875rem',
          fontWeight: 700,
          cursor: disabled ? 'not-allowed' : 'pointer',
          backgroundColor: mode === 'IMAGE' ? 'var(--surface-elevated)' : 'transparent',
          color: mode === 'IMAGE' ? 'var(--brand-accent)' : 'var(--text-secondary)',
          boxShadow: mode === 'IMAGE' ? 'var(--shadow-sm)' : 'none',
          transition: 'all 0.15s ease'
        }}
      >
        📷 Image Inspection
      </button>
      <button
        type="button"
        role="tab"
        aria-selected={mode === 'VIDEO'}
        disabled={disabled}
        onClick={() => onChange('VIDEO')}
        style={{
          padding: '0.55rem 1.25rem',
          border: '1px solid ' + (mode === 'VIDEO' ? 'var(--brand-accent-border)' : 'transparent'),
          borderRadius: 'var(--radius-sm)',
          fontSize: '0.875rem',
          fontWeight: 700,
          cursor: disabled ? 'not-allowed' : 'pointer',
          backgroundColor: mode === 'VIDEO' ? 'var(--surface-elevated)' : 'transparent',
          color: mode === 'VIDEO' ? 'var(--brand-accent)' : 'var(--text-secondary)',
          boxShadow: mode === 'VIDEO' ? 'var(--shadow-sm)' : 'none',
          transition: 'all 0.15s ease'
        }}
      >
        🎥 Video / Dashcam (Async)
      </button>
    </div>
  );
};
