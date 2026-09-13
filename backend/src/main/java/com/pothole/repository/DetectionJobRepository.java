package com.pothole.repository;

import com.pothole.model.DetectionJob;
import com.pothole.model.enums.DetectionJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DetectionJobRepository extends JpaRepository<DetectionJob, UUID> {
    Optional<DetectionJob> findByMediaAssetId(UUID mediaAssetId);
    List<DetectionJob> findByStatus(DetectionJobStatus status);
}
