import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { UploadPage } from './UploadPage';
import { apiClient } from '../services/api';
import { DetectImageResponse, DetectVideoAcceptedResponse } from '../types';

describe('UploadPage Component Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  const renderUploadPage = () => {
    return render(
      <MemoryRouter>
        <UploadPage />
      </MemoryRouter>
    );
  };

  it('Requirement 1: Upload page renders with headings, mode switch, and dropzone', () => {
    renderUploadPage();

    expect(screen.getByRole('heading', { name: /report road surface defect/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /image/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /video/i })).toBeInTheDocument();
    expect(screen.getByTestId('file-dropzone')).toBeInTheDocument();
    expect(screen.getByTestId('latitude-input')).toBeInTheDocument();
    expect(screen.getByTestId('longitude-input')).toBeInTheDocument();
    expect(screen.getByTestId('submit-button')).toBeInTheDocument();
  });

  it('Requirement 4: Submit button is disabled when required fields are missing', async () => {
    renderUploadPage();

    const submitBtn = screen.getByTestId('submit-button');
    // Initially no file or coordinates
    expect(submitBtn).toBeDisabled();

    // Enter only latitude
    const latInput = screen.getByTestId('latitude-input');
    await userEvent.type(latInput, '28.62');
    expect(submitBtn).toBeDisabled();

    // Enter only longitude
    const lonInput = screen.getByTestId('longitude-input');
    await userEvent.type(lonInput, '77.22');
    // Still no file selected
    expect(submitBtn).toBeDisabled();

    // Attach valid image
    const validFile = new File(['dummy content'], 'road.jpg', { type: 'image/jpeg' });
    const fileInput = screen.getByTestId('file-input');
    await userEvent.upload(fileInput, validFile);

    // Now all required fields are present
    expect(submitBtn).toBeEnabled();
  });

  it('Requirement 2: Invalid image is rejected with validation message', async () => {
    renderUploadPage();

    // Upload an invalid image extension / MIME type (e.g. .pdf or .txt)
    const invalidFile = new File(['text'], 'report.txt', { type: 'text/plain' });
    const fileInput = screen.getByTestId('file-input');
    fireEvent.change(fileInput, { target: { files: [invalidFile] } });

    expect(screen.getByText(/unsupported image format/i)).toBeInTheDocument();
    expect(screen.getByTestId('submit-button')).toBeDisabled();
  });

  it('Requirement 3: Invalid video is rejected when in video mode', async () => {
    renderUploadPage();

    // Switch to video mode
    const videoTab = screen.getByRole('tab', { name: /video/i });
    await userEvent.click(videoTab);

    // Upload an image or unsupported file in video mode
    const invalidVideo = new File(['dummy'], 'image.png', { type: 'image/png' });
    const fileInput = screen.getByTestId('file-input');
    fireEvent.change(fileInput, { target: { files: [invalidVideo] } });

    expect(screen.getByText(/unsupported video format/i)).toBeInTheDocument();
    expect(screen.getByTestId('submit-button')).toBeDisabled();
  });

  it('Requirement 5: Successful image API response renders results, metrics, authority, and evidence image', async () => {
    const mockImageResponse: DetectImageResponse = {
      job: {
        id: 'job-img-001',
        status: 'COMPLETED',
        started_at: '2026-09-11T10:00:00Z',
        completed_at: '2026-09-11T10:00:01Z'
      },
      media_asset_id: 'media-asset-001',
      pothole_count: 1,
      pothole_created: true,
      pothole: {
        id: 'pothole-uuid-001',
        latitude: 28.62,
        longitude: 77.22,
        address_text: 'Ring Road, Delhi',
        severity_score: 42.5,
        severity_class: 'MEDIUM',
        max_confidence: 0.885,
        status: 'REPORTED',
        is_duplicate: false,
        authority: {
          id: 'auth-001',
          name: 'Delhi Public Works Department',
          code: 'DEMO_PWD_ARTERIAL',
          department_type: 'PWD'
        },
        representative_image_url: 'http://localhost:9000/presigned/annotated.jpg',
        detections: [
          {
            id: 'det-001',
            box: { xmin: 100, ymin: 100, xmax: 300, ymax: 300 },
            confidence: 0.885,
            visual_area_ratio: 0.042
          }
        ]
      }
    };

    const detectImageMock = vi
      .spyOn(apiClient, 'detectImage')
      .mockResolvedValueOnce(mockImageResponse);

    renderUploadPage();

    // Fill form
    const validFile = new File(['fake-jpeg'], 'pothole.jpg', { type: 'image/jpeg' });
    await userEvent.upload(screen.getByTestId('file-input'), validFile);
    await userEvent.type(screen.getByTestId('latitude-input'), '28.62');
    await userEvent.type(screen.getByTestId('longitude-input'), '77.22');

    // Submit
    const submitBtn = screen.getByTestId('submit-button');
    expect(submitBtn).toBeEnabled();
    await userEvent.click(submitBtn);

    // Wait for result view
    await waitFor(() => {
      expect(screen.getByTestId('image-detection-result')).toBeInTheDocument();
    });

    expect(detectImageMock).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId('pothole-count')).toHaveTextContent('1');
    expect(screen.getByTestId('max-confidence')).toHaveTextContent('88.5%');
    expect(screen.getByTestId('authority-name')).toHaveTextContent('Delhi Public Works Department');
    expect(screen.getByText(/DEMO_PWD_ARTERIAL/)).toBeInTheDocument();
    expect(screen.getByTestId('annotated-image')).toHaveAttribute(
      'src',
      'http://localhost:9000/presigned/annotated.jpg'
    );
    expect(screen.getByText(/UNIQUE DEFECT/i)).toBeInTheDocument();
  });

  it('Requirement 6: API failure renders an error alert with user-friendly message', async () => {
    vi.spyOn(apiClient, 'detectImage').mockRejectedValueOnce(
      new Error('AI inference service is temporarily unreachable')
    );

    renderUploadPage();

    const validFile = new File(['fake-jpeg'], 'pothole.jpg', { type: 'image/jpeg' });
    await userEvent.upload(screen.getByTestId('file-input'), validFile);
    await userEvent.type(screen.getByTestId('latitude-input'), '28.62');
    await userEvent.type(screen.getByTestId('longitude-input'), '77.22');

    await userEvent.click(screen.getByTestId('submit-button'));

    await waitFor(() => {
      expect(screen.getByText(/AI inference service is temporarily unreachable/i)).toBeInTheDocument();
    });
  });

  it('Requirement 7: Video upload returns 202 Accepted and renders VideoJobTracker', async () => {
    const mockAcceptedResponse: DetectVideoAcceptedResponse = {
      jobId: 'video-job-777',
      status: 'PENDING',
      mediaAssetId: 'asset-777',
      pollUrl: '/api/v1/detection-jobs/video-job-777'
    };

    const detectVideoMock = vi
      .spyOn(apiClient, 'detectVideo')
      .mockResolvedValueOnce(mockAcceptedResponse);

    // Mock initial job poll response
    vi.spyOn(apiClient, 'getDetectionJob').mockResolvedValueOnce({
      jobId: 'video-job-777',
      status: 'PENDING'
    });

    renderUploadPage();

    // Switch to video mode
    await userEvent.click(screen.getByRole('tab', { name: /video/i }));

    const validVideo = new File(['fake-mp4'], 'dashcam.mp4', { type: 'video/mp4' });
    await userEvent.upload(screen.getByTestId('file-input'), validVideo);
    await userEvent.type(screen.getByTestId('latitude-input'), '28.62');
    await userEvent.type(screen.getByTestId('longitude-input'), '77.22');

    await userEvent.click(screen.getByTestId('submit-button'));

    await waitFor(() => {
      expect(screen.getByTestId('video-job-tracker')).toBeInTheDocument();
    });

    expect(detectVideoMock).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId('job-id')).toHaveTextContent('video-job-777');
  });
});
