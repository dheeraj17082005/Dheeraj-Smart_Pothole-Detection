import React from 'react';
import { PotholeMap } from '../components/map/PotholeMap';
import { Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export const MapPage: React.FC = () => {
  const { user } = useAuth();
  const isOfficer = user?.role === 'ROLE_OFFICER';

  const [searchParams] = useSearchParams();
  const latParam = searchParams.get('lat');
  const lngParam = searchParams.get('lng');
  const idParm = searchParams.get('id');

  const focusCoords: [number, number] | null =
    latParam && lngParam && !isNaN(parseFloat(latParam)) && !isNaN(parseFloat(lngParam))
      ? [parseFloat(latParam), parseFloat(lngParam)]
      : null;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Page Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
            <span style={{ fontSize: '0.72rem', fontWeight: 800, color: 'var(--brand-accent)', textTransform: 'uppercase', letterSpacing: '0.08em' }}>
              GIS OPERATIONS
            </span>
            <span style={{ color: 'var(--border)' }}>&bull;</span>
            <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>MUNICIPAL ROAD DEFECT MAP</span>
          </div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--text-main)', margin: '0 0 0.35rem 0', letterSpacing: '-0.03em', textTransform: 'uppercase' }}>
            Interactive Road Defect Map
          </h1>
          <p style={{ margin: 0, color: 'var(--text-muted)', fontSize: '0.875rem' }}>
            Geographic operations view. Pan and zoom across any road network to query defects or jump directly to coordinates.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem' }}>
          {!isOfficer && (
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
          )}
          <Link
            to="/potholes"
            className="btn btn-secondary"
          >
            Registry Table →
          </Link>
        </div>
      </div>

      {/* Full-view Map Card */}
      <div className="card" style={{ padding: '0.75rem', position: 'relative' }}>
        <PotholeMap
          height="640px"
          focusCoords={focusCoords}
          focusPotholeId={idParm}
        />
      </div>
    </div>
  );
};


