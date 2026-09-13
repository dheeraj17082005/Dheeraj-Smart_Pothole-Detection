import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { PotholeDetailPage } from './PotholeDetailPage';
import { apiClient } from '../services/api';
import { PotholeDetailResponse, PotholeStatusHistoryResponse } from '../types';

vi.mock('../services/api', () => ({
  apiClient: {
    getPothole: vi.fn(),
    getPotholeHistory: vi.fn(),
    updatePotholeStatus: vi.fn(),
    updateOfficerReportStatus: vi.fn()
  }
}));

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 'u-1', email: 'officer@pwd.gov.in', fullName: 'Officer Vikram', role: 'ROLE_OFFICER' },
    isAuthenticated: true
  })
}));

// Mock react-leaflet
vi.mock('react-leaflet', () => ({
  MapContainer: ({ children }: any) => <div data-testid="detail-map-container">{children}</div>,
  TileLayer: () => <div />,
  Marker: ({ children }: any) => <div>{children}</div>,
  Popup: ({ children }: any) => <div>{children}</div>
}));

const mockPothole: PotholeDetailResponse = {
  id: '33333333-4444-5555-6666-777788889999',
  latitude: 28.6139,
  longitude: 77.2090,
  addressText: 'Rajpath Avenue, New Delhi',
  firstDetectedAt: '2026-09-10T15:30:00Z',
  severityScore: 62.4,
  severityClass: 'HIGH',
  maxConfidence: 0.94,
  status: 'REPORTED',
  isDuplicate: false,
  duplicateOfId: null,
  authority: {
    id: 'auth-1',
    name: 'Delhi Public Works Department',
    code: 'DEMO_PWD_ARTERIAL',
    department_type: 'PWD'
  },
  authorityCode: 'DEMO_PWD_ARTERIAL',
  evidence: {
    representativeImageUrl: 'http://localhost:9000/pothole-annotated/ann_1.jpg',
    representativeKey: 'ann_1.jpg',
    rawMediaUrl: 'http://localhost:9000/pothole-raw/raw_1.jpg',
    rawKey: 'raw_1.jpg',
    mediaType: 'IMAGE'
  },
  report: {
    id: 'rep-1',
    pothole_id: '33333333-4444-5555-6666-777788889999',
    authority: { id: 'auth-1', name: 'Delhi Public Works Department', code: 'DEMO_PWD_ARTERIAL', department_type: 'PWD' },
    status: 'DISPATCHED',
    idempotency_key: 'report-33333333-auth-1-v1',
    created_timestamp: '2026-09-10T15:30:05Z',
    external_reference: 'PWD-TICKET-2026-001',
    attempts: [
      {
        id: 'att-1',
        attempt_number: 1,
        channel: 'MOCK_EMAIL',
        idempotency_key: 'report-33333333-auth-1-v1',
        attempt_timestamp: '2026-09-10T15:30:06Z',
        status: 'SUCCESS',
        response_summary: 'Sent mock notification'
      }
    ]
  },
  detections: [],
  createdAt: '2026-09-10T15:30:00Z',
  updatedAt: '2026-09-10T15:30:00Z'
};

const mockHistory: PotholeStatusHistoryResponse[] = [
  {
    id: 'h-1',
    pothole_id: '33333333-4444-5555-6666-777788889999',
    previous_status: null,
    new_status: 'REPORTED',
    changed_at: '2026-09-10T15:30:00Z',
    changed_by: 'SYSTEM',
    notes: 'Initial pothole detection'
  }
];

describe('PotholeDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderComponent = (potholeId: string = '33333333-4444-5555-6666-777788889999') => {
    return render(
      <MemoryRouter initialEntries={[`/potholes/${potholeId}`]}>
        <Routes>
          <Route path="/potholes/:id" element={<PotholeDetailPage />} />
        </Routes>
      </MemoryRouter>
    );
  };

  it('11. renders pothole details, detection metrics, and evidence correctly', async () => {
    vi.mocked(apiClient.getPothole).mockResolvedValue(mockPothole);
    vi.mocked(apiClient.getPotholeHistory).mockResolvedValue(mockHistory);

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/Pothole Inspection Report #33333333/i)).toBeInTheDocument();
    });

    expect(screen.getByText('Rajpath Avenue, New Delhi')).toBeInTheDocument();
    expect(screen.getByText('94.0%')).toBeInTheDocument(); // confidence
    expect(screen.getByText(/HIGH \(62.4\)/i)).toBeInTheDocument(); // severity
    expect(screen.getByAltText('AI Annotated Pothole')).toHaveAttribute(
      'src',
      'http://localhost:9000/pothole-annotated/ann_1.jpg'
    );
  });

  it('12. renders assigned civic authority and dispatch report details', async () => {
    vi.mocked(apiClient.getPothole).mockResolvedValue(mockPothole);
    vi.mocked(apiClient.getPotholeHistory).mockResolvedValue(mockHistory);

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Delhi Public Works Department')).toBeInTheDocument();
      expect(screen.getByText(/PWD-TICKET-2026-001/i)).toBeInTheDocument();
      expect(screen.getByText('DISPATCHED')).toBeInTheDocument();
    });
  });

  it('13. unknown authority renders visible triage alert state', async () => {
    const unknownAuthPothole: PotholeDetailResponse = {
      ...mockPothole,
      authority: null,
      authorityCode: 'UNKNOWN_AUTHORITY',
      report: null
    };

    vi.mocked(apiClient.getPothole).mockResolvedValue(unknownAuthPothole);
    vi.mocked(apiClient.getPotholeHistory).mockResolvedValue(mockHistory);

    renderComponent();

    await waitFor(() => {
      expect(screen.getAllByText(/Authority Requires Triage/i).length).toBeGreaterThan(0);
    });
  });

  it('14. duplicate status renders visible duplicate submission alert', async () => {
    const duplicatePothole: PotholeDetailResponse = {
      ...mockPothole,
      isDuplicate: true,
      duplicateOfId: '99999999-8888-7777-6666-555544443333'
    };

    vi.mocked(apiClient.getPothole).mockResolvedValue(duplicatePothole);
    vi.mocked(apiClient.getPotholeHistory).mockResolvedValue(mockHistory);

    renderComponent();

    await waitFor(() => {
      expect(screen.getAllByText(/Duplicate Submission/i).length).toBeGreaterThan(0);
      expect(screen.getAllByText(/99999999/).length).toBeGreaterThan(0);
    });
  });

  it('15. renders status history audit trail timeline', async () => {
    vi.mocked(apiClient.getPothole).mockResolvedValue(mockPothole);
    vi.mocked(apiClient.getPotholeHistory).mockResolvedValue(mockHistory);

    renderComponent();

    await waitFor(() => {
      expect(screen.getByTestId('status-timeline')).toBeInTheDocument();
      expect(screen.getByText('"Initial pothole detection"')).toBeInTheDocument();
    });
  });

  it('16. status update workflow succeeds and updates view', async () => {
    vi.mocked(apiClient.getPothole).mockResolvedValue(mockPothole);
    vi.mocked(apiClient.getPotholeHistory).mockResolvedValue(mockHistory);

    const updatedPothole: PotholeDetailResponse = {
      ...mockPothole,
      status: 'ACKNOWLEDGED'
    };

    const updatedHistory: PotholeStatusHistoryResponse[] = [
      ...mockHistory,
      {
        id: 'h-2',
        pothole_id: mockPothole.id,
        previous_status: 'REPORTED',
        new_status: 'ACKNOWLEDGED',
        changed_at: '2026-09-10T16:00:00Z',
        changed_by: 'Inspector Rajesh',
        notes: 'Acknowledged and ticket assigned'
      }
    ];

    vi.mocked(apiClient.updateOfficerReportStatus).mockResolvedValue(updatedPothole as any);

    renderComponent();

    await waitFor(() => {
      expect(screen.getByTestId('open-status-modal-button')).toBeInTheDocument();
    });

    // Open modal
    fireEvent.click(screen.getByTestId('open-status-modal-button'));
    expect(screen.getByTestId('status-update-modal')).toBeInTheDocument();

    // Fill changedBy and notes
    fireEvent.change(screen.getByTestId('modal-changed-by-input'), { target: { value: 'Officer Vikram' } });
    fireEvent.change(screen.getByTestId('modal-notes-input'), { target: { value: 'Acknowledged and ticket assigned' } });

    vi.mocked(apiClient.getPotholeHistory).mockResolvedValue(updatedHistory);

    // Confirm transition
    fireEvent.click(screen.getByTestId('modal-confirm-button'));

    await waitFor(() => {
      expect(apiClient.updateOfficerReportStatus).toHaveBeenCalledWith(
        mockPothole.id,
        expect.objectContaining({
          newStatus: 'ACCEPTED',
          changedBy: 'Officer Vikram'
        })
      );
    });

    await waitFor(() => {
      expect(screen.getByText(/Successfully updated status to ACCEPTED/i)).toBeInTheDocument();
    });
  });

  it('17. status update failure shows clear error without breaking view', async () => {
    vi.mocked(apiClient.getPothole).mockResolvedValue(mockPothole);
    vi.mocked(apiClient.getPotholeHistory).mockResolvedValue(mockHistory);

    vi.mocked(apiClient.updateOfficerReportStatus).mockRejectedValue(
      new Error('Invalid status transition from REPORTED to RESOLVED')
    );

    renderComponent();

    await waitFor(() => {
      expect(screen.getByTestId('open-status-modal-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('open-status-modal-button'));

    fireEvent.click(screen.getByTestId('modal-confirm-button'));

    await waitFor(() => {
      expect(screen.getByTestId('modal-error-alert')).toHaveTextContent(
        'Invalid status transition from REPORTED to RESOLVED'
      );
    });
  });
});
