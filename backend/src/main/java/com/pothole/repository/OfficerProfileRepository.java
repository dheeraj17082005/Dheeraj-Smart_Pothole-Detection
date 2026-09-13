package com.pothole.repository;

import com.pothole.model.OfficerProfile;
import com.pothole.model.User;
import com.pothole.model.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfficerProfileRepository extends JpaRepository<OfficerProfile, Long> {
    Optional<OfficerProfile> findByUser(User user);
    Optional<OfficerProfile> findByUserId(Long userId);
    List<OfficerProfile> findByVerificationStatus(VerificationStatus verificationStatus);
}
