package com.pothole.repository;

import com.pothole.model.Pothole;
import com.pothole.model.enums.PotholeStatus;
import com.pothole.model.enums.SeverityClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PotholeRepository extends JpaRepository<Pothole, UUID>, JpaSpecificationExecutor<Pothole> {

    long countByStatus(PotholeStatus status);

    long countBySeverityClass(SeverityClass severityClass);

    @Query(value = """
            SELECT * FROM potholes p
            WHERE p.location && ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
            ORDER BY p.first_detected_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Pothole> findWithinBoundingBox(
            @Param("minLng") double minLng,
            @Param("minLat") double minLat,
            @Param("maxLng") double maxLng,
            @Param("maxLat") double maxLat,
            @Param("limit") int limit
    );

    List<Pothole> findByStatus(PotholeStatus status);

    List<Pothole> findBySeverityClass(SeverityClass severityClass);

    List<Pothole> findByIsDuplicateFalse();

    List<Pothole> findByDuplicateOfId(UUID duplicateOfId);

    long countByDuplicateOfId(UUID duplicateOfId);

    List<Pothole> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query(value = """
            SELECT * FROM potholes p
            WHERE ST_DWithin(
                    CAST(p.location AS geography),
                    CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
                    :radiusMeters
                  )
            ORDER BY p.first_detected_at DESC
            """, nativeQuery = true)
    List<Pothole> findWithinRadius(
            @Param("longitude") double longitude,
            @Param("latitude") double latitude,
            @Param("radiusMeters") double radiusMeters
    );

    @Query(value = """
            SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END FROM potholes p
            WHERE p.id = :potholeId
              AND ST_DWithin(
                    CAST(p.location AS geography),
                    CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
                    :radiusMeters
                  )
            """, nativeQuery = true)
    boolean isReportWithinJurisdiction(
            @Param("potholeId") UUID potholeId,
            @Param("longitude") double longitude,
            @Param("latitude") double latitude,
            @Param("radiusMeters") double radiusMeters
    );

    /**
     * Finds the nearest active, unresolved candidate pothole within the metric distance and temporal window.
     * Uses PostGIS ST_DWithin on geography for metric measurement (15.0m threshold).
     * Filters for non-duplicate parent potholes in REPORTED, ACKNOWLEDGED, or IN_PROGRESS status.
     * Deterministic selection: Closest spatial distance ASC, then newest first_detected_at DESC, then ID ASC.
     */
    @Query(value = """
            SELECT p.id FROM potholes p
            WHERE (p.is_duplicate = FALSE OR p.duplicate_of_id IS NULL)
              AND p.status IN ('REPORTED', 'ACKNOWLEDGED', 'IN_PROGRESS')
              AND p.first_detected_at >= :cutoffTimestamp
              AND p.first_detected_at <= :detectionTimestamp
              AND ST_DWithin(
                    CAST(p.location AS geography),
                    CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
                    :radiusMeters
                  )
            ORDER BY
              ST_Distance(
                CAST(p.location AS geography),
                CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)
              ) ASC,
              p.first_detected_at DESC,
              p.id ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<UUID> findNearbyUnresolvedPotholeId(
            @Param("longitude") double longitude,
            @Param("latitude") double latitude,
            @Param("cutoffTimestamp") Instant cutoffTimestamp,
            @Param("detectionTimestamp") Instant detectionTimestamp,
            @Param("radiusMeters") double radiusMeters
    );
}
