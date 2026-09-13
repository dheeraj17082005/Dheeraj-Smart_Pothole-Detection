package com.pothole.repository;

import com.pothole.model.OfficerJurisdiction;
import com.pothole.model.OfficerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfficerJurisdictionRepository extends JpaRepository<OfficerJurisdiction, Long> {
    List<OfficerJurisdiction> findByOfficerProfileAndActiveTrue(OfficerProfile officerProfile);
    Optional<OfficerJurisdiction> findFirstByOfficerProfileIdAndActiveTrue(Long officerProfileId);
}
