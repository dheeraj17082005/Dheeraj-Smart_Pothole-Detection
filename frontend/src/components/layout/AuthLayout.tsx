import React from 'react';
import { useTheme } from '../../context/ThemeContext';

interface AuthLayoutProps {
  children: React.ReactNode;
}

export const AuthLayout: React.FC<AuthLayoutProps> = ({ children }) => {
  const { theme, toggleTheme } = useTheme();

  return (
    <div className="auth-fullscreen-bg">
      {/* Top Brand Header Bar */}
      <header className="auth-header">
        <div className="auth-header-brand">
          <img
            src="/logo.png"
            alt="PotholeX Logo"
            className="auth-brand-logo"
          />
          <div className="auth-brand-title">
            Pothole<span className="accent-x">X</span>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
          <div className="auth-header-pills">
            <div className="auth-pill">
              <span className="solid-dot green"></span>
              Municipal Road Works
            </div>
            <div className="auth-pill">
              <span className="solid-dot cyan"></span>
              Civil Inspection Portal
            </div>
            <div className="auth-pill hide-mobile">
              <span className="solid-dot blue"></span>
              Officer Verification
            </div>
          </div>

          <button
            type="button"
            onClick={toggleTheme}
            title={`Switch to ${theme === 'dark' ? 'Light' : 'Dark'} Mode`}
            aria-label="Toggle Theme"
            style={{
              background: 'var(--surface-elevated)',
              border: '1px solid var(--border)',
              borderRadius: '6px',
              padding: '0.45rem 0.65rem',
              color: 'var(--text-primary)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.35rem',
              fontSize: '0.825rem',
              fontWeight: 600,
              transition: 'all 0.15s ease'
            }}
          >
            {theme === 'dark' ? (
              <>
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#F59E0B" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="5" />
                  <line x1="12" y1="1" x2="12" y2="3" />
                  <line x1="12" y1="21" x2="12" y2="23" />
                  <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
                  <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
                  <line x1="1" y1="12" x2="3" y2="12" />
                  <line x1="21" y1="12" x2="23" y2="12" />
                  <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
                  <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
                </svg>
                <span>Light</span>
              </>
            ) : (
              <>
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#2563EB" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
                </svg>
                <span>Dark</span>
              </>
            )}
          </button>
        </div>
      </header>

      {/* Main Solid Card Wrapper */}
      <main className="auth-main-wrapper">
        {children}
      </main>

      {/* Footer Notice */}
      <footer className="auth-footer">
        <div>
          &copy; {new Date().getFullYear()} <strong>PotholeX</strong> &bull; Public Works &amp; Municipal Infrastructure
        </div>
        <div className="auth-footer-links">
          <span>Public Works Department</span>
          <span>&bull;</span>
          <span>Road Operations</span>
          <span>&bull;</span>
          <span>Civil Hazard Tracking</span>
        </div>
      </footer>
    </div>
  );
};
