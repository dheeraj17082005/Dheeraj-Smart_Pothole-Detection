import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { DashboardPage } from './DashboardPage';
import { apiClient } from '../services/api';
import { AuthProvider } from '../context/AuthContext';

vi.mock('../services/api', () => ({
  apiClient: {
    getDashboardStats: vi.fn(),
    getPotholes: vi.fn(),
    getPotholesMap: vi.fn()
  }
}));

// Mock PotholeMap so we don't invoke Leaflet canvas inside DashboardPage tests
vi.mock('../components/map/PotholeMap', () => ({
  PotholeMap: () => <div data-testid="mock-pothole-map">Mock Map</div>
}));

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('1. displays loading state while fetching dashboard data', () => {
    vi.mocked(apiClient.getDashboardStats).mockReturnValue(new Promise(() => {}));
    vi.mocked(apiClient.getPotholes).mockReturnValue(new Promise(() => {}));

    render(
      <AuthProvider>
        <MemoryRouter>
          <DashboardPage />
        </MemoryRouter>
      </AuthProvider>
    );

    expect(screen.getByText(/Loading operational statistics/i)).toBeInTheDocument();
  });

  it('2. renders summary cards with data from API', async () => {
    vi.mocked(apiClient.getDashboardStats).mockResolvedValue({
      totalPotholes: 25,
      reportedCount: 10,
      acknowledgedCount: 5,
      inProgressCount: 6,
      resolvedCount: 4,
      highSeverityCount: 8
    });

    vi.mocked(apiClient.getPotholes).mockResolvedValue({
      content: [
        {
          id: '11111111-2222-3333-4444-555566667777',
          latitude: 28.6139,
          longitude: 77.2090,
          address_text: 'Rajpath',
          first_detected_at: '2026-09-10T12:00:00Z',
          severity_score: 55.0,
          severity_class: 'HIGH',
          max_confidence: 0.92,
          status: 'REPORTED',
          is_duplicate: false,
          authority: { id: 'a1', name: 'NDMC Central', code: 'NDMC_CENTRAL', department_type: 'MUNICIPAL' },
          authority_code: 'NDMC_CENTRAL',
          detections: []
        }
      ],
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1,
      last: true
    });

    render(
      <AuthProvider>
        <MemoryRouter>
          <DashboardPage />
        </MemoryRouter>
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('stat-value-total')).toHaveTextContent('25');
      expect(screen.getByTestId('stat-value-reported')).toHaveTextContent('10');
      expect(screen.getByTestId('stat-value-acknowledged')).toHaveTextContent('5');
      expect(screen.getByTestId('stat-value-in_progress')).toHaveTextContent('6');
      expect(screen.getByTestId('stat-value-resolved')).toHaveTextContent('4');
      expect(screen.getByTestId('stat-value-high_severity')).toHaveTextContent('8');
    });

    // Verify recent report row
    expect(screen.getByTestId('recent-row-11111111-2222-3333-4444-555566667777')).toBeInTheDocument();
    expect(screen.getByText('NDMC Central')).toBeInTheDocument();
  });

  it('3. renders error state when API fails', async () => {
    vi.mocked(apiClient.getDashboardStats).mockRejectedValue(new Error('Connection to backend failed'));
    vi.mocked(apiClient.getPotholes).mockResolvedValue({
      content: [],
      page: 0,
      size: 5,
      totalElements: 0,
      totalPages: 0,
      last: true
    });

    render(
      <AuthProvider>
        <MemoryRouter>
          <DashboardPage />
        </MemoryRouter>
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByText(/Connection to backend failed/i)).toBeInTheDocument();
    });
  });
});
