package com.pothole.repository;

import com.pothole.model.AuthorityJurisdiction;
import com.pothole.model.CivicAuthority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthorityJurisdictionRepository extends JpaRepository<AuthorityJurisdiction, UUID> {

    List<AuthorityJurisdiction> findByAuthorityId(UUID authorityId);

    /**
     * Resolves the nearest road authority jurisdiction within the specified distance in meters.
     * Uses PostGIS ST_DWithin with CAST(... AS geography) to calculate real metric distance.
     * Priority ordering: HIGHWAY (NHAI) > PWD > Others, with deterministic ID ordering.
     */
    @Query(value = """
            SELECT a.id FROM civic_authorities a
            JOIN authority_jurisdictions j ON a.id = j.authority_id
            WHERE j.jurisdiction_type = 'ROAD_NETWORK'
              AND ST_DWithin(
                    CAST(j.geometry AS geography),
                    CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
                    :distanceMeters
                  )
            ORDER BY 
              CASE a.department_type 
                WHEN 'HIGHWAY' THEN 1 
                WHEN 'PWD' THEN 2 
                ELSE 3 
              END,
              a.id ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<UUID> findRoadAuthorityIdNearby(
            @Param("longitude") double longitude,
            @Param("latitude") double latitude,
            @Param("distanceMeters") double distanceMeters
    );

    /**
     * Resolves the municipal authority whose boundary covers the specified point.
     * Uses PostGIS ST_Covers so points lying directly on polygon boundaries evaluate to TRUE.
     */
    @Query(value = """
            SELECT a.id FROM civic_authorities a
            JOIN authority_jurisdictions j ON a.id = j.authority_id
            WHERE j.jurisdiction_type = 'MUNICIPAL_BOUNDARY'
              AND ST_Covers(
                    j.geometry,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)
                  )
            ORDER BY a.id ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<UUID> findMunicipalAuthorityIdCovering(
            @Param("longitude") double longitude,
            @Param("latitude") double latitude
    );
}
