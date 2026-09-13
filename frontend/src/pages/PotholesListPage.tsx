import React, { useState, useEffect, useCallback } from 'react';
import { PotholeResponse, PageResponse, PotholeFilterParams } from '../types';
import { apiClient } from '../services/api';
import { PotholeFilterBar } from '../components/potholes/PotholeFilterBar';
import { PotholeTable } from '../components/potholes/PotholeTable';
import { Alert } from '../components/common/Alert';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export const PotholesListPage: React.FC = () => {
  const { user } = useAuth();
  const isOfficer = user?.role === 'ROLE_OFFICER';

  const [filters, setFilters] = useState<PotholeFilterParams>({
    status: '',
    severity: '',
    authority: '',
    fromDate: '',
    toDate: '',
    page: 0,
    size: 10,
    sort: 'firstDetectedAt,desc'
  });

  const [data, setData] = useState<PageResponse<PotholeResponse>>({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    last: true
  });

  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchPotholes = useCallback(async (currentFilters: PotholeFilterParams) => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.getPotholes(currentFilters);
      setData(response);
    } catch (err: any) {
      setError(err.message || 'Failed to load potholes.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchPotholes(filters);
  }, [filters, fetchPotholes]);

  const handleFilterChange = (newFilters: PotholeFilterParams) => {
    setFilters(newFilters);
  };

  const handleReset = () => {
    setFilters({
      status: '',
      severity: '',
      authority: '',
      fromDate: '',
      toDate: '',
      page: 0,
      size: 10,
      sort: 'firstDetectedAt,desc'
    });
  };

  const handlePageChange = (newPage: number) => {
    setFilters((prev) => ({ ...prev, page: newPage }));
  };

  const handlePageSizeChange = (newSize: number) => {
    setFilters((prev) => ({ ...prev, size: newSize, page: 0 }));
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
            <span style={{ fontSize: '0.72rem', fontWeight: 800, color: 'var(--brand-accent)', textTransform: 'uppercase', letterSpacing: '0.08em' }}>
              DEFECT TELEMATICS
            </span>
            <span style={{ color: 'var(--border)' }}>&bull;</span>
            <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>POSTGIS SPATIAL INDEX</span>
          </div>
          <h1 style={{ fontSize: '1.95rem', fontWeight: 800, color: 'var(--text-primary)', margin: '0 0 0.35rem 0', letterSpacing: '-0.025em' }}>
            ROAD DEFECT REGISTRY
          </h1>
          <p style={{ margin: 0, color: 'var(--text-secondary)', fontSize: '0.925rem' }}>
            Browse, filter, and audit verified road surface hazards and civic dispatches.
          </p>
        </div>

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
      </div>

      {/* Filter Toolbar */}
      <PotholeFilterBar
        filters={filters}
        onFilterChange={handleFilterChange}
        onReset={handleReset}
        loading={loading}
      />

      {/* Error Alert */}
      {error && (
        <Alert variant="error" title="Error Querying Potholes">
          {error}
        </Alert>
      )}

      {/* Results Table */}
      <PotholeTable
        data={data}
        loading={loading}
        onPageChange={handlePageChange}
        onPageSizeChange={handlePageSizeChange}
      />
    </div>
  );
};
