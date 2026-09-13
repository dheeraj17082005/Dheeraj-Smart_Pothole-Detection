package com.pothole.service;

import com.pothole.dto.OfficerProfileResponse;
import com.pothole.dto.PotholeResponse;
import com.pothole.model.*;
import com.pothole.model.enums.PotholeStatus;
import com.pothole.model.enums.Role;
import com.pothole.model.enums.SeverityClass;
import com.pothole.model.enums.VerificationStatus;
import com.pothole.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfficerServiceTest {

    @Mock
    private OfficerProfileRepository officerProfileRepository;

    @Mock
    private OfficerJurisdictionRepository officerJurisdictionRepository;

    @Mock
    private PotholeRepository potholeRepository;

    @Mock
    private PotholeStatusHistoryRepository statusHistoryRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OfficerService officerService;

    private User officerUser;
    private OfficerProfile verifiedProfile;
    private OfficerProfile pendingProfile;
    private OfficerJurisdiction jurisdiction;
    private Pothole samplePothole;
    private GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @BeforeEach
    void setUp() {
        officerUser = new User("officer@delhipwd.gov.in", "pass", "Rajesh Sharma", "+919876543210", Role.ROLE_OFFICER);
        officerUser.setId(10L);

        verifiedProfile = new OfficerProfile(officerUser, "Delhi PWD", "PWD-DL-100", "docs/key.pdf");
        verifiedProfile.setId(20L);
        verifiedProfile.setVerificationStatus(VerificationStatus.VERIFIED);

        pendingProfile = new OfficerProfile(officerUser, "Delhi PWD", "PWD-DL-100", "docs/key.pdf");
        pendingProfile.setId(21L);
        pendingProfile.setVerificationStatus(VerificationStatus.PENDING_VERIFICATION);

        Point officePoint = geometryFactory.createPoint(new Coordinate(77.2200, 28.6200));
        jurisdiction = new OfficerJurisdiction(verifiedProfile, "Delhi Central", officePoint, 10.0);
        jurisdiction.setId(30L);

        Point potholePoint = geometryFactory.createPoint(new Coordinate(77.2210, 28.6210));
        samplePothole = new Pothole(potholePoint, 7.5, SeverityClass.HIGH, 0.92, "rep-key.jpg");
        samplePothole.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should return reports within jurisdiction when officer is verified")
    void getReportsInJurisdiction_VerifiedOfficer_Success() {
        when(officerProfileRepository.findByUser(officerUser)).thenReturn(Optional.of(verifiedProfile));
        when(officerJurisdictionRepository.findFirstByOfficerProfileIdAndActiveTrue(20L)).thenReturn(Optional.of(jurisdiction));
        when(potholeRepository.findWithinRadius(eq(77.2200), eq(28.6200), eq(10000.0))).thenReturn(List.of(samplePothole));

        List<PotholeResponse> results = officerService.getReportsInJurisdiction(officerUser);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(samplePothole.getId(), results.get(0).id());
    }

    @Test
    @DisplayName("Should throw 403 Forbidden when officer account is pending verification")
    void getReportsInJurisdiction_PendingOfficer_Throws403() {
        when(officerProfileRepository.findByUser(officerUser)).thenReturn(Optional.of(pendingProfile));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> officerService.getReportsInJurisdiction(officerUser));
        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("pending verification"));
    }

    @Test
    @DisplayName("Should successfully update report status when report is within spatial jurisdiction")
    void updateReportStatus_WithinJurisdiction_Success() {
        UUID potholeId = samplePothole.getId();
        when(officerProfileRepository.findByUser(officerUser)).thenReturn(Optional.of(verifiedProfile));
        when(officerJurisdictionRepository.findFirstByOfficerProfileIdAndActiveTrue(20L)).thenReturn(Optional.of(jurisdiction));
        when(potholeRepository.isReportWithinJurisdiction(eq(potholeId), eq(77.2200), eq(28.6200), eq(10000.0))).thenReturn(true);
        when(potholeRepository.findById(potholeId)).thenReturn(Optional.of(samplePothole));
        when(potholeRepository.save(any(Pothole.class))).thenAnswer(i -> i.getArgument(0));

        PotholeResponse updated = officerService.updateReportStatusInJurisdiction(officerUser, potholeId, PotholeStatus.IN_PROGRESS, "Work order assigned");

        assertNotNull(updated);
        assertEquals(PotholeStatus.IN_PROGRESS, updated.status());
        verify(statusHistoryRepository).save(any(PotholeStatusHistory.class));
    }

    @Test
    @DisplayName("Should throw 403 Forbidden when report is outside officer spatial jurisdiction")
    void updateReportStatus_OutsideJurisdiction_Throws403() {
        UUID potholeId = samplePothole.getId();
        when(officerProfileRepository.findByUser(officerUser)).thenReturn(Optional.of(verifiedProfile));
        when(officerJurisdictionRepository.findFirstByOfficerProfileIdAndActiveTrue(20L)).thenReturn(Optional.of(jurisdiction));
        when(potholeRepository.isReportWithinJurisdiction(eq(potholeId), eq(77.2200), eq(28.6200), eq(10000.0))).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> officerService.updateReportStatusInJurisdiction(officerUser, potholeId, PotholeStatus.RESOLVED, "Done"));
        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("outside officer spatial jurisdiction"));
        verify(potholeRepository, never()).save(any(Pothole.class));
    }

    @Test
    @DisplayName("Should verify officer status and notify user")
    void verifyOfficer_Success() {
        when(officerProfileRepository.findById(20L)).thenReturn(Optional.of(verifiedProfile));
        when(officerProfileRepository.save(any(OfficerProfile.class))).thenAnswer(i -> i.getArgument(0));

        OfficerProfileResponse response = officerService.verifyOfficer(20L, VerificationStatus.VERIFIED, "ADMIN_USER");

        assertNotNull(response);
        assertEquals(VerificationStatus.VERIFIED, response.getVerificationStatus());
        verify(notificationService).createNotification(eq(officerUser), any(), anyString(), anyString(), any());
    }
}
