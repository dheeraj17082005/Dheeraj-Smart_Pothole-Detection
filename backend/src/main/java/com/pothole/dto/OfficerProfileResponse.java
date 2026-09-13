package com.pothole.dto;

import com.pothole.model.enums.VerificationStatus;
import java.time.OffsetDateTime;

public class OfficerProfileResponse {

    private Long id;
    private Long userId;
    private String email;
    private String fullName;
    private String phone;
    private String department;
    private String officerIdCode;
    private VerificationStatus verificationStatus;
    private String jurisdictionName;
    private Double officeLatitude;
    private Double officeLongitude;
    private Double radiusKm;
    private OffsetDateTime verifiedAt;
    private String verifiedBy;

    public OfficerProfileResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getJurisdictionName() {
        return jurisdictionName;
    }

    public void setJurisdictionName(String jurisdictionName) {
        this.jurisdictionName = jurisdictionName;
    }

    public Double getOfficeLatitude() {
        return officeLatitude;
    }

    public void setOfficeLatitude(Double officeLatitude) {
        this.officeLatitude = officeLatitude;
    }

    public Double getOfficeLongitude() {
        return officeLongitude;
    }

    public void setOfficeLongitude(Double officeLongitude) {
        this.officeLongitude = officeLongitude;
    }

    public Double getRadiusKm() {
        return radiusKm;
    }

    public void setRadiusKm(Double radiusKm) {
        this.radiusKm = radiusKm;
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
}
