import React, { useEffect } from 'react';

interface ImageLightboxModalProps {
  isOpen: boolean;
  imageUrl: string;
  title?: string;
  subtitle?: string;
  onClose: () => void;
}

export const ImageLightboxModal: React.FC<ImageLightboxModalProps> = ({
  isOpen,
  imageUrl,
  title = 'Visual Evidence Inspection',
  subtitle,
  onClose
}) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown);
      document.body.style.overflow = 'hidden';
    }
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = 'unset';
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label={title}
      onClick={onClose}
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.85)',
        backdropFilter: 'blur(8px)',
        WebkitBackdropFilter: 'blur(8px)',
        zIndex: 3000,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '1.5rem',
        animation: 'fadeIn 0.15s ease-out'
      }}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          position: 'relative',
          maxWidth: '92vw',
          maxHeight: '90vh',
          display: 'flex',
          flexDirection: 'column',
          backgroundColor: 'var(--surface)',
          borderRadius: '12px',
          border: '1px solid var(--border)',
          overflow: 'hidden',
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.7)'
        }}
      >
        {/* Header Bar */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '0.85rem 1.25rem',
            backgroundColor: 'var(--surface-elevated)',
            borderBottom: '1px solid var(--border)',
            color: 'var(--text-main)'
          }}
        >
          <div>
            <h3 style={{ margin: 0, fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-main)', letterSpacing: '0.02em', textTransform: 'uppercase' }}>
              {title}
            </h3>
            {subtitle && (
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{subtitle}</span>
            )}
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <a
              href={imageUrl}
              target="_blank"
              rel="noreferrer"
              style={{
                color: 'var(--brand-accent)',
                fontSize: '0.75rem',
                textDecoration: 'none',
                fontWeight: 700,
                padding: '0.35rem 0.65rem',
                borderRadius: '6px',
                backgroundColor: 'rgba(212, 246, 61, 0.1)',
                border: '1px solid rgba(212, 246, 61, 0.2)',
                textTransform: 'uppercase',
                letterSpacing: '0.05em'
              }}
            >
              Open Original ↗
            </a>
            <button
              type="button"
              onClick={onClose}
              aria-label="Close image viewer"
              style={{
                background: 'transparent',
                border: 'none',
                color: 'var(--text-muted)',
                fontSize: '1.25rem',
                lineHeight: 1,
                cursor: 'pointer',
                padding: '0.25rem 0.5rem',
                borderRadius: '4px',
                transition: 'color 0.15s ease'
              }}
              onMouseEnter={(e) => (e.currentTarget.style.color = '#FFFFFF')}
              onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--text-muted)')}
            >
              ✕
            </button>
          </div>
        </div>

        {/* Image Content */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '1rem',
            backgroundColor: '#050709',
            overflow: 'auto'
          }}
        >
          <img
            src={imageUrl}
            alt={title}
            style={{
              maxWidth: '100%',
              maxHeight: '75vh',
              objectFit: 'contain',
              borderRadius: '6px',
              boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.5)'
            }}
          />
        </div>
      </div>
    </div>
  );
};

