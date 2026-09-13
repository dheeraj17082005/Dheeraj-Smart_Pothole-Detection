package com.pothole.repository;

import com.pothole.model.PotholeStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PotholeStatusHistoryRepository extends JpaRepository<PotholeStatusHistory, UUID> {
    List<PotholeStatusHistory> findByPotholeIdOrderByChangedAtDesc(UUID potholeId);
}
