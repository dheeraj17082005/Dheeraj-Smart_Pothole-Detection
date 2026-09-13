package com.pothole.dto;

import com.pothole.model.enums.VerificationStatus;
import jakarta.validation.constraints.NotNull;

public class VerifyOfficerRequest {

    @NotNull(message = "Verification status is required")
    private VerificationStatus verificationStatus;

    private String verifiedBy;

    public VerifyOfficerRequest() {
    }

    public VerifyOfficerRequest(VerificationStatus verificationStatus, String verifiedBy) {
        this.verificationStatus = verificationStatus;
        this.verifiedBy = verifiedBy;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }
}
