package com.pothole.model;

import com.pothole.model.enums.PotholeStatus;
import com.pothole.model.enums.SeverityClass;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "potholes")
public class Pothole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "location", columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point location;

    @Column(name = "address_text", length = 255)
    private String addressText;

    @Column(name = "first_detected_at", nullable = false)
    private Instant firstDetectedAt = Instant.now();

    @Column(name = "severity_score", nullable = false)
    private Double severityScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity_class", nullable = false, length = 20)
    private SeverityClass severityClass;

    @Column(name = "max_confidence", nullable = false)
    private Double maxConfidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PotholeStatus status = PotholeStatus.REPORTED;

    @Column(name = "is_duplicate", nullable = false)
    private Boolean isDuplicate = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "duplicate_of_id")
    private Pothole duplicateOf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "civic_authority_id")
    private CivicAuthority civicAuthority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by_user_id")
    private User acceptedByUser;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by_user_id")
    private User rejectedByUser;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason", length = 50)
    private com.pothole.model.enums.RejectionReason rejectionReason;

    @Column(name = "rejection_note", columnDefinition = "TEXT")
    private String rejectionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_officer_id")
    private OfficerProfile assignedOfficer;

    @Column(name = "representative_key", nullable = false, length = 255)
    private String representativeKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Pothole() {
    }

    public Pothole(Point location, Double severityScore, SeverityClass severityClass, Double maxConfidence, String representativeKey) {
        this.location = location;
        this.severityScore = severityScore;
        this.severityClass = severityClass;
        this.maxConfidence = maxConfidence;
        this.representativeKey = representativeKey;
        this.status = PotholeStatus.REPORTED;
        this.isDuplicate = false;
        this.firstDetectedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (firstDetectedAt == null) {
            firstDetectedAt = Instant.now();
        }
        if (isDuplicate == null) {
            isDuplicate = false;
        }
        if (status == null) {
            status = PotholeStatus.REPORTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public String getAddressText() {
        return addressText;
    }

    public void setAddressText(String addressText) {
        this.addressText = addressText;
    }

    public Instant getFirstDetectedAt() {
        return firstDetectedAt;
    }

    public void setFirstDetectedAt(Instant firstDetectedAt) {
        this.firstDetectedAt = firstDetectedAt;
    }

    public Double getSeverityScore() {
        return severityScore;
    }

    public void setSeverityScore(Double severityScore) {
        this.severityScore = severityScore;
    }

    public SeverityClass getSeverityClass() {
        return severityClass;
    }

    public void setSeverityClass(SeverityClass severityClass) {
        this.severityClass = severityClass;
    }

    public Double getMaxConfidence() {
        return maxConfidence;
    }

    public void setMaxConfidence(Double maxConfidence) {
        this.maxConfidence = maxConfidence;
    }

    public PotholeStatus getStatus() {
        return status;
    }

    public void setStatus(PotholeStatus status) {
        this.status = status;
    }

    public Boolean getIsDuplicate() {
        return isDuplicate;
    }

    public void setIsDuplicate(Boolean isDuplicate) {
        this.isDuplicate = isDuplicate;
    }

    public Pothole getDuplicateOf() {
        return duplicateOf;
    }

    public void setDuplicateOf(Pothole duplicateOf) {
        this.duplicateOf = duplicateOf;
    }

    public CivicAuthority getCivicAuthority() {
        return civicAuthority;
    }

    public void setCivicAuthority(CivicAuthority civicAuthority) {
        this.civicAuthority = civicAuthority;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public User getAcceptedByUser() {
        return acceptedByUser;
    }

    public void setAcceptedByUser(User acceptedByUser) {
        this.acceptedByUser = acceptedByUser;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public User getRejectedByUser() {
        return rejectedByUser;
    }

    public void setRejectedByUser(User rejectedByUser) {
        this.rejectedByUser = rejectedByUser;
    }

    public Instant getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(Instant rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public com.pothole.model.enums.RejectionReason getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(com.pothole.model.enums.RejectionReason rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRejectionNote() {
        return rejectionNote;
    }

    public void setRejectionNote(String rejectionNote) {
        this.rejectionNote = rejectionNote;
    }

    public OfficerProfile getAssignedOfficer() {
        return assignedOfficer;
    }

    public void setAssignedOfficer(OfficerProfile assignedOfficer) {
        this.assignedOfficer = assignedOfficer;
    }

    public String getRepresentativeKey() {
        return representativeKey;
    }

    public void setRepresentativeKey(String representativeKey) {
        this.representativeKey = representativeKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pothole pothole = (Pothole) o;
        return id != null && Objects.equals(id, pothole.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Pothole{" +
                "id=" + id +
                ", severityClass=" + severityClass +
                ", severityScore=" + severityScore +
                ", status=" + status +
                ", isDuplicate=" + isDuplicate +
                ", representativeKey='" + representativeKey + '\'' +
                '}';
    }
}
