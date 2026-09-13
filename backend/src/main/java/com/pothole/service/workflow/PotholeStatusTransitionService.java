package com.pothole.service.workflow;

import com.pothole.model.*;
import com.pothole.model.enums.NotificationType;
import com.pothole.model.enums.PotholeStatus;
import com.pothole.model.enums.RejectionReason;
import com.pothole.model.enums.VerificationStatus;
import com.pothole.repository.PotholeRepository;
import com.pothole.repository.PotholeStatusHistoryRepository;
import com.pothole.service.NotificationService;
import com.pothole.service.OfficerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PotholeStatusTransitionService {

    private static final Logger log = LoggerFactory.getLogger(PotholeStatusTransitionService.class);

    private final PotholeRepository potholeRepository;
    private final PotholeStatusHistoryRepository historyRepository;
    private final OfficerService officerService;
    private final NotificationService notificationService;

    public PotholeStatusTransitionService(
            PotholeRepository potholeRepository,
            PotholeStatusHistoryRepository historyRepository,
            OfficerService officerService,
            NotificationService notificationService
    ) {
        this.potholeRepository = potholeRepository;
        this.historyRepository = historyRepository;
        this.officerService = officerService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Pothole acceptReport(User officerUser, UUID potholeId, String notes) {
        OfficerProfile officerProfile = validateVerifiedOfficer(officerUser);
        Pothole pothole = getPotholeAndValidateJurisdiction(officerUser, officerProfile, potholeId);

        PotholeStatus current = pothole.getStatus();
        if (current != PotholeStatus.SUBMITTED && current != PotholeStatus.PENDING_OFFICER_REVIEW && current != PotholeStatus.REPORTED) {
            throw new IllegalArgumentException("Cannot accept report in status: " + current + ". Must be SUBMITTED or PENDING_OFFICER_REVIEW.");
        }

        pothole.setStatus(PotholeStatus.ACCEPTED);
        pothole.setAcceptedByUser(officerUser);
        pothole.setAcceptedAt(Instant.now());
        pothole.setAssignedOfficer(officerProfile);

        Pothole saved = potholeRepository.save(pothole);
        recordHistory(saved, current, PotholeStatus.ACCEPTED, officerUser, notes != null ? notes : "Report accepted by officer.");

        // Send notification to Citizen creator
        User creator = saved.getCreatedByUser() != null ? saved.getCreatedByUser() : saved.getUser();
        if (creator != null) {
            String shortId = saved.getId().toString().substring(0, 8);
            notificationService.createNotification(
                    creator,
                    saved,
                    "Report Accepted",
                    "Your pothole report #" + shortId + " was accepted by Officer " + officerUser.getFullName() + ".",
                    NotificationType.REPORT_ACCEPTED
            );
        }

        return saved;
    }

    @Transactional
    public Pothole rejectReport(User officerUser, UUID potholeId, RejectionReason reason, String notes) {
        OfficerProfile officerProfile = validateVerifiedOfficer(officerUser);
        Pothole pothole = getPotholeAndValidateJurisdiction(officerUser, officerProfile, potholeId);

        PotholeStatus current = pothole.getStatus();
        if (current != PotholeStatus.SUBMITTED && current != PotholeStatus.PENDING_OFFICER_REVIEW && current != PotholeStatus.REPORTED && current != PotholeStatus.ACCEPTED) {
            throw new IllegalArgumentException("Cannot reject report in status: " + current);
        }

        pothole.setStatus(PotholeStatus.REJECTED);
        pothole.setRejectedByUser(officerUser);
        pothole.setRejectedAt(Instant.now());
        pothole.setRejectionReason(reason);
        pothole.setRejectionNote(notes);

        Pothole saved = potholeRepository.save(pothole);
        String historyNote = "Report rejected. Reason: " + reason + (notes != null ? " - " + notes : "");
        recordHistory(saved, current, PotholeStatus.REJECTED, officerUser, historyNote);

        // Send notification to Citizen creator
        User creator = saved.getCreatedByUser() != null ? saved.getCreatedByUser() : saved.getUser();
        if (creator != null) {
            String shortId = saved.getId().toString().substring(0, 8);
            notificationService.createNotification(
                    creator,
                    saved,
                    "Report Rejected",
                    "Your pothole report #" + shortId + " was rejected by Officer " + officerUser.getFullName() + ". Reason: " + reason,
                    NotificationType.REPORT_REJECTED
            );
        }

        return saved;
    }

    @Transactional
    public Pothole transitionStatus(User officerUser, UUID potholeId, PotholeStatus targetStatus, String notes) {
        OfficerProfile officerProfile = validateVerifiedOfficer(officerUser);
        Pothole pothole = getPotholeAndValidateJurisdiction(officerUser, officerProfile, potholeId);

        PotholeStatus current = pothole.getStatus();

        // Validate state machine
        if (targetStatus == PotholeStatus.IN_PROGRESS) {
            if (current != PotholeStatus.ACCEPTED && current != PotholeStatus.ACKNOWLEDGED && current != PotholeStatus.REPORTED) {
                throw new IllegalArgumentException("Cannot transition to IN_PROGRESS from current status: " + current);
            }
        } else if (targetStatus == PotholeStatus.RESOLVED) {
            if (current != PotholeStatus.IN_PROGRESS) {
                throw new IllegalArgumentException("Cannot mark RESOLVED unless report is IN_PROGRESS. Current status: " + current);
            }
        } else {
            throw new IllegalArgumentException("Unsupported status transition to: " + targetStatus);
        }

        pothole.setStatus(targetStatus);
        Pothole saved = potholeRepository.save(pothole);
        recordHistory(saved, current, targetStatus, officerUser, notes);

        // Send notification to Citizen creator
        User creator = saved.getCreatedByUser() != null ? saved.getCreatedByUser() : saved.getUser();
        if (creator != null) {
            String shortId = saved.getId().toString().substring(0, 8);
            NotificationType type = (targetStatus == PotholeStatus.RESOLVED) ? NotificationType.REPORT_RESOLVED : NotificationType.WORK_STARTED;
            String title = (targetStatus == PotholeStatus.RESOLVED) ? "Pothole Resolved" : "Work Started";
            String msg = (targetStatus == PotholeStatus.RESOLVED)
                    ? "Your reported pothole #" + shortId + " has been marked resolved."
                    : "Remediation work has started on your reported pothole #" + shortId + ".";

            notificationService.createNotification(creator, saved, title, msg, type);
        }

        return saved;
    }

    private OfficerProfile validateVerifiedOfficer(User user) {
        if (user == null || user.getRole() != com.pothole.model.enums.Role.ROLE_OFFICER) {
            throw new AccessDeniedException("Only government officers can perform official report status actions.");
        }
        OfficerProfile profile = officerService.getOfficerProfileEntity(user);
        if (profile == null || profile.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new AccessDeniedException("Officer profile is not VERIFIED. Official operations are locked.");
        }
        return profile;
    }

    private Pothole getPotholeAndValidateJurisdiction(User officerUser, OfficerProfile officerProfile, UUID potholeId) {
        Pothole pothole = potholeRepository.findById(potholeId)
                .orElseThrow(() -> new IllegalArgumentException("Pothole not found: " + potholeId));

        boolean inJurisdiction = officerService.isPotholeInOfficerJurisdiction(officerProfile, pothole);
        if (!inJurisdiction) {
            throw new AccessDeniedException("Access Denied: Report #" + potholeId + " is outside your assigned spatial jurisdiction.");
        }
        return pothole;
    }

    private void recordHistory(Pothole pothole, PotholeStatus prevStatus, PotholeStatus newStatus, User changedBy, String notes) {
        String changedByName = (changedBy != null && changedBy.getFullName() != null) ? changedBy.getFullName() : "SYSTEM";
        PotholeStatusHistory history = new PotholeStatusHistory(pothole, prevStatus, newStatus, changedByName, notes);
        historyRepository.save(history);
    }
}
