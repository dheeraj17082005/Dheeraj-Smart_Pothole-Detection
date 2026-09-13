import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useDetectionJob } from './useDetectionJob';
import { apiClient } from '../services/api';
import { JobDetailResponse } from '../types';

describe('useDetectionJob Hook', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  it('Requirement 8: Polling stops when job status is COMPLETED', async () => {
    const pendingJob: JobDetailResponse = {
      jobId: 'job-123',
      status: 'PENDING',
      progress: 0.1
    };

    const processingJob: JobDetailResponse = {
      jobId: 'job-123',
      status: 'PROCESSING',
      progress: 0.5
    };

    const completedJob: JobDetailResponse = {
      jobId: 'job-123',
      status: 'COMPLETED',
      startedAt: '2026-09-11T10:00:00Z',
      completedAt: '2026-09-11T10:00:02Z',
      progress: 1.0,
      result: {
        totalFramesSampled: 4,
        framesWithPotholes: 2,
        potholesCreated: 1,
        duplicatesDetected: 0
      }
    };

    const getDetectionJobMock = vi
      .spyOn(apiClient, 'getDetectionJob')
      .mockResolvedValueOnce(pendingJob)
      .mockResolvedValueOnce(processingJob)
      .mockResolvedValueOnce(completedJob);

    const { result } = renderHook(() =>
      useDetectionJob('job-123', { pollingIntervalMs: 100 })
    );

    // Initial state
    expect(result.current.isPolling).toBe(true);

    // Flush first poll (PENDING)
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0);
    });
    expect(result.current.job?.status).toBe('PENDING');
    expect(result.current.isPolling).toBe(true);

    // Advance for second poll (PROCESSING)
    await act(async () => {
      await vi.advanceTimersByTimeAsync(100);
    });
    expect(result.current.job?.status).toBe('PROCESSING');
    expect(result.current.isPolling).toBe(true);

    // Advance for third poll (COMPLETED)
    await act(async () => {
      await vi.advanceTimersByTimeAsync(100);
    });
    expect(result.current.job?.status).toBe('COMPLETED');
    expect(result.current.isPolling).toBe(false);

    // Advance timer further to ensure polling stopped
    await act(async () => {
      await vi.advanceTimersByTimeAsync(300);
    });
    expect(getDetectionJobMock).toHaveBeenCalledTimes(3);
    expect(result.current.job?.result?.potholesCreated).toBe(1);
    expect(result.current.error).toBeNull();
  });

  it('Requirement 9: Polling stops when job status is FAILED', async () => {
    const pendingJob: JobDetailResponse = {
      jobId: 'job-fail-456',
      status: 'PENDING'
    };

    const failedJob: JobDetailResponse = {
      jobId: 'job-fail-456',
      status: 'FAILED',
      error: 'Corrupt video header'
    };

    const getDetectionJobMock = vi
      .spyOn(apiClient, 'getDetectionJob')
      .mockResolvedValueOnce(pendingJob)
      .mockResolvedValueOnce(failedJob);

    const { result } = renderHook(() =>
      useDetectionJob('job-fail-456', { pollingIntervalMs: 100 })
    );

    expect(result.current.isPolling).toBe(true);

    // First poll (PENDING)
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0);
    });
    expect(result.current.job?.status).toBe('PENDING');

    // Second poll (FAILED)
    await act(async () => {
      await vi.advanceTimersByTimeAsync(100);
    });
    expect(result.current.job?.status).toBe('FAILED');
    expect(result.current.isPolling).toBe(false);
    expect(result.current.error).toBe('Corrupt video header');

    // Ensure polling has stopped
    await act(async () => {
      await vi.advanceTimersByTimeAsync(300);
    });
    expect(getDetectionJobMock).toHaveBeenCalledTimes(2);
  });
});
