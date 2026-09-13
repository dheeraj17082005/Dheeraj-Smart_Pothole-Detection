import React from 'react';
import { useGeolocation } from '../../hooks/useGeolocation';
import { LoadingSpinner } from '../common/LoadingSpinner';

export interface LocationPickerProps {
  latitude: string;
  longitude: string;
  addressText: string;
  capturedAt: string;
  onLatitudeChange: (val: string) => void;
  onLongitudeChange: (val: string) => void;
  onAddressChange: (val: string) => void;
  onCapturedAtChange: (val: string) => void;
  disabled?: boolean;
}

export const LocationPicker: React.FC<LocationPickerProps> = ({
  latitude,
  longitude,
  addressText,
  capturedAt,
  onLatitudeChange,
  onLongitudeChange,
  onAddressChange,
  onCapturedAtChange,
  disabled = false
}) => {
  const { loading: geoLoading, error: geoError, requestLocation, clearError } = useGeolocation(
    (coords) => {
      onLatitudeChange(coords.latitude.toString());
      onLongitudeChange(coords.longitude.toString());
    }
  );

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
        <h3 style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
          Location &amp; Sighting Telematics
        </h3>
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={() => {
            clearError();
            requestLocation();
          }}
          disabled={disabled || geoLoading}
          data-testid="use-location-button"
        >
          {geoLoading ? (
            <LoadingSpinner size={14} label="Locating GPS..." inline />
          ) : (
            <>📍 Use My Location</>
          )}
        </button>
      </div>

      {geoError && (
        <div
          className="alert alert-warning"
          style={{ padding: '0.6rem 0.85rem', marginBottom: '1rem', fontSize: '0.85rem' }}
          role="alert"
          data-testid="geolocation-error-alert"
        >
          <span>⚠️ {geoError}</span>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
        <div className="form-group">
          <label htmlFor="latitude-input" className="form-label">
            Latitude *
          </label>
          <input
            id="latitude-input"
            type="number"
            step="any"
            className="form-input"
            placeholder="e.g. 28.6200"
            value={latitude}
            onChange={(e) => onLatitudeChange(e.target.value)}
            disabled={disabled}
            data-testid="latitude-input"
            required
          />
          <div className="form-help">Valid range: -90.0 to 90.0</div>
        </div>

        <div className="form-group">
          <label htmlFor="longitude-input" className="form-label">
            Longitude *
          </label>
          <input
            id="longitude-input"
            type="number"
            step="any"
            className="form-input"
            placeholder="e.g. 77.2200"
            value={longitude}
            onChange={(e) => onLongitudeChange(e.target.value)}
            disabled={disabled}
            data-testid="longitude-input"
            required
          />
          <div className="form-help">Valid range: -180.0 to 180.0</div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
        <div className="form-group">
          <label htmlFor="address-input" className="form-label">
            Road Name / Address Description (Optional)
          </label>
          <input
            id="address-input"
            type="text"
            className="form-input"
            placeholder="e.g. Outer Ring Road, near IIT Flyover"
            value={addressText}
            onChange={(e) => onAddressChange(e.target.value)}
            disabled={disabled}
          />
        </div>

        <div className="form-group">
          <label htmlFor="captured-at-input" className="form-label">
            Capture Timestamp (Optional)
          </label>
          <input
            id="captured-at-input"
            type="datetime-local"
            className="form-input"
            value={capturedAt}
            onChange={(e) => onCapturedAtChange(e.target.value)}
            disabled={disabled}
          />
        </div>
      </div>
    </div>
  );
};
