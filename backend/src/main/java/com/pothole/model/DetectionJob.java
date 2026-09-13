package com.pothole.model;

import com.pothole.model.enums.DetectionJobStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "detection_jobs")
public class DetectionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_asset_id", nullable = false)
    private MediaAsset mediaAsset;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DetectionJobStatus status = DetectionJobStatus.PENDING;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    public DetectionJob() {
    }

    public DetectionJob(MediaAsset mediaAsset, DetectionJobStatus status) {
        this.mediaAsset = mediaAsset;
        this.status = (status != null) ? status : DetectionJobStatus.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public MediaAsset getMediaAsset() {
        return mediaAsset;
    }

    public void setMediaAsset(MediaAsset mediaAsset) {
        this.mediaAsset = mediaAsset;
    }

    public DetectionJobStatus getStatus() {
        return status;
    }

    public void setStatus(DetectionJobStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public void setErrorSummary(String errorSummary) {
        this.errorSummary = errorSummary;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetectionJob that = (DetectionJob) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "DetectionJob{" +
                "id=" + id +
                ", status=" + status +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                '}';
    }
}
