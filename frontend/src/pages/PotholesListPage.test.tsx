import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { PotholesListPage } from './PotholesListPage';
import { apiClient } from '../services/api';

vi.mock('../services/api', () => ({
  apiClient: {
    getPotholes: vi.fn(),
    getAuthorities: vi.fn()
  }
}));

describe('PotholesListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(apiClient.getAuthorities).mockResolvedValue([
      { id: 'auth-1', name: 'Delhi PWD', code: 'DEMO_PWD_ARTERIAL', department_type: 'PWD' }
    ]);
  });

  it('8. filters create correct query parameters when submitted', async () => {
    vi.mocked(apiClient.getPotholes).mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      last: true
    });

    render(
      <MemoryRouter>
        <PotholesListPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(apiClient.getPotholes).toHaveBeenCalledTimes(1);
    });

    // Change status filter
    const statusSelect = screen.getByTestId('filter-status-select');
    fireEvent.change(statusSelect, { target: { value: 'IN_PROGRESS' } });

    // Change severity filter
    const severitySelect = screen.getByTestId('filter-severity-select');
    fireEvent.change(severitySelect, { target: { value: 'HIGH' } });

    // Click apply
    const applyButton = screen.getByTestId('filter-apply-button');
    fireEvent.click(applyButton);

    await waitFor(() => {
      expect(apiClient.getPotholes).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'IN_PROGRESS',
          severity: 'HIGH',
          page: 0
        })
      );
    });
  });

  it('9. pagination works and requests next page', async () => {
    vi.mocked(apiClient.getPotholes).mockResolvedValue({
      content: [
        {
          id: '12345678-aaaa-bbbb-cccc-dddddddddddd',
          latitude: 28.6139,
          longitude: 77.2090,
          severity_score: 45.0,
          severity_class: 'MEDIUM',
          max_confidence: 0.88,
          status: 'REPORTED',
          is_duplicate: false,
          authority_code: 'DEMO_PWD_ARTERIAL',
          detections: []
        }
      ],
      page: 0,
      size: 10,
      totalElements: 25,
      totalPages: 3,
      last: false
    });

    render(
      <MemoryRouter>
        <PotholesListPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('pagination-next')).toBeInTheDocument();
    });

    const nextBtn = screen.getByTestId('pagination-next');
    fireEvent.click(nextBtn);

    await waitFor(() => {
      expect(apiClient.getPotholes).toHaveBeenCalledWith(
        expect.objectContaining({
          page: 1
        })
      );
    });
  });

  it('10. renders empty result state when 0 records returned', async () => {
    vi.mocked(apiClient.getPotholes).mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      last: true
    });

    render(
      <MemoryRouter>
        <PotholesListPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('potholes-empty-state')).toHaveTextContent('No potholes found');
    });
  });
});
