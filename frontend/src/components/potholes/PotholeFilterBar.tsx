import React, { useState, useEffect } from 'react';
import { PotholeFilterParams, PotholeStatus, SeverityClass, AuthorityResponse } from '../../types';
import { apiClient } from '../../services/api';

interface PotholeFilterBarProps {
  filters: PotholeFilterParams;
  onFilterChange: (newFilters: PotholeFilterParams) => void;
  onReset: () => void;
  loading?: boolean;
}

export const PotholeFilterBar: React.FC<PotholeFilterBarProps> = ({
  filters,
  onFilterChange,
  onReset,
  loading = false
}) => {
  const [authorities, setAuthorities] = useState<AuthorityResponse[]>([]);
  const [localFilters, setLocalFilters] = useState<PotholeFilterParams>(filters);

  useEffect(() => {
    setLocalFilters(filters);
  }, [filters]);

  useEffect(() => {
    let mounted = true;
    apiClient.getAuthorities()
      .then((data) => {
        if (mounted) setAuthorities(data);
      })
      .catch(() => {
        // Silently tolerate authority lookup failure
      });
    return () => {
      mounted = false;
    };
  }, []);

  const handleChange = (field: keyof PotholeFilterParams, value: any) => {
    setLocalFilters((prev) => ({ ...prev, [field]: value, page: 0 }));
  };

  const handleApply = (e: React.FormEvent) => {
    e.preventDefault();
    onFilterChange({ ...localFilters, page: 0 });
  };

  const handleResetClick = () => {
    const emptyFilters: PotholeFilterParams = {
      status: '',
      severity: '',
      authority: '',
      fromDate: '',
      toDate: '',
      page: 0,
      size: filters.size || 10,
      sort: 'firstDetectedAt,desc'
    };
    setLocalFilters(emptyFilters);
    onReset();
  };

  return (
    <form
      onSubmit={handleApply}
      data-testid="pothole-filter-bar"
      style={{
        backgroundColor: 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: '10px',
        padding: '1.25rem',
        marginBottom: '1.25rem',
        display: 'flex',
        flexWrap: 'wrap',
        gap: '1rem',
        alignItems: 'flex-end',
        boxShadow: 'var(--shadow-sm)'
      }}
    >
      {/* Status Filter */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem', minWidth: '150px' }}>
        <label htmlFor="filter-status" style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
          Remediation Status
        </label>
        <select
          id="filter-status"
          data-testid="filter-status-select"
          value={localFilters.status || ''}
          onChange={(e) => handleChange('status', e.target.value as PotholeStatus | '')}
          className="form-input"
        >
          <option value="">All Statuses</option>
          <option value="REPORTED">Reported</option>
          <option value="ACKNOWLEDGED">Acknowledged</option>
          <option value="IN_PROGRESS">In Progress</option>
          <option value="RESOLVED">Resolved</option>
        </select>
      </div>

      {/* Severity Filter */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem', minWidth: '140px' }}>
        <label htmlFor="filter-severity" style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
          Visual Severity
        </label>
        <select
          id="filter-severity"
          data-testid="filter-severity-select"
          value={localFilters.severity || ''}
          onChange={(e) => handleChange('severity', e.target.value as SeverityClass | '')}
          className="form-input"
        >
          <option value="">All Severities</option>
          <option value="HIGH">High Severity</option>
          <option value="MEDIUM">Medium Severity</option>
          <option value="LOW">Low Severity</option>
        </select>
      </div>

      {/* Authority Filter */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem', minWidth: '200px' }}>
        <label htmlFor="filter-authority" style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
          Civic Jurisdiction
        </label>
        <select
          id="filter-authority"
          data-testid="filter-authority-select"
          value={localFilters.authority || ''}
          onChange={(e) => handleChange('authority', e.target.value)}
          className="form-input"
        >
          <option value="">All Authorities</option>
          <option value="UNKNOWN_AUTHORITY">Authority requires triage</option>
          {authorities.map((auth) => (
            <option key={auth.id} value={auth.code}>
              {auth.name}
            </option>
          ))}
        </select>
      </div>

      {/* Date From */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
        <label htmlFor="filter-from-date" style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
          From Date
        </label>
        <input
          type="date"
          id="filter-from-date"
          data-testid="filter-from-date"
          value={localFilters.fromDate ? localFilters.fromDate.split('T')[0] : ''}
          onChange={(e) => handleChange('fromDate', e.target.value ? `${e.target.value}T00:00:00Z` : '')}
          className="form-input"
        />
      </div>

      {/* Date To */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
        <label htmlFor="filter-to-date" style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
          To Date
        </label>
        <input
          type="date"
          id="filter-to-date"
          data-testid="filter-to-date"
          value={localFilters.toDate ? localFilters.toDate.split('T')[0] : ''}
          onChange={(e) => handleChange('toDate', e.target.value ? `${e.target.value}T23:59:59Z` : '')}
          className="form-input"
        />
      </div>

      {/* Action Buttons */}
      <div style={{ display: 'flex', gap: '0.5rem', marginLeft: 'auto' }}>
        <button
          type="submit"
          disabled={loading}
          data-testid="filter-apply-button"
          className="btn btn-primary btn-sm"
        >
          {loading ? 'Filtering...' : 'Apply Filters'}
        </button>

        <button
          type="button"
          onClick={handleResetClick}
          disabled={loading}
          data-testid="filter-reset-button"
          className="btn btn-secondary btn-sm"
        >
          Reset
        </button>
      </div>
    </form>
  );
};
