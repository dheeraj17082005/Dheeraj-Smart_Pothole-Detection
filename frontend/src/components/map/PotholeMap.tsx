import React, { useState, useEffect, useRef, useCallback } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { PotholeMapMarkerResponse, BoundingBoxParams, SeverityClass } from '../../types';
import { apiClient } from '../../services/api';
import { SeverityBadge, PotholeStatusBadge } from '../common/Badge';
import { formatConfidence, formatDateTime, formatCoordinates } from '../../utils/formatters';
import { useNavigate } from 'react-router-dom';

interface PotholeMapProps {
  initialCenter?: [number, number];
  initialZoom?: number;
  height?: string;
  focusCoords?: [number, number] | null;
  focusPotholeId?: string | null;
  onSelectPothole?: (id: string) => void;
}

// Marker icon generator with high-severity distinction and focus highlighting
function getMarkerIcon(severity: SeverityClass, status: string, isFocused: boolean = false): L.DivIcon {
  let color = '#10B981'; // Green (LOW)
  let symbol = 'L';
  let size = 28;

  if (severity === 'HIGH') {
    color = '#EF4444'; // Red (HIGH)
    symbol = 'H';
    size = isFocused ? 36 : 30;
  } else if (severity === 'MEDIUM') {
    color = '#0284C7'; // Cyan (MEDIUM)
    symbol = 'M';
    size = isFocused ? 34 : 28;
  } else if (isFocused) {
    size = 34;
  }

  const isResolved = status === 'RESOLVED';
  const opacity = isResolved ? 0.6 : 1.0;
  const borderStyle = isFocused ? '3px solid #2563EB' : '2px solid #0B0D0F';
  const glow = isFocused
    ? '0 0 16px rgba(37, 99, 235, 0.9), 0 4px 10px rgba(0,0,0,0.8)'
    : `0 0 10px ${color}80, 0 2px 6px rgba(0,0,0,0.6)`;

  const html = `
    <div style="
      background-color: ${color};
      width: ${size}px;
      height: ${size}px;
      border-radius: 50%;
      border: ${borderStyle};
      box-shadow: ${glow};
      display: flex;
      align-items: center;
      justify-content: center;
      color: #FFFFFF;
      font-size: ${size > 30 ? '13px' : '11px'};
      font-weight: 900;
      opacity: ${opacity};
      cursor: pointer;
      transition: transform 0.15s ease;
      position: relative;
    " title="${severity} Severity - Status: ${status}">
      ${symbol}
      ${isFocused ? '<div style="position:absolute;top:-6px;left:-6px;right:-6px;bottom:-6px;border-radius:50%;border:2px dashed #2563EB;animation:spin 4s linear infinite;"></div>' : ''}
    </div>
  `;

  return L.divIcon({
    html,
    className: 'pothole-leaflet-marker',
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -(size / 2)]
  });
}

// Subcomponent that manages viewport changes and programmatic focus
const MapViewController: React.FC<{
  onBoundsChange: (bounds: BoundingBoxParams) => void;
  focusCoords?: [number, number] | null;
  focusZoom?: number;
}> = ({ onBoundsChange, focusCoords, focusZoom = 16 }) => {
  const map = useMap();

  const reportBounds = useCallback(() => {
    const b = map.getBounds();
    onBoundsChange({
      minLat: b.getSouth(),
      minLng: b.getWest(),
      maxLat: b.getNorth(),
      maxLng: b.getEast()
    });
  }, [map, onBoundsChange]);

  useMapEvents({
    moveend: reportBounds,
    zoomend: reportBounds
  });

  useEffect(() => {
    reportBounds();
  }, [reportBounds]);

  // When focusCoords change, smoothly fly to target location
  useEffect(() => {
    if (focusCoords && focusCoords[0] && focusCoords[1]) {
      map.flyTo(focusCoords, focusZoom, {
        duration: 1.2,
        easeLinearity: 0.25
      });
    }
  }, [focusCoords, focusZoom, map]);

  return null;
};

export const PotholeMap: React.FC<PotholeMapProps> = ({
  initialCenter = [28.6139, 77.2090], // Sensible default (New Delhi / NCR)
  initialZoom = 12,
  height = '520px',
  focusCoords,
  focusPotholeId,
  onSelectPothole
}) => {
  const navigate = useNavigate();
  const containerRef = useRef<HTMLDivElement>(null);
  const [markers, setMarkers] = useState<PotholeMapMarkerResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [hasQueried, setHasQueried] = useState<boolean>(false);
  const [isFullscreen, setIsFullscreen] = useState<boolean>(false);

  // Active center / focus state
  const [currentFocus, setCurrentFocus] = useState<[number, number] | null>(focusCoords || null);

  // Coordinate Search Inputs
  const [searchLat, setSearchLat] = useState<string>('');
  const [searchLng, setSearchLng] = useState<string>('');
  const [coordsSearchError, setCoordsSearchError] = useState<string | null>(null);

  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);
  const markerRefs = useRef<{ [id: string]: L.Marker }>({});

  useEffect(() => {
    if (focusCoords) {
      setCurrentFocus(focusCoords);
    }
  }, [focusCoords]);

  const fetchMarkersForBounds = useCallback((bounds: BoundingBoxParams) => {
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }

    debounceTimerRef.current = setTimeout(async () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
      const controller = new AbortController();
      abortControllerRef.current = controller;

      setLoading(true);
      setError(null);

      try {
        const data = await apiClient.getPotholesMap(bounds, controller.signal);
        setMarkers(data);
        setHasQueried(true);
      } catch (err: any) {
        if (err.name !== 'AbortError') {
          setError(err.message || 'Failed to load map markers.');
        }
      } finally {
        setLoading(false);
      }
    }, 250);
  }, []);

  useEffect(() => {
    return () => {
      if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current);
      if (abortControllerRef.current) abortControllerRef.current.abort();
    };
  }, []);

  // Auto-open popup for focused marker when available
  useEffect(() => {
    if (focusPotholeId && markerRefs.current[focusPotholeId]) {
      const timer = setTimeout(() => {
        markerRefs.current[focusPotholeId]?.openPopup();
      }, 500);
      return () => clearTimeout(timer);
    }
  }, [focusPotholeId, markers]);

  const handleResetView = () => {
    setCurrentFocus([...initialCenter]);
  };

  const handleLocateMe = () => {
    if (!navigator.geolocation) {
      alert('Geolocation is not supported by your browser.');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setCurrentFocus([pos.coords.latitude, pos.coords.longitude]);
      },
      (err) => {
        alert(`Location access failed: ${err.message}`);
      },
      { timeout: 10000, enableHighAccuracy: true }
    );
  };

  const handleCoordinateJump = (e: React.FormEvent) => {
    e.preventDefault();
    setCoordsSearchError(null);
    const lat = parseFloat(searchLat.trim());
    const lng = parseFloat(searchLng.trim());

    if (isNaN(lat) || lat < -90 || lat > 90) {
      setCoordsSearchError('Invalid Latitude (-90 to 90)');
      return;
    }
    if (isNaN(lng) || lng < -180 || lng > 180) {
      setCoordsSearchError('Invalid Longitude (-180 to 180)');
      return;
    }

    setCurrentFocus([lat, lng]);
  };

  const toggleFullscreen = () => {
    if (!containerRef.current) return;
    if (!document.fullscreenElement) {
      containerRef.current.requestFullscreen().then(() => setIsFullscreen(true)).catch(() => {});
    } else {
      document.exitFullscreen().then(() => setIsFullscreen(false)).catch(() => {});
    }
  };

  const handleViewDetails = (potholeId: string) => {
    if (onSelectPothole) {
      onSelectPothole(potholeId);
    } else {
      navigate(`/potholes/${potholeId}`);
    }
  };

  return (
    <div
      ref={containerRef}
      style={{
        position: 'relative',
        width: '100%',
        height: isFullscreen ? '100vh' : height,
        borderRadius: isFullscreen ? '0' : '10px',
        overflow: 'hidden',
        border: isFullscreen ? 'none' : '1px solid var(--border)',
        backgroundColor: '#0B0D0F',
        display: 'flex',
        flexDirection: 'column'
      }}
    >
      {/* Top Map Action Toolbar */}
      <div
        style={{
          position: 'absolute',
          top: '10px',
          left: '10px',
          right: '10px',
          zIndex: 1000,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '0.5rem',
          pointerEvents: 'none'
        }}
      >
        {/* Left: Coordinate Jump Search Bar */}
        <form
          onSubmit={handleCoordinateJump}
          style={{
            pointerEvents: 'auto',
            display: 'flex',
            alignItems: 'center',
            gap: '0.4rem',
            backgroundColor: 'rgba(17, 20, 23, 0.95)',
            padding: '0.35rem 0.6rem',
            borderRadius: '8px',
            border: '1px solid var(--border)',
            boxShadow: '0 4px 14px rgba(0,0,0,0.6)',
            backdropFilter: 'blur(6px)'
          }}
        >
          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--brand-accent)' }}>📍</span>
          <input
            type="number"
            step="any"
            placeholder="Latitude (e.g. 30.7046)"
            value={searchLat}
            onChange={(e) => setSearchLat(e.target.value)}
            style={{
              width: '135px',
              padding: '0.3rem 0.5rem',
              backgroundColor: 'var(--surface-elevated)',
              border: '1px solid var(--border)',
              borderRadius: '4px',
              color: 'var(--text-main)',
              fontSize: '0.75rem',
              fontFamily: 'var(--font-mono)'
            }}
          />
          <input
            type="number"
            step="any"
            placeholder="Longitude (e.g. 76.7179)"
            value={searchLng}
            onChange={(e) => setSearchLng(e.target.value)}
            style={{
              width: '135px',
              padding: '0.3rem 0.5rem',
              backgroundColor: 'var(--surface-elevated)',
              border: '1px solid var(--border)',
              borderRadius: '4px',
              color: 'var(--text-main)',
              fontSize: '0.75rem',
              fontFamily: 'var(--font-mono)'
            }}
          />
          <button
            type="submit"
            style={{
              backgroundColor: 'var(--brand-accent)',
              color: '#0B0D0F',
              border: 'none',
              padding: '0.32rem 0.65rem',
              borderRadius: '4px',
              fontSize: '0.75rem',
              fontWeight: 800,
              cursor: 'pointer',
              textTransform: 'uppercase',
              letterSpacing: '0.04em'
            }}
          >
            Go To Location
          </button>
          {coordsSearchError && (
            <span style={{ fontSize: '0.7rem', color: '#EF4444', fontWeight: 600 }}>{coordsSearchError}</span>
          )}
        </form>

        {/* Right: Map Control Buttons */}
        <div style={{ pointerEvents: 'auto', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <button
            type="button"
            onClick={handleLocateMe}
            title="Locate my position"
            style={{
              backgroundColor: 'rgba(17, 20, 23, 0.95)',
              color: 'var(--text-main)',
              border: '1px solid var(--border)',
              padding: '0.35rem 0.65rem',
              borderRadius: '6px',
              fontSize: '0.75rem',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.35rem',
              boxShadow: '0 4px 12px rgba(0,0,0,0.5)',
              backdropFilter: 'blur(6px)'
            }}
          >
            <span>🎯</span> Locate Me
          </button>

          <button
            type="button"
            onClick={handleResetView}
            title="Reset to default map view"
            style={{
              backgroundColor: 'rgba(17, 20, 23, 0.95)',
              color: 'var(--text-main)',
              border: '1px solid var(--border)',
              padding: '0.35rem 0.65rem',
              borderRadius: '6px',
              fontSize: '0.75rem',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.35rem',
              boxShadow: '0 4px 12px rgba(0,0,0,0.5)',
              backdropFilter: 'blur(6px)'
            }}
          >
            <span>↺</span> Reset View
          </button>

          <button
            type="button"
            onClick={toggleFullscreen}
            title="Toggle fullscreen map view"
            style={{
              backgroundColor: 'rgba(17, 20, 23, 0.95)',
              color: 'var(--text-main)',
              border: '1px solid var(--border)',
              padding: '0.35rem 0.65rem',
              borderRadius: '6px',
              fontSize: '0.75rem',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.35rem',
              boxShadow: '0 4px 12px rgba(0,0,0,0.5)',
              backdropFilter: 'blur(6px)'
            }}
          >
            <span>{isFullscreen ? '✕' : '⛶'}</span> {isFullscreen ? 'Exit' : 'Fullscreen'}
          </button>
        </div>
      </div>

      {/* Interactive Leaflet Map */}
      <MapContainer
        center={initialCenter}
        zoom={initialZoom}
        style={{ width: '100%', height: '100%', backgroundColor: '#0B0D0F' }}
        scrollWheelZoom={true}
        doubleClickZoom={true}
        dragging={true}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright" style="color: #737B82">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        <MapViewController
          onBoundsChange={fetchMarkersForBounds}
          focusCoords={currentFocus}
          focusZoom={16}
        />

        {markers.map((marker) => {
          const isFocused = marker.id === focusPotholeId;
          return (
            <Marker
              key={marker.id}
              position={[marker.latitude, marker.longitude]}
              icon={getMarkerIcon(marker.severity_class, marker.status, isFocused)}
              ref={(ref) => {
                if (ref) markerRefs.current[marker.id] = ref;
              }}
            >
              <Popup minWidth={240}>
                <div style={{ padding: '0.4rem 0.2rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.65rem' }}>
                    <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.82rem', fontWeight: 800, color: 'var(--text-main)' }}>
                      #{marker.id.slice(0, 8)}
                    </span>
                    <PotholeStatusBadge status={marker.status} />
                  </div>

                  <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.65rem' }}>
                    <SeverityBadge severity={marker.severity_class} score={marker.severity_score} />
                    <span style={{ fontSize: '0.72rem', padding: '0.2rem 0.45rem', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '4px', color: 'var(--text-muted)' }}>
                      Conf: {formatConfidence(marker.confidence)}
                    </span>
                  </div>

                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
                    <strong style={{ color: 'var(--text-main)' }}>Coordinates:</strong>{' '}
                    <span style={{ fontFamily: 'var(--font-mono)' }}>{formatCoordinates(marker.latitude, marker.longitude)}</span>
                  </div>

                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
                    <strong style={{ color: 'var(--text-main)' }}>Authority:</strong>{' '}
                    {marker.authority_code === 'UNKNOWN_AUTHORITY' || !marker.authority_name ? (
                      <span style={{ color: '#0284C7', fontWeight: 600 }}>Authority requires triage</span>
                    ) : (
                      <span>{marker.authority_name}</span>
                    )}
                  </div>

                  <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', marginBottom: '0.85rem' }}>
                    Detected: {formatDateTime(marker.first_detected_at)}
                  </div>

                  <button
                    type="button"
                    onClick={() => handleViewDetails(marker.id)}
                    className="btn btn-primary btn-sm btn-block"
                  >
                    Open Inspection Record →
                  </button>
                </div>
              </Popup>
            </Marker>
          );
        })}
      </MapContainer>

      {/* Top-Right Scanning Indicator */}
      {loading && (
        <div
          data-testid="map-loading-indicator"
          style={{
            position: 'absolute',
            top: '56px',
            right: '12px',
            zIndex: 1000,
            background: 'rgba(17, 20, 23, 0.95)',
            padding: '0.35rem 0.75rem',
            borderRadius: '20px',
            fontSize: '0.72rem',
            fontWeight: 700,
            color: 'var(--text-main)',
            boxShadow: '0 4px 12px rgba(0,0,0,0.5)',
            border: '1px solid var(--border)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.45rem',
            backdropFilter: 'blur(4px)'
          }}
        >
          <div
            style={{
              width: '9px',
              height: '9px',
              border: '2px solid var(--brand-accent)',
              borderTopColor: 'transparent',
              borderRadius: '50%',
              animation: 'spin 0.8s linear infinite'
            }}
          />
          Scanning viewport...
        </div>
      )}

      {/* Empty State Banner */}
      {hasQueried && !loading && markers.length === 0 && (
        <div
          data-testid="map-empty-state"
          style={{
            position: 'absolute',
            bottom: '24px',
            left: '50%',
            transform: 'translateX(-50%)',
            zIndex: 1000,
            background: 'rgba(17, 20, 23, 0.94)',
            padding: '0.6rem 1.25rem',
            borderRadius: '8px',
            fontSize: '0.82rem',
            fontWeight: 600,
            color: 'var(--text-muted)',
            boxShadow: '0 4px 16px rgba(0,0,0,0.6)',
            border: '1px solid var(--border)',
            backdropFilter: 'blur(4px)'
          }}
        >
          No recorded road defects in this viewport area.
        </div>
      )}

      {/* Error Banner */}
      {error && (
        <div
          style={{
            position: 'absolute',
            top: '56px',
            left: '50%',
            transform: 'translateX(-50%)',
            zIndex: 1000,
            background: 'rgba(239, 68, 68, 0.25)',
            color: '#FCA5A5',
            padding: '0.45rem 1rem',
            borderRadius: '6px',
            fontSize: '0.8rem',
            fontWeight: 600,
            boxShadow: '0 4px 12px rgba(0,0,0,0.5)',
            border: '1px solid rgba(239, 68, 68, 0.4)',
            backdropFilter: 'blur(4px)'
          }}
        >
          {error}
        </div>
      )}

      {/* Map Legend */}
      <div
        data-testid="map-legend"
        style={{
          position: 'absolute',
          bottom: '12px',
          right: '12px',
          zIndex: 1000,
          background: 'rgba(17, 20, 23, 0.95)',
          padding: '0.65rem 0.85rem',
          borderRadius: '8px',
          boxShadow: '0 4px 12px rgba(0,0,0,0.6)',
          fontSize: '0.72rem',
          color: 'var(--text-muted)',
          display: 'flex',
          flexDirection: 'column',
          gap: '0.35rem',
          border: '1px solid var(--border)',
          backdropFilter: 'blur(6px)'
        }}
      >
        <span style={{ fontWeight: 800, fontSize: '0.68rem', color: 'var(--text-main)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
          DEFECT SEVERITY
        </span>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#EF4444' }} />
          <span>High Severity (H)</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#0284C7' }} />
          <span>Medium Severity (M)</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#10B981' }} />
          <span>Low Severity (L)</span>
        </div>
      </div>
    </div>
  );
};

