package com.pothole.dto;

import com.pothole.model.enums.Role;
import com.pothole.model.enums.VerificationStatus;

public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long id;
    private String email;
    private String fullName;
    private Role role;
    private VerificationStatus verificationStatus;

    public AuthResponse() {
    }

    public AuthResponse(String token, Long id, String email, String fullName, Role role, VerificationStatus verificationStatus) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.verificationStatus = verificationStatus;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
}
