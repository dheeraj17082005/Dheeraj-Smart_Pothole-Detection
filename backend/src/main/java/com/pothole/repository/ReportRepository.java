package com.pothole.repository;

import com.pothole.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    Optional<Report> findByPotholeId(UUID potholeId);

    Optional<Report> findByIdempotencyKey(String idempotencyKey);

    boolean existsByPotholeId(UUID potholeId);

    Optional<Report> findByPotholeIdAndAuthorityId(UUID potholeId, UUID authorityId);

    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.attempts LEFT JOIN FETCH r.authority LEFT JOIN FETCH r.pothole WHERE r.id = :id")
    Optional<Report> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.attempts LEFT JOIN FETCH r.authority LEFT JOIN FETCH r.pothole WHERE r.pothole.id = :potholeId")
    Optional<Report> findByPotholeIdWithDetails(@Param("potholeId") UUID potholeId);
}
