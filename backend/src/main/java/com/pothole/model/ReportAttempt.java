package com.pothole.model;

import com.pothole.model.enums.ReportAttemptStatus;
import com.pothole.model.enums.ReportChannel;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "report_attempts")
public class ReportAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    private ReportChannel channel;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "attempt_timestamp", nullable = false)
    private Instant attemptTimestamp = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReportAttemptStatus status;

    @Column(name = "response_summary", columnDefinition = "TEXT")
    private String responseSummary;

    public ReportAttempt() {
    }

    public ReportAttempt(Report report, Integer attemptNumber, ReportChannel channel, String idempotencyKey, ReportAttemptStatus status, String responseSummary) {
        this.report = report;
        this.attemptNumber = attemptNumber;
        this.channel = channel;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.responseSummary = responseSummary;
        this.attemptTimestamp = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Report getReport() {
        return report;
    }

    public void setReport(Report report) {
        this.report = report;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public ReportChannel getChannel() {
        return channel;
    }

    public void setChannel(ReportChannel channel) {
        this.channel = channel;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getAttemptTimestamp() {
        return attemptTimestamp;
    }

    public void setAttemptTimestamp(Instant attemptTimestamp) {
        this.attemptTimestamp = attemptTimestamp;
    }

    public ReportAttemptStatus getStatus() {
        return status;
    }

    public void setStatus(ReportAttemptStatus status) {
        this.status = status;
    }

    public String getResponseSummary() {
        return responseSummary;
    }

    public void setResponseSummary(String responseSummary) {
        this.responseSummary = responseSummary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReportAttempt that = (ReportAttempt) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ReportAttempt{" +
                "id=" + id +
                ", attemptNumber=" + attemptNumber +
                ", channel=" + channel +
                ", idempotencyKey='" + idempotencyKey + '\'' +
                ", status=" + status +
                ", attemptTimestamp=" + attemptTimestamp +
                '}';
    }
}
