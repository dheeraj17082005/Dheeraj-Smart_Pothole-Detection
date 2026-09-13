package com.pothole.repository;

import com.pothole.model.Detection;
import com.pothole.model.DetectionJob;
import com.pothole.model.Pothole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DetectionRepository extends JpaRepository<Detection, UUID> {
    List<Detection> findByDetectionJobId(UUID detectionJobId);
    List<Detection> findByDetectionJob(DetectionJob detectionJob);
    List<Detection> findByPotholeId(UUID potholeId);
    List<Detection> findByPothole(Pothole pothole);
}
