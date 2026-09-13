import React, { useState, useEffect } from 'react';
import { DashboardStatsResponse, PotholeResponse, PageResponse } from '../types';
import { apiClient } from '../services/api';
import { StatCards } from '../components/dashboard/StatCards';
import { PotholeMap } from '../components/map/PotholeMap';
import { SeverityBadge, PotholeStatusBadge } from '../components/common/Badge';
import { formatCoordinates, formatDateTime } from '../utils/formatters';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { Alert } from '../components/common/Alert';
import { useNavigate, Link, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [stats, setStats] = useState<DashboardStatsResponse | null>(null);
  const [statsLoading, setStatsLoading] = useState<boolean>(true);
  const [statsError, setStatsError] = useState<string | null>(null);

  const [recentReports, setRecentReports] = useState<PotholeResponse[]>([]);
  const [recentLoading, setRecentLoading] = useState<boolean>(true);
  const [recentError, setRecentError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    apiClient.getDashboardStats()
      .then((data) => {
        if (mounted) {
          setStats(data);
          setStatsLoading(false);
        }
      })
      .catch((err) => {
        if (mounted) {
          setStatsError(err.message || 'Failed to load dashboard statistics.');
          setStatsLoading(false);
        }
      });

    apiClient.getPotholes({ page: 0, size: 5, sort: 'firstDetectedAt,desc' })
      .then((data: PageResponse<PotholeResponse>) => {
        if (mounted) {
          setRecentReports(data.content || []);
          setRecentLoading(false);
        }
      })
      .catch((err) => {
        if (mounted) {
          setRecentError(err.message || 'Failed to load recent pothole reports.');
          setRecentLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  if (user?.role === 'ROLE_OFFICER') {
    return <Navigate to="/officer/dashboard" replace />;
  }

  const openIssuesCount = stats
    ? stats.reportedCount + stats.acknowledgedCount + stats.inProgressCount
    : 0;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
      {/* Hero Header */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          flexWrap: 'wrap',
          gap: '1rem',
          paddingBottom: '0.25rem'
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
            <span style={{ fontSize: '0.72rem', fontWeight: 800, color: 'var(--brand-accent)', textTransform: 'uppercase', letterSpacing: '0.08em' }}>
              MY ROAD REPORTS & CIVIC DASHBOARD
            </span>
            <span style={{ color: 'var(--border)' }}>&bull;</span>
            <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>CITIZEN PORTAL</span>
          </div>
          <h1 style={{ fontSize: '1.95rem', fontWeight: 800, color: 'var(--text-main)', margin: '0 0 0.35rem 0', letterSpacing: '-0.025em' }}>
            MY REPORTS & ROAD OVERVIEW
          </h1>
          <p style={{ margin: 0, color: 'var(--text-muted)', fontSize: '0.925rem' }}>
            Monitor your reported road hazards, track repair status in real-time, and view nearby community defect reports.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <Link
            to="/upload"
            className="btn btn-primary"
            style={{ gap: '0.4rem' }}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            <span>Report Pothole</span>
          </Link>
          <Link
            to="/potholes"
            className="btn btn-secondary"
          >
            My Reports →
          </Link>
        </div>
      </div>

      {/* Top Operational Stats */}
      <section aria-label="Operational Summary">
        <StatCards stats={stats} loading={statsLoading} error={statsError} />
      </section>

      {/* Priority Action Needed Banner */}
      {stats && (stats.highSeverityCount > 0 || stats.reportedCount > 0) && (
        <div
          style={{
            backgroundColor: 'var(--surface)',
            color: 'var(--text-main)',
            borderRadius: '12px',
            padding: '1.25rem 1.5rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '1rem',
            border: '1px solid var(--border)',
            boxShadow: 'var(--shadow-card)'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <div
              style={{
                width: '42px',
                height: '42px',
                borderRadius: '8px',
                backgroundColor: 'rgba(239, 68, 68, 0.15)',
                border: '1px solid rgba(239, 68, 68, 0.35)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#EF4444',
                fontSize: '1.25rem'
              }}
            >
              ⚠️
            </div>
            <div>
              <div style={{ fontWeight: 700, fontSize: '0.95rem', color: '#ffffff' }}>
                Priority Action Required ({openIssuesCount} Open Hazards)
              </div>
              <div style={{ fontSize: '0.825rem', color: 'var(--text-muted)', marginTop: '0.15rem' }}>
                {stats.highSeverityCount} high-severity defects identified requiring expedited public works remediation.
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <Link
              to="/potholes?severity=HIGH"
              style={{
                padding: '0.45rem 0.95rem',
                backgroundColor: '#EF4444',
                color: '#ffffff',
                borderRadius: '6px',
                fontSize: '0.8125rem',
                fontWeight: 700,
                textDecoration: 'none'
              }}
            >
              Filter High Severity
            </Link>
            <Link
              to="/map"
              className="btn btn-secondary btn-sm"
            >
              Open Interactive Map
            </Link>
          </div>
        </div>
      )}

      {/* Middle Map Section */}
      <section
        aria-label="Interactive Pothole Map"
        className="card"
        style={{ padding: '1.25rem' }}
      >
        <div className="card-header">
          <div>
            <h2 className="card-title">Live Defect Map</h2>
            <p className="card-subtitle">
              Potholes update dynamically based on map viewport bounds. Click markers for defect inspection.
            </p>
          </div>
          <Link
            to="/map"
            style={{ fontSize: '0.8125rem', fontWeight: 700, color: 'var(--brand-accent)' }}
          >
            Full Map View ↗
          </Link>
        </div>

        <PotholeMap height="480px" />
      </section>

      {/* Bottom Recent Inspections Table */}
      <section
        aria-label="Recent Pothole Reports"
        className="card"
        style={{ padding: '1.25rem' }}
      >
        <div className="card-header">
          <div>
            <h2 className="card-title">Recent Inspections</h2>
            <p className="card-subtitle">
              Latest recorded roadway defects sorted chronologically by report timestamp.
            </p>
          </div>
          <Link
            to="/potholes"
            style={{ fontSize: '0.8125rem', fontWeight: 700, color: 'var(--brand-accent)' }}
          >
            View full registry ({stats?.totalPotholes ?? '—'}) →
          </Link>
        </div>

        {recentLoading ? (
          <div style={{ padding: '2.5rem', textAlign: 'center' }}>
            <LoadingSpinner text="Loading recent reports..." />
          </div>
        ) : recentError ? (
          <Alert variant="error" title="Could not load recent reports">
            {recentError}
          </Alert>
        ) : recentReports.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '2.5rem', color: 'var(--text-muted)', fontSize: '0.875rem' }}>
            No potholes recorded yet. Submit an image or dashcam stream to begin defect tracking.
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem', textAlign: 'left' }}>
              <thead>
                <tr style={{ background: 'var(--surface-elevated)', borderBottom: '1px solid var(--border)', color: 'var(--text-muted)', fontSize: '0.725rem', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                  <th style={{ padding: '0.75rem 0.85rem' }}>Defect ID</th>
                  <th style={{ padding: '0.75rem 0.85rem' }}>Severity</th>
                  <th style={{ padding: '0.75rem 0.85rem' }}>Civic Authority</th>
                  <th style={{ padding: '0.75rem 0.85rem' }}>Location</th>
                  <th style={{ padding: '0.75rem 0.85rem' }}>Status</th>
                  <th style={{ padding: '0.75rem 0.85rem' }}>Detected At</th>
                  <th style={{ padding: '0.75rem 0.85rem', textAlign: 'right' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {recentReports.map((pothole) => {
                  const authorityText = pothole.authority?.name || (pothole.authority_code === 'UNKNOWN_AUTHORITY' ? 'Authority requires triage' : 'Unassigned');
                  const isTriage = !pothole.authority || pothole.authority_code === 'UNKNOWN_AUTHORITY';

                  return (
                    <tr
                      key={pothole.id}
                      data-testid={`recent-row-${pothole.id}`}
                      onClick={() => navigate(`/potholes/${pothole.id}`)}
                      style={{
                        borderBottom: '1px solid var(--border)',
                        cursor: 'pointer',
                        transition: 'background-color 0.15s ease'
                      }}
                      onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--surface-hover)')}
                      onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
                    >
                      <td style={{ padding: '0.85rem 0.85rem', fontFamily: 'var(--font-mono)', fontWeight: 700, color: 'var(--text-primary)' }}>
                        #{pothole.id.slice(0, 8)}
                      </td>
                      <td style={{ padding: '0.85rem 0.85rem' }}>
                        <SeverityBadge severity={pothole.severity_class} score={pothole.severity_score} />
                      </td>
                      <td style={{ padding: '0.85rem 0.85rem', color: isTriage ? '#0284C7' : 'var(--text-primary)', fontWeight: isTriage ? 700 : 500 }}>
                        {authorityText}
                      </td>
                      <td style={{ padding: '0.85rem 0.85rem', fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                        {formatCoordinates(pothole.latitude, pothole.longitude)}
                      </td>
                      <td style={{ padding: '0.85rem 0.85rem' }}>
                        <PotholeStatusBadge status={pothole.status} />
                      </td>
                      <td style={{ padding: '0.85rem 0.85rem', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                        {formatDateTime(pothole.first_detected_at)}
                      </td>
                      <td style={{ padding: '0.85rem 0.85rem', textAlign: 'right' }}>
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            navigate(`/potholes/${pothole.id}`);
                          }}
                          className="btn btn-secondary btn-sm"
                        >
                          View
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
};
