import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useGeolocation } from './useGeolocation';

describe('useGeolocation Hook', () => {
  const originalGeolocation = navigator.geolocation;

  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
    Object.defineProperty(window.navigator, 'geolocation', {
      value: originalGeolocation,
      configurable: true,
      writable: true
    });
  });

  it('Requirement 10: Browser geolocation success populates coordinates', () => {
    const mockSuccessPosition = {
      coords: {
        latitude: 28.6139,
        longitude: 77.2090,
        accuracy: 10
      }
    };

    const getCurrentPositionMock = vi.fn().mockImplementation((successCallback) => {
      successCallback(mockSuccessPosition);
    });

    Object.defineProperty(window.navigator, 'geolocation', {
      value: { getCurrentPosition: getCurrentPositionMock },
      configurable: true,
      writable: true
    });

    const onSuccess = vi.fn();
    const { result } = renderHook(() => useGeolocation(onSuccess));

    expect(result.current.loading).toBe(false);
    expect(result.current.coordinates).toBeNull();
    expect(result.current.error).toBeNull();

    act(() => {
      result.current.requestLocation();
    });

    expect(getCurrentPositionMock).toHaveBeenCalledTimes(1);
    expect(result.current.loading).toBe(false);
    expect(result.current.coordinates).toEqual({
      latitude: 28.6139,
      longitude: 77.2090,
      accuracy: 10
    });
    expect(result.current.error).toBeNull();
    expect(onSuccess).toHaveBeenCalledWith({
      latitude: 28.6139,
      longitude: 77.2090,
      accuracy: 10
    });
  });

  it('Requirement 11: Browser geolocation failure (permission denied) is handled gracefully', () => {
    const mockError = {
      code: 1, // PERMISSION_DENIED
      PERMISSION_DENIED: 1,
      POSITION_UNAVAILABLE: 2,
      TIMEOUT: 3,
      message: 'User denied Geolocation'
    };

    const getCurrentPositionMock = vi.fn().mockImplementation((_, errorCallback) => {
      errorCallback(mockError);
    });

    Object.defineProperty(window.navigator, 'geolocation', {
      value: { getCurrentPosition: getCurrentPositionMock },
      configurable: true,
      writable: true
    });

    const { result } = renderHook(() => useGeolocation());

    act(() => {
      result.current.requestLocation();
    });

    expect(getCurrentPositionMock).toHaveBeenCalledTimes(1);
    expect(result.current.loading).toBe(false);
    expect(result.current.coordinates).toBeNull();
    expect(result.current.error).toContain('Location access was denied');
  });

  it('handles browser without geolocation support', () => {
    Object.defineProperty(window.navigator, 'geolocation', {
      value: undefined,
      configurable: true,
      writable: true
    });

    const { result } = renderHook(() => useGeolocation());

    act(() => {
      result.current.requestLocation();
    });

    expect(result.current.error).toBe('Geolocation is not supported by your browser.');
  });
});
