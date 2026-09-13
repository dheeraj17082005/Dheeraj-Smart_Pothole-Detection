package com.pothole.model;

import com.pothole.model.enums.ReportStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "reports", uniqueConstraints = {
        @UniqueConstraint(name = "uq_reports_pothole_authority", columnNames = {"pothole_id", "authority_id"}),
        @UniqueConstraint(name = "uq_reports_idempotency_key", columnNames = {"idempotency_key"})
})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pothole_id", nullable = false)
    private Pothole pothole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authority_id")
    private CivicAuthority authority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "external_reference", length = 255)
    private String externalReference;

    @Column(name = "created_timestamp", nullable = false, updatable = false)
    private Instant createdTimestamp = Instant.now();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("attemptNumber ASC")
    private List<ReportAttempt> attempts = new ArrayList<>();

    public Report() {
    }

    public Report(Pothole pothole, CivicAuthority authority, String idempotencyKey) {
        this.pothole = pothole;
        this.authority = authority;
        this.idempotencyKey = idempotencyKey;
        this.status = ReportStatus.PENDING;
        this.createdTimestamp = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Pothole getPothole() {
        return pothole;
    }

    public void setPothole(Pothole pothole) {
        this.pothole = pothole;
    }

    public CivicAuthority getAuthority() {
        return authority;
    }

    public void setAuthority(CivicAuthority authority) {
        this.authority = authority;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public Instant getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Instant createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public List<ReportAttempt> getAttempts() {
        return attempts;
    }

    public void setAttempts(List<ReportAttempt> attempts) {
        this.attempts = attempts;
    }

    public void addAttempt(ReportAttempt attempt) {
        attempts.add(attempt);
        attempt.setReport(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Report report = (Report) o;
        return id != null && Objects.equals(id, report.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Report{" +
                "id=" + id +
                ", status=" + status +
                ", idempotencyKey='" + idempotencyKey + '\'' +
                ", externalReference='" + externalReference + '\'' +
                ", createdTimestamp=" + createdTimestamp +
                '}';
    }
}
