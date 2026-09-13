import React, { useState } from 'react';
import { Sidebar } from './Sidebar';
import { Header } from './Header';

interface LayoutProps {
  children: React.ReactNode;
}

export const Layout: React.FC<LayoutProps> = ({ children }) => {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  return (
    <div className="app-container">
      <Sidebar
        isOpen={isMobileMenuOpen}
        onClose={() => setIsMobileMenuOpen(false)}
      />

      <div className="app-main-content">
        <Header onToggleMobileMenu={() => setIsMobileMenuOpen(!isMobileMenuOpen)} />

        <main className="page-body">
          {children}
        </main>

        <footer className="app-footer">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
            <img
              src="/logo.png"
              alt="PotholeX"
              style={{ width: '20px', height: '20px', borderRadius: '3px' }}
            />
            <span>
              <strong style={{ color: 'var(--text-main)' }}>PotholeX</strong> &bull; Road Inspection &amp; Reporting System
            </span>
          </div>
          <div>
            Municipal Public Works &amp; Spatial Infrastructure Management
          </div>
        </footer>
      </div>
    </div>
  );
};
