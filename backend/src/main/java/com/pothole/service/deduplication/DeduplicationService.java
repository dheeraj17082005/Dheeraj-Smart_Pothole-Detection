package com.pothole.service.deduplication;

import com.pothole.model.Pothole;
import com.pothole.repository.PotholeRepository;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(DeduplicationService.class);

    public static final double SPATIAL_THRESHOLD_METERS = 15.0;
    public static final double SPATIAL_EPSILON_METERS = 0.01; // 1 cm floating-point tolerance for geodesic measurement
    public static final long TEMPORAL_THRESHOLD_DAYS = 7L;

    private final PotholeRepository potholeRepository;

    public DeduplicationService(PotholeRepository potholeRepository) {
        this.potholeRepository = potholeRepository;
    }

    /**
     * Evaluates whether a new pothole detection at the given GPS coordinates and timestamp
     * represents a duplicate of an existing unresolved parent pothole (Type B deduplication).
     *
     * Criteria per docs/ARCHITECTURE.md:
     * - Spatial distance <= 15.0 meters (using PostGIS ST_DWithin on geography with 1cm tolerance)
     * - Temporal window: first_detected_at within the last 7 days (inclusive lower boundary)
     * - Status: Unresolved parent potholes (REPORTED, ACKNOWLEDGED, IN_PROGRESS). RESOLVED potholes are ignored.
     *
     * @param latitude           GPS Latitude (-90.0 to 90.0)
     * @param longitude          GPS Longitude (-180.0 to 180.0)
     * @param detectionTimestamp Time of detection / capture
     * @return DeduplicationResult indicating if a match was found and referencing the parent pothole
     */
    @Transactional(readOnly = true)
    public DeduplicationResult evaluateDuplicate(double latitude, double longitude, Instant detectionTimestamp) {
        Instant effectiveTime = (detectionTimestamp != null) ? detectionTimestamp : Instant.now();
        Instant cutoffTime = effectiveTime.minus(TEMPORAL_THRESHOLD_DAYS, ChronoUnit.DAYS);

        log.debug("Evaluating duplicate for coords [{}, {}] at {} (cutoff: {})",
                latitude, longitude, effectiveTime, cutoffTime);

        Optional<UUID> candidateId = potholeRepository.findNearbyUnresolvedPotholeId(
                longitude,
                latitude,
                cutoffTime,
                effectiveTime,
                SPATIAL_THRESHOLD_METERS + SPATIAL_EPSILON_METERS
        );

        if (candidateId.isPresent()) {
            Pothole parent = potholeRepository.findById(candidateId.get()).orElse(null);
            if (parent != null) {
                log.info("Duplicate pothole detected! Coords [{}, {}] matches parent pothole [{}] (status: {}, firstDetectedAt: {})",
                        latitude, longitude, parent.getId(), parent.getStatus(), parent.getFirstDetectedAt());
                return DeduplicationResult.duplicate(parent);
            }
        }

        log.debug("No active parent pothole found within 15m and 7 days for [{}, {}]. Marked as non-duplicate.",
                latitude, longitude);
        return DeduplicationResult.nonDuplicate();
    }

    /**
     * Overload accepting a JTS Point geometry.
     */
    @Transactional(readOnly = true)
    public DeduplicationResult evaluateDuplicate(Point location, Instant detectionTimestamp) {
        if (location == null) {
            return DeduplicationResult.nonDuplicate();
        }
        return evaluateDuplicate(location.getY(), location.getX(), detectionTimestamp);
    }

    /**
     * Dynamically calculates the confirmation/vote count for a parent pothole by querying
     * the number of linked duplicate child submissions.
     */
    @Transactional(readOnly = true)
    public long getConfirmationCount(UUID parentPotholeId) {
        if (parentPotholeId == null) {
            return 0L;
        }
        return potholeRepository.countByDuplicateOfId(parentPotholeId);
    }
}
