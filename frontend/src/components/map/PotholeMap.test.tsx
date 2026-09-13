import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { PotholeMap } from './PotholeMap';
import { apiClient } from '../../services/api';

vi.mock('../../services/api', () => ({
  apiClient: {
    getPotholesMap: vi.fn()
  }
}));

// Mock react-leaflet components to avoid canvas / DOM layout issues in JSDOM
vi.mock('react-leaflet', () => ({
  MapContainer: ({ children }: any) => <div data-testid="leaflet-map-container">{children}</div>,
  TileLayer: () => <div data-testid="leaflet-tile-layer" />,
  Marker: ({ children, position }: any) => (
    <div data-testid={`leaflet-marker-${position[0]}-${position[1]}`}>
      {children}
    </div>
  ),
  Popup: ({ children }: any) => <div data-testid="leaflet-popup">{children}</div>,
  useMap: () => ({
    getBounds: () => ({
      getSouth: () => 28.5,
      getWest: () => 77.1,
      getNorth: () => 28.7,
      getEast: () => 77.3
    })
  }),
  useMapEvents: vi.fn()
}));

describe('PotholeMap Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('4 & 5. renders markers from API data with correct severity and status information', async () => {
    vi.mocked(apiClient.getPotholesMap).mockResolvedValue([
      {
        id: '22222222-3333-4444-5555-666677778888',
        latitude: 28.6139,
        longitude: 77.2090,
        severity_class: 'HIGH',
        severity_score: 72.5,
        status: 'REPORTED',
        confidence: 0.94,
        first_detected_at: '2026-09-10T14:00:00Z',
        authority_name: 'Delhi PWD',
        authority_code: 'DEMO_PWD_ARTERIAL',
        is_duplicate: false
      }
    ]);

    render(
      <MemoryRouter>
        <PotholeMap />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('leaflet-marker-28.6139-77.209')).toBeInTheDocument();
    });

    // Check popup content
    const popup = screen.getByTestId('leaflet-popup');
    expect(popup).toHaveTextContent('#22222222');
    expect(popup).toHaveTextContent('HIGH');
    expect(popup).toHaveTextContent('REPORTED');
    expect(popup).toHaveTextContent('Delhi PWD');
    expect(popup).toHaveTextContent('Open Inspection Record →');
  });

  it('6. debounces and queries viewport bounds on load', async () => {
    vi.mocked(apiClient.getPotholesMap).mockResolvedValue([]);

    render(
      <MemoryRouter>
        <PotholeMap />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(apiClient.getPotholesMap).toHaveBeenCalledTimes(1);
      expect(apiClient.getPotholesMap).toHaveBeenCalledWith(
        expect.objectContaining({
          minLat: 28.5,
          minLng: 77.1,
          maxLat: 28.7,
          maxLng: 77.3
        }),
        expect.any(AbortSignal)
      );
    });
  });

  it('7. displays empty state banner when no potholes are within viewport', async () => {
    vi.mocked(apiClient.getPotholesMap).mockResolvedValue([]);

    render(
      <MemoryRouter>
        <PotholeMap />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('map-empty-state')).toHaveTextContent(
        'No recorded road defects in this viewport area.'
      );
    });
  });
});
