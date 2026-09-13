import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose }) => {
  const { isOfficer } = useAuth();

  return (
    <>
      {/* Mobile backdrop overlay */}
      {isOpen && (
        <div
          onClick={onClose}
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.75)',
            backdropFilter: 'blur(3px)',
            zIndex: 90
          }}
        />
      )}

      <aside className={`app-sidebar ${isOpen ? 'open' : ''}`}>
        {/* Brand Header with Logo as HOME */}
        <NavLink
          to="/"
          className="sidebar-brand"
          onClick={onClose}
          aria-label="PotholeX Home"
          title="PotholeX Home"
        >
          <img
            src="/logo.png"
            alt="PotholeX Logo"
            className="sidebar-logo-img"
          />
          <div className="sidebar-brand-text">
            <span className="sidebar-brand-title">
              Pothole<span className="accent-x">X</span>
            </span>
            <span className="sidebar-brand-subtitle">ROAD MAINTENANCE PORTAL</span>
          </div>
        </NavLink>

        {/* Navigation Section */}
        <nav className="sidebar-nav" aria-label="Main Navigation">
          <div className="sidebar-section-label">Operations</div>

          <NavLink
            to="/"
            end
            className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            onClick={onClose}
          >
            <svg
              className="sidebar-nav-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <rect x="3" y="3" width="7" height="9" />
              <rect x="14" y="3" width="7" height="5" />
              <rect x="14" y="12" width="7" height="9" />
              <rect x="3" y="16" width="7" height="5" />
            </svg>
            <span>Overview</span>
          </NavLink>

          {isOfficer && (
            <NavLink
              to="/officer/dashboard"
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
              onClick={onClose}
            >
              <svg
                className="sidebar-nav-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
              </svg>
              <span>Officer Portal</span>
            </NavLink>
          )}

          {/* Only Citizens can report road defects. Hide report/upload option from Officers */}
          {!isOfficer && (
            <NavLink
              to="/upload"
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
              onClick={onClose}
            >
              <svg
                className="sidebar-nav-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
                <circle cx="12" cy="13" r="4" />
              </svg>
              <span>Report Defect</span>
            </NavLink>
          )}

          <NavLink
            to="/potholes"
            className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            onClick={onClose}
          >
            <svg
              className="sidebar-nav-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <polyline points="14 2 14 8 20 8" />
              <line x1="16" y1="13" x2="8" y2="13" />
              <line x1="16" y1="17" x2="8" y2="17" />
              <polyline points="10 9 9 9 8 9" />
            </svg>
            <span>Potholes</span>
          </NavLink>

          <NavLink
            to="/map"
            className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            onClick={onClose}
          >
            <svg
              className="sidebar-nav-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6" />
              <line x1="8" y1="2" x2="8" y2="18" />
              <line x1="16" y1="6" x2="16" y2="22" />
            </svg>
            <span>Live Map</span>
          </NavLink>
        </nav>

        {/* Footer System Status */}
        <div className="sidebar-footer">
          <div className="sidebar-system-status">
            <span className="status-beacon-pulse" />
            <span>System Operational</span>
          </div>
        </div>
      </aside>
    </>
  );
};
