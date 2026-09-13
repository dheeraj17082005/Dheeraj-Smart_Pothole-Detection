import React, { useState, useEffect } from 'react';
import { useLocation, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { apiClient } from '../../services/api';
import { NotificationResponse } from '../../types';

interface HeaderProps {
  onToggleMobileMenu: () => void;
}

export const Header: React.FC<HeaderProps> = ({ onToggleMobileMenu }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, isAuthenticated, isOfficer, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();

  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const [showNotifications, setShowNotifications] = useState(false);

  useEffect(() => {
    if (isAuthenticated) {
      apiClient.getUnreadNotificationCount()
        .then((res) => setUnreadCount(res.unreadCount))
        .catch(() => {});
    }
  }, [isAuthenticated, location]);

  const loadNotifications = async () => {
    if (!isAuthenticated) return;
    try {
      const page = await apiClient.getNotifications();
      setNotifications(page.content);
    } catch {}
  };

  const toggleNotificationsPopover = () => {
    if (!showNotifications) {
      loadNotifications();
    }
    setShowNotifications(!showNotifications);
  };

  const handleMarkAsRead = async (id: number) => {
    try {
      await apiClient.markNotificationAsRead(id);
      setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, readStatus: true } : n)));
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch {}
  };

  const getPageTitle = (pathname: string): string => {
    if (pathname === '/' || pathname === '/dashboard') {
      return isOfficer ? 'Municipal Officer Operations' : 'Citizen Overview & Reports';
    }
    if (pathname.startsWith('/upload')) return 'Report Road Defect';
    if (pathname.startsWith('/potholes/') && pathname !== '/potholes') return 'Defect Record & Remediation';
    if (pathname.startsWith('/potholes')) return 'Road Defect Registry';
    if (pathname.startsWith('/map')) return 'Interactive Road Defect Map';
    if (pathname.startsWith('/officer')) return 'Municipal Officer Operations';
    if (pathname.startsWith('/login')) return 'User Sign In';
    if (pathname.startsWith('/register')) return 'Citizen Registration';
    return 'PotholeX Road Portal';
  };

  return (
    <header className="app-topbar">
      <div className="topbar-left">
        <button
          type="button"
          className="mobile-nav-toggle"
          onClick={onToggleMobileMenu}
          aria-label="Toggle navigation menu"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </button>

        <Link to="/" className="topbar-home-brand" aria-label="PotholeX Home" title="PotholeX Home">
          <img src="/logo.png" alt="PotholeX" style={{ width: '32px', height: '32px', borderRadius: '4px' }} />
          <span style={{ fontWeight: 800, fontSize: '1.05rem', color: 'var(--text-main)' }}>
            Pothole<span style={{ color: 'var(--brand-accent)' }}>X</span>
          </span>
        </Link>

        <h1 className="topbar-page-title">{getPageTitle(location.pathname)}</h1>
      </div>

      <div className="topbar-right">
        {!isOfficer && (
          <Link to="/upload" className="btn btn-primary btn-sm" style={{ gap: '0.35rem' }}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            <span>Report Pothole</span>
          </Link>
        )}

        <div className="topbar-status-badge" title="PostGIS spatial database & backend microservices connected">
          <span className="beacon-dot" />
          <span>System Operational</span>
        </div>

        {/* Theme Mode Switcher Toggle */}
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
            fontSize: '0.85rem',
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
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#3B82F6" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
              </svg>
              <span>Dark</span>
            </>
          )}
        </button>

        {/* Notification Bell */}
        {isAuthenticated && (
          <div style={{ position: 'relative' }}>
            <button
              type="button"
              onClick={toggleNotificationsPopover}
              title="Notifications"
              style={{
                background: 'var(--surface-elevated)',
                border: '1px solid var(--border)',
                borderRadius: '6px',
                padding: '0.45rem 0.65rem',
                color: 'var(--text-main)',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '0.3rem',
                position: 'relative'
              }}
            >
              🔔
              {unreadCount > 0 && (
                <span
                  style={{
                    backgroundColor: '#EF4444',
                    color: '#FFF',
                    borderRadius: '50%',
                    padding: '0.15rem 0.4rem',
                    fontSize: '0.7rem',
                    fontWeight: 800,
                    lineHeight: 1
                  }}
                >
                  {unreadCount}
                </span>
              )}
            </button>

            {showNotifications && (
              <div
                style={{
                  position: 'absolute',
                  right: 0,
                  top: '110%',
                  width: '320px',
                  maxHeight: '380px',
                  backgroundColor: 'var(--surface)',
                  border: '1px solid var(--border)',
                  borderRadius: '10px',
                  boxShadow: 'var(--shadow-card)',
                  zIndex: 1000,
                  overflowY: 'auto',
                  padding: '0.75rem'
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.4rem' }}>
                  <strong style={{ fontSize: '0.85rem', color: 'var(--text-main)' }}>Notifications</strong>
                  <button type="button" onClick={() => setShowNotifications(false)} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}>✕</button>
                </div>

                {notifications.length === 0 ? (
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', textAlign: 'center', padding: '1rem 0' }}>
                    No notifications
                  </div>
                ) : (
                  notifications.map((n) => (
                    <div
                      key={n.id}
                      onClick={() => handleMarkAsRead(n.id)}
                      style={{
                        padding: '0.5rem',
                        borderRadius: '6px',
                        marginBottom: '0.4rem',
                        backgroundColor: n.readStatus ? 'transparent' : 'rgba(214, 245, 61, 0.08)',
                        border: '1px solid var(--border)',
                        cursor: 'pointer'
                      }}
                    >
                      <div style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-main)' }}>{n.title}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.15rem' }}>{n.message}</div>
                    </div>
                  ))
                )}
              </div>
            )}
          </div>
        )}

        {/* User Account / Officer Pill */}
        {isAuthenticated ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <div className="topbar-operator-pill">
              <div className="operator-avatar">{user?.fullName.charAt(0).toUpperCase()}</div>
              <span>{user?.fullName}</span>
              <span style={{ fontSize: '0.7rem', padding: '0.15rem 0.35rem', borderRadius: '4px', backgroundColor: isOfficer ? '#3B82F6' : '#10B981', color: '#FFF', marginLeft: '0.3rem' }}>
                {isOfficer ? 'OFFICER' : 'CITIZEN'}
              </span>
            </div>
            <button
              type="button"
              onClick={() => { logout(); navigate('/'); }}
              style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', fontSize: '0.8rem', fontWeight: 600 }}
            >
              Sign Out
            </button>
          </div>
        ) : (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Link to="/login" className="btn btn-secondary btn-sm">
              Sign In
            </Link>
            <Link to="/register" className="btn btn-primary btn-sm">
              Register
            </Link>
          </div>
        )}
      </div>
    </header>
  );
};
