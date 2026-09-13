package com.pothole.controller;

import com.pothole.dto.*;
import com.pothole.model.Pothole;
import com.pothole.model.User;
import com.pothole.service.AuthService;
import com.pothole.service.OfficerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/officer")
public class OfficerController {

    private final OfficerService officerService;
    private final AuthService authService;
    private final com.pothole.service.workflow.PotholeStatusTransitionService transitionService;

    public OfficerController(
            OfficerService officerService,
            AuthService authService,
            com.pothole.service.workflow.PotholeStatusTransitionService transitionService
    ) {
        this.officerService = officerService;
        this.authService = authService;
        this.transitionService = transitionService;
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAuthority('ROLE_OFFICER')")
    public ResponseEntity<OfficerProfileResponse> getProfile(Authentication authentication) {
        User user = authService.getUserByEmail(authentication.getName());
        OfficerProfileResponse response = officerService.getOfficerProfile(user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAuthority('ROLE_OFFICER')")
    public ResponseEntity<List<PotholeResponse>> getReportsInJurisdiction(Authentication authentication) {
        User user = authService.getUserByEmail(authentication.getName());
        List<PotholeResponse> reports = officerService.getReportsInJurisdiction(user);
        return ResponseEntity.ok(reports);
    }

    @PostMapping("/reports/{id}/accept")
    @PreAuthorize("hasAuthority('ROLE_OFFICER')")
    public ResponseEntity<PotholeResponse> acceptReport(
            Authentication authentication,
            @PathVariable("id") UUID potholeId,
            @RequestParam(value = "notes", required = false) String notes
    ) {
        User user = authService.getUserByEmail(authentication.getName());
        Pothole updated = transitionService.acceptReport(user, potholeId, notes);
        return ResponseEntity.ok(officerService.mapToPotholeResponse(updated));
    }

    @PostMapping("/reports/{id}/reject")
    @PreAuthorize("hasAuthority('ROLE_OFFICER')")
    public ResponseEntity<PotholeResponse> rejectReport(
            Authentication authentication,
            @PathVariable("id") UUID potholeId,
            @Valid @RequestBody RejectReportRequest request
    ) {
        User user = authService.getUserByEmail(authentication.getName());
        Pothole updated = transitionService.rejectReport(user, potholeId, request.reason(), request.notes());
        return ResponseEntity.ok(officerService.mapToPotholeResponse(updated));
    }

    @PatchMapping("/reports/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_OFFICER')")
    public ResponseEntity<PotholeResponse> updateReportStatus(
            Authentication authentication,
            @PathVariable("id") UUID potholeId,
            @Valid @RequestBody UpdatePotholeStatusRequest request
    ) {
        User user = authService.getUserByEmail(authentication.getName());
        Pothole updated = transitionService.transitionStatus(user, potholeId, request.newStatus(), request.notes());
        return ResponseEntity.ok(officerService.mapToPotholeResponse(updated));
    }

    @GetMapping("/admin/pending")
    public ResponseEntity<List<OfficerProfileResponse>> getPendingOfficers() {
        List<OfficerProfileResponse> pending = officerService.getPendingOfficers();
        return ResponseEntity.ok(pending);
    }

    @PatchMapping("/admin/verify/{id}")
    public ResponseEntity<OfficerProfileResponse> verifyOfficer(
            @PathVariable("id") Long officerProfileId,
            @Valid @RequestBody VerifyOfficerRequest request
    ) {
        OfficerProfileResponse verified = officerService.verifyOfficer(
                officerProfileId, request.getVerificationStatus(), request.getVerifiedBy()
        );
        return ResponseEntity.ok(verified);
    }
}
