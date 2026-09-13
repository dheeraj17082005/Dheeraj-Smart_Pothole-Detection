import { useState, useEffect, useRef, useCallback } from 'react';
import { JobDetailResponse } from '../types';
import { apiClient } from '../services/api';

export interface UseDetectionJobOptions {
  pollingIntervalMs?: number;
  maxAttempts?: number;
}

export interface UseDetectionJobReturn {
  job: JobDetailResponse | null;
  loading: boolean;
  error: string | null;
  isPolling: boolean;
  pollCount: number;
  reset: () => void;
}

export function useDetectionJob(
  jobId: string | null,
  options: UseDetectionJobOptions = {}
): UseDetectionJobReturn {
  const { pollingIntervalMs = 1000, maxAttempts = 60 } = options;

  const [job, setJob] = useState<JobDetailResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [isPolling, setIsPolling] = useState<boolean>(false);
  const [pollCount, setPollCount] = useState<number>(0);

  const timerRef = useRef<number | null>(null);
  const attemptsRef = useRef<number>(0);
  const consecutiveErrorsRef = useRef<number>(0);

  const reset = useCallback(() => {
    if (timerRef.current !== null) {
      window.clearTimeout(timerRef.current);
      timerRef.current = null;
    }
    setJob(null);
    setLoading(false);
    setError(null);
    setIsPolling(false);
    setPollCount(0);
    attemptsRef.current = 0;
    consecutiveErrorsRef.current = 0;
  }, []);

  useEffect(() => {
    if (!jobId) {
      reset();
      return;
    }

    let isMounted = true;
    attemptsRef.current = 0;
    consecutiveErrorsRef.current = 0;
    setLoading(true);
    setError(null);
    setIsPolling(true);

    const poll = async () => {
      attemptsRef.current += 1;
      if (isMounted) {
        setPollCount(attemptsRef.current);
      }

      if (attemptsRef.current > maxAttempts) {
        if (isMounted) {
          setError('Job polling timed out before completion. The job may still be processing in the background.');
          setIsPolling(false);
          setLoading(false);
        }
        return;
      }

      try {
        const result = await apiClient.getDetectionJob(jobId);
        if (!isMounted) return;

        consecutiveErrorsRef.current = 0;
        setJob(result);
        setLoading(false);

        if (result.status === 'COMPLETED' || result.status === 'FAILED') {
          setIsPolling(false);
          if (result.status === 'FAILED') {
            setError(result.error || 'Video detection job failed during processing.');
          }
          return;
        }

        // Continue polling if still PENDING or PROCESSING
        timerRef.current = window.setTimeout(poll, pollingIntervalMs);
      } catch (err: any) {
        if (!isMounted) return;

        consecutiveErrorsRef.current += 1;
        // Allow up to 3 consecutive network retries before failing
        if (consecutiveErrorsRef.current >= 3) {
          setError(err.message || 'Failed to poll detection job after multiple attempts.');
          setIsPolling(false);
          setLoading(false);
        } else {
          timerRef.current = window.setTimeout(poll, pollingIntervalMs);
        }
      }
    };

    poll();

    return () => {
      isMounted = false;
      if (timerRef.current !== null) {
        window.clearTimeout(timerRef.current);
        timerRef.current = null;
      }
    };
  }, [jobId, pollingIntervalMs, maxAttempts, reset]);

  return {
    job,
    loading,
    error,
    isPolling,
    pollCount,
    reset
  };
}
