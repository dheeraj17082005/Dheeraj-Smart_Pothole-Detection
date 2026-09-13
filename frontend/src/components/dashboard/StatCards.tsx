import React from 'react';
import { DashboardStatsResponse } from '../../types';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { Alert } from '../common/Alert';

interface StatCardsProps {
  stats: DashboardStatsResponse | null;
  loading: boolean;
  error: string | null;
}

interface CardConfig {
  id: string;
  label: string;
  value: number;
  color: string;
  borderColor: string;
  icon: React.ReactNode;
}

export const StatCards: React.FC<StatCardsProps> = ({ stats, loading, error }) => {
  if (loading) {
    return (
      <div style={{ padding: '2.5rem', textAlign: 'center', backgroundColor: 'var(--surface)', borderRadius: '12px', border: '1px solid var(--border)' }}>
        <LoadingSpinner text="Loading operational statistics..." />
      </div>
    );
  }

  if (error) {
    return (
      <Alert variant="error" title="Statistics Unavailable">
        {error}
      </Alert>
    );
  }

  if (!stats) {
    return null;
  }

  const cards: CardConfig[] = [
    {
      id: 'total',
      label: 'TOTAL POTHOLES',
      value: stats.totalPotholes,
      color: 'var(--text-primary)',
      borderColor: 'var(--border)',
      icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--brand-accent)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
        </svg>
      )
    },
    {
      id: 'reported',
      label: 'OPEN / REPORTED',
      value: stats.reportedCount,
      color: '#EF4444',
      borderColor: 'rgba(239, 68, 68, 0.3)',
      icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#EF4444" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="8" x2="12" y2="12" />
          <line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
      )
    },
    {
      id: 'acknowledged',
      label: 'ACKNOWLEDGED',
      value: stats.acknowledgedCount,
      color: '#0284C7',
      borderColor: 'rgba(2, 132, 199, 0.35)',
      icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#0284C7" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
          <polyline points="14 2 14 8 20 8" />
          <line x1="16" y1="13" x2="8" y2="13" />
        </svg>
      )
    },
    {
      id: 'in_progress',
      label: 'IN PROGRESS',
      value: stats.inProgressCount,
      color: '#3B82F6',
      borderColor: 'rgba(59, 130, 246, 0.3)',
      icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#3B82F6" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <polygon points="12 2 2 7 12 12 22 7 12 2" />
          <polyline points="2 17 12 22 22 17" />
          <polyline points="2 12 12 17 22 12" />
        </svg>
      )
    },
    {
      id: 'resolved',
      label: 'RESOLVED',
      value: stats.resolvedCount,
      color: '#10B981',
      borderColor: 'rgba(16, 185, 129, 0.3)',
      icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#10B981" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
          <polyline points="22 4 12 14.01 9 11.01" />
        </svg>
      )
    },
    {
      id: 'high_severity',
      label: 'HIGH SEVERITY',
      value: stats.highSeverityCount,
      color: '#EF4444',
      borderColor: 'rgba(239, 68, 68, 0.35)',
      icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#EF4444" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
          <line x1="12" y1="9" x2="12" y2="13" />
          <line x1="12" y1="17" x2="12.01" y2="17" />
        </svg>
      )
    }
  ];

  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(175px, 1fr))',
        gap: '1rem'
      }}
    >
      {cards.map((card) => (
        <div
          key={card.id}
          data-testid={`stat-card-${card.id}`}
          style={{
            backgroundColor: 'var(--surface)',
            border: `1px solid ${card.borderColor}`,
            borderRadius: '10px',
            padding: '1.15rem 1rem',
            display: 'flex',
            flexDirection: 'column',
            gap: '0.35rem',
            boxShadow: 'var(--shadow-card)',
            transition: 'transform 0.15s ease, border-color 0.15s ease'
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-muted)', letterSpacing: '0.06em' }}>
              {card.label}
            </span>
            <div style={{ padding: '0.25rem', borderRadius: '4px', backgroundColor: 'var(--surface-elevated)' }}>
              {card.icon}
            </div>
          </div>
          <div
            data-testid={`stat-value-${card.id}`}
            style={{
              fontSize: '1.85rem',
              fontWeight: 800,
              color: card.color,
              lineHeight: 1.1,
              letterSpacing: '-0.02em',
              fontFamily: 'var(--font-sans)'
            }}
          >
            {card.value.toLocaleString()}
          </div>
        </div>
      ))}
    </div>
  );
};
