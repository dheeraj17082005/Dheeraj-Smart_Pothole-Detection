package com.pothole.model;

import com.pothole.model.enums.PotholeStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "pothole_status_history")
public class PotholeStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pothole_id", nullable = false)
    private Pothole pothole;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private PotholeStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private PotholeStatus newStatus;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt = Instant.now();

    @Column(name = "changed_by", length = 100)
    private String changedBy = "SYSTEM";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public PotholeStatusHistory() {
    }

    public PotholeStatusHistory(Pothole pothole, PotholeStatus previousStatus, PotholeStatus newStatus, String changedBy, String notes) {
        this.pothole = pothole;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedBy = (changedBy != null) ? changedBy : "SYSTEM";
        this.notes = notes;
        this.changedAt = Instant.now();
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

    public PotholeStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(PotholeStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public PotholeStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(PotholeStatus newStatus) {
        this.newStatus = newStatus;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PotholeStatusHistory that = (PotholeStatusHistory) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "PotholeStatusHistory{" +
                "id=" + id +
                ", previousStatus=" + previousStatus +
                ", newStatus=" + newStatus +
                ", changedAt=" + changedAt +
                ", changedBy='" + changedBy + '\'' +
                '}';
    }
}
