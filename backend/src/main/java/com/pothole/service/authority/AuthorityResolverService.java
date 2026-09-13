package com.pothole.service.authority;

import com.pothole.model.CivicAuthority;
import com.pothole.repository.AuthorityJurisdictionRepository;
import com.pothole.repository.CivicAuthorityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthorityResolverService {

    private static final Logger log = LoggerFactory.getLogger(AuthorityResolverService.class);
    public static final double ROAD_NETWORK_BUFFER_METERS = 15.0;

    private final AuthorityJurisdictionRepository jurisdictionRepository;
    private final CivicAuthorityRepository civicAuthorityRepository;

    public AuthorityResolverService(
            AuthorityJurisdictionRepository jurisdictionRepository,
            CivicAuthorityRepository civicAuthorityRepository
    ) {
        this.jurisdictionRepository = jurisdictionRepository;
        this.civicAuthorityRepository = civicAuthorityRepository;
    }

    /**
     * Resolves the responsible civic authority for a given GPS coordinate (latitude, longitude).
     *
     * Hierarchy:
     * 1. Road Ownership Layer (ST_DWithin on ROAD_NETWORK within 15 meters)
     * 2. Municipal Jurisdiction Layer (ST_Covers on MUNICIPAL_BOUNDARY)
     * 3. UNKNOWN_AUTHORITY (Fallback when coordinate lies outside all registered boundaries)
     *
     * @param latitude  GPS Latitude (-90.0 to 90.0)
     * @param longitude GPS Longitude (-180.0 to 180.0)
     * @return AuthorityResolutionResult holding the resolved authority or UNKNOWN_AUTHORITY
     */
    @Transactional(readOnly = true)
    public AuthorityResolutionResult resolveAuthority(double latitude, double longitude) {
        log.debug("Resolving civic authority for coordinates: lat={}, lon={}", latitude, longitude);

        // 1. Layer 1: Road Ownership Check (15m buffer using PostGIS geography ST_DWithin)
        Optional<UUID> roadAuthorityId = jurisdictionRepository.findRoadAuthorityIdNearby(
                longitude,
                latitude,
                ROAD_NETWORK_BUFFER_METERS
        );

        if (roadAuthorityId.isPresent()) {
            CivicAuthority authority = civicAuthorityRepository.findById(roadAuthorityId.get()).orElse(null);
            if (authority != null) {
                log.info("Resolved dedicated road authority [{}] ({}) for coordinates [{}, {}]",
                        authority.getName(), authority.getCode(), latitude, longitude);
                return AuthorityResolutionResult.resolved(authority);
            }
        }

        // 2. Layer 2: Municipal Boundary Check (ST_Covers on municipal polygon)
        Optional<UUID> municipalAuthorityId = jurisdictionRepository.findMunicipalAuthorityIdCovering(
                longitude,
                latitude
        );

        if (municipalAuthorityId.isPresent()) {
            CivicAuthority authority = civicAuthorityRepository.findById(municipalAuthorityId.get()).orElse(null);
            if (authority != null) {
                log.info("Resolved municipal authority [{}] ({}) for coordinates [{}, {}]",
                        authority.getName(), authority.getCode(), latitude, longitude);
                return AuthorityResolutionResult.resolved(authority);
            }
        }

        // 3. Fallback: Coordinate is outside all spatial layers
        log.info("No spatial authority match found for coordinates [{}, {}]. Assigning UNKNOWN_AUTHORITY.",
                latitude, longitude);
        return AuthorityResolutionResult.unknown();
    }
}
