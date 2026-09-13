package com.pothole.repository;

import com.pothole.model.ReportAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportAttemptRepository extends JpaRepository<ReportAttempt, UUID> {

    List<ReportAttempt> findByReportIdOrderByAttemptNumberAsc(UUID reportId);

    List<ReportAttempt> findByIdempotencyKey(String idempotencyKey);
}
