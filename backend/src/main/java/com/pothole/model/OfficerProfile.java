package com.pothole.model;

import com.pothole.model.enums.VerificationStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "officer_profiles")
public class OfficerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String department;

    @Column(name = "officer_id_code", nullable = false)
    private String officerIdCode;

    @Column(name = "id_card_object_key", nullable = false)
    private String idCardObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING_VERIFICATION;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public OfficerProfile() {
    }

    public OfficerProfile(User user, String department, String officerIdCode, String idCardObjectKey) {
        this.user = user;
        this.department = department;
        this.officerIdCode = officerIdCode;
        this.idCardObjectKey = idCardObjectKey;
        this.verificationStatus = VerificationStatus.PENDING_VERIFICATION;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getOfficerIdCode() {
        return officerIdCode;
    }

    public void setOfficerIdCode(String officerIdCode) {
        this.officerIdCode = officerIdCode;
    }

    public String getIdCardObjectKey() {
        return idCardObjectKey;
    }

    public void setIdCardObjectKey(String idCardObjectKey) {
        this.idCardObjectKey = idCardObjectKey;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public OffsetDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(OffsetDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OfficerProfile profile = (OfficerProfile) o;
        return id != null && id.equals(profile.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
