import { useState, useCallback } from 'react';

export interface GeolocationCoordinates {
  latitude: number;
  longitude: number;
  accuracy?: number;
}

export interface UseGeolocationReturn {
  loading: boolean;
  error: string | null;
  coordinates: GeolocationCoordinates | null;
  requestLocation: () => void;
  clearError: () => void;
}

export function useGeolocation(
  onSuccess?: (coords: GeolocationCoordinates) => void
): UseGeolocationReturn {
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [coordinates, setCoordinates] = useState<GeolocationCoordinates | null>(null);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  const requestLocation = useCallback(() => {
    if (!navigator.geolocation) {
      setError('Geolocation is not supported by your browser.');
      return;
    }

    setLoading(true);
    setError(null);

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLoading(false);
        const coords: GeolocationCoordinates = {
          latitude: parseFloat(position.coords.latitude.toFixed(6)),
          longitude: parseFloat(position.coords.longitude.toFixed(6)),
          accuracy: position.coords.accuracy
        };
        setCoordinates(coords);
        if (onSuccess) {
          onSuccess(coords);
        }
      },
      (geoError) => {
        setLoading(false);
        let message = 'An unknown error occurred while retrieving your location.';
        switch (geoError.code) {
          case geoError.PERMISSION_DENIED:
            message = 'Location access was denied. Please allow location permissions or enter coordinates manually.';
            break;
          case geoError.POSITION_UNAVAILABLE:
            message = 'Location information is currently unavailable. Please enter coordinates manually.';
            break;
          case geoError.TIMEOUT:
            message = 'Location request timed out. Please try again or enter coordinates manually.';
            break;
        }
        setError(message);
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      }
    );
  }, [onSuccess]);

  return {
    loading,
    error,
    coordinates,
    requestLocation,
    clearError
  };
}
