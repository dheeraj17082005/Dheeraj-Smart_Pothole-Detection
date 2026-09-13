package com.pothole.service;

import com.pothole.dto.*;
import com.pothole.model.*;
import com.pothole.model.enums.NotificationType;
import com.pothole.model.enums.PotholeStatus;
import com.pothole.model.enums.VerificationStatus;
import com.pothole.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OfficerService {

    private static final Logger log = LoggerFactory.getLogger(OfficerService.class);

    private final OfficerProfileRepository officerProfileRepository;
    private final OfficerJurisdictionRepository officerJurisdictionRepository;
    private final PotholeRepository potholeRepository;
    private final PotholeStatusHistoryRepository statusHistoryRepository;
    private final NotificationService notificationService;

    public OfficerService(
            OfficerProfileRepository officerProfileRepository,
            OfficerJurisdictionRepository officerJurisdictionRepository,
            PotholeRepository potholeRepository,
            PotholeStatusHistoryRepository statusHistoryRepository,
            NotificationService notificationService
    ) {
        this.officerProfileRepository = officerProfileRepository;
        this.officerJurisdictionRepository = officerJurisdictionRepository;
        this.potholeRepository = potholeRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.notificationService = notificationService;
    }

    public OfficerProfileResponse getOfficerProfile(User user) {
        OfficerProfile profile = getOfficerProfileEntity(user);
        return mapToResponse(profile);
    }

    public OfficerProfile getOfficerProfileEntity(User user) {
        return officerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Officer profile not found"));
    }

    public boolean isPotholeInOfficerJurisdiction(OfficerProfile profile, Pothole pothole) {
        if (profile == null || pothole == null || pothole.getLocation() == null) {
            return false;
        }
        return officerJurisdictionRepository.findFirstByOfficerProfileIdAndActiveTrue(profile.getId())
                .map(jurisdiction -> {
                    double lng = jurisdiction.getOfficeLocation().getX();
                    double lat = jurisdiction.getOfficeLocation().getY();
                    double radiusMeters = jurisdiction.getRadiusKm() * 1000.0;
                    return potholeRepository.isReportWithinJurisdiction(pothole.getId(), lng, lat, radiusMeters);
                })
                .orElse(false);
    }

    public List<PotholeResponse> getReportsInJurisdiction(User officerUser) {
        OfficerProfile profile = officerProfileRepository.findByUser(officerUser)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Officer profile not found"));

        if (profile.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Officer account is pending verification or not active");
        }

        OfficerJurisdiction jurisdiction = officerJurisdictionRepository.findFirstByOfficerProfileIdAndActiveTrue(profile.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active jurisdiction not configured for officer"));

        double lng = jurisdiction.getOfficeLocation().getX();
        double lat = jurisdiction.getOfficeLocation().getY();
        double radiusMeters = jurisdiction.getRadiusKm() * 1000.0;

        List<Pothole> potholes = potholeRepository.findWithinRadius(lng, lat, radiusMeters);
        return potholes.stream().map(this::mapToPotholeResponse).collect(Collectors.toList());
    }

    @Transactional
    public PotholeResponse updateReportStatusInJurisdiction(User officerUser, UUID potholeId, PotholeStatus newStatus, String notes) {
        OfficerProfile profile = officerProfileRepository.findByUser(officerUser)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Officer profile not found"));

        if (profile.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Officer account is pending verification or not active");
        }

        OfficerJurisdiction jurisdiction = officerJurisdictionRepository.findFirstByOfficerProfileIdAndActiveTrue(profile.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active jurisdiction not configured for officer"));

        double lng = jurisdiction.getOfficeLocation().getX();
        double lat = jurisdiction.getOfficeLocation().getY();
        double radiusMeters = jurisdiction.getRadiusKm() * 1000.0;

        boolean withinJurisdiction = potholeRepository.isReportWithinJurisdiction(potholeId, lng, lat, radiusMeters);
        if (!withinJurisdiction) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Report " + potholeId + " is outside officer spatial jurisdiction");
        }

        Pothole pothole = potholeRepository.findById(potholeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pothole report not found"));

        PotholeStatus oldStatus = pothole.getStatus();
        pothole.setStatus(newStatus);
        pothole.setAssignedOfficer(profile);
        pothole = potholeRepository.save(pothole);

        PotholeStatusHistory history = new PotholeStatusHistory(
                pothole,
                oldStatus,
                newStatus,
                officerUser.getFullName() + " (" + profile.getDepartment() + ")",
                notes != null ? notes : "Status updated by officer in spatial jurisdiction"
        );
        statusHistoryRepository.save(history);

        if (pothole.getUser() != null) {
            String title = "Pothole Report Status Updated";
            String message = String.format(
                    "Your reported pothole near %s has been updated to %s by %s.",
                    pothole.getAddressText() != null ? pothole.getAddressText() : "location",
                    newStatus,
                    profile.getDepartment()
            );
            notificationService.createNotification(pothole.getUser(), pothole, title, message, NotificationType.STATUS_CHANGE);
        }

        log.info("Report {} status updated to {} by officer {}", potholeId, newStatus, officerUser.getEmail());
        return mapToPotholeResponse(pothole);
    }

    @Transactional
    public OfficerProfileResponse verifyOfficer(Long officerProfileId, VerificationStatus newStatus, String verifiedBy) {
        OfficerProfile profile = officerProfileRepository.findById(officerProfileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Officer profile not found"));

        profile.setVerificationStatus(newStatus);
        profile.setVerifiedAt(OffsetDateTime.now());
        profile.setVerifiedBy(verifiedBy != null ? verifiedBy : "SYSTEM_ADMIN");
        profile = officerProfileRepository.save(profile);

        String title = "Officer Verification Status Update";
        String message = "Your officer profile verification status is now: " + newStatus;
        notificationService.createNotification(profile.getUser(), null, title, message, NotificationType.OFFICER_VERIFIED);

        return mapToResponse(profile);
    }

    public List<OfficerProfileResponse> getPendingOfficers() {
        return officerProfileRepository.findByVerificationStatus(VerificationStatus.PENDING_VERIFICATION).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private OfficerProfileResponse mapToResponse(OfficerProfile profile) {
        OfficerProfileResponse dto = new OfficerProfileResponse();
        dto.setId(profile.getId());
        dto.setUserId(profile.getUser().getId());
        dto.setEmail(profile.getUser().getEmail());
        dto.setFullName(profile.getUser().getFullName());
        dto.setPhone(profile.getUser().getPhone());
        dto.setDepartment(profile.getDepartment());
        dto.setOfficerIdCode(profile.getOfficerIdCode());
        dto.setVerificationStatus(profile.getVerificationStatus());
        dto.setVerifiedAt(profile.getVerifiedAt());
        dto.setVerifiedBy(profile.getVerifiedBy());

        officerJurisdictionRepository.findFirstByOfficerProfileIdAndActiveTrue(profile.getId()).ifPresent(j -> {
            dto.setJurisdictionName(j.getJurisdictionName());
            dto.setOfficeLongitude(j.getOfficeLocation().getX());
            dto.setOfficeLatitude(j.getOfficeLocation().getY());
            dto.setRadiusKm(j.getRadiusKm());
        });

        return dto;
    }

    public PotholeResponse mapToPotholeResponse(Pothole pothole) {
        Double lat = pothole.getLocation() != null ? pothole.getLocation().getY() : null;
        Double lng = pothole.getLocation() != null ? pothole.getLocation().getX() : null;

        AuthorityResponse authorityDto = (pothole.getCivicAuthority() != null)
                ? new AuthorityResponse(
                        pothole.getCivicAuthority().getId(),
                        pothole.getCivicAuthority().getName(),
                        pothole.getCivicAuthority().getCode(),
                        pothole.getCivicAuthority().getDepartmentType()
                  )
                : null;

        String authorityCode = (pothole.getCivicAuthority() != null)
                ? pothole.getCivicAuthority().getCode()
                : "UNKNOWN_AUTHORITY";

        UUID duplicateOfId = (pothole.getDuplicateOf() != null) ? pothole.getDuplicateOf().getId() : null;

        return new PotholeResponse(
                pothole.getId(),
                lat,
                lng,
                pothole.getAddressText(),
                pothole.getFirstDetectedAt(),
                pothole.getSeverityScore(),
                pothole.getSeverityClass(),
                pothole.getMaxConfidence(),
                pothole.getStatus(),
                pothole.getIsDuplicate(),
                duplicateOfId,
                authorityDto,
                authorityCode,
                pothole.getRepresentativeKey(),
                null,
                List.of(),
                pothole.getCreatedAt(),
                pothole.getUpdatedAt()
        );
    }
}
