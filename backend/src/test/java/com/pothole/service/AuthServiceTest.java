package com.pothole.service;

import com.pothole.dto.*;
import com.pothole.model.*;
import com.pothole.model.enums.Role;
import com.pothole.model.enums.VerificationStatus;
import com.pothole.repository.*;
import com.pothole.security.JwtTokenProvider;
import com.pothole.service.storage.MinioStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OfficerProfileRepository officerProfileRepository;

    @Mock
    private OfficerJurisdictionRepository officerJurisdictionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private MinioStorageService minioStorageService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("aman.kumar@example.com", "encodedPassword", "Aman Kumar", "+919876543210", Role.ROLE_USER);
        sampleUser.setId(101L);
    }

    @Test
    @DisplayName("Should successfully register a new citizen user")
    void registerUser_Success() {
        RegisterRequest request = new RegisterRequest("aman.kumar@example.com", "password123", "Aman Kumar", "+919876543210");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(tokenProvider.generateToken(any(User.class))).thenReturn("jwt-token-sample");

        AuthResponse response = authService.registerUser(request);

        assertNotNull(response);
        assertEquals("jwt-token-sample", response.getToken());
        assertEquals("aman.kumar@example.com", response.getEmail());
        assertEquals(Role.ROLE_USER, response.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw 409 Conflict when registering user with duplicate email")
    void registerUser_DuplicateEmail_ThrowsConflict() {
        RegisterRequest request = new RegisterRequest("aman.kumar@example.com", "password123", "Aman Kumar", "+919876543210");
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> authService.registerUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully register an officer with ID document upload")
    void registerOfficer_Success() {
        OfficerRegisterRequest request = new OfficerRegisterRequest();
        request.setEmail("officer.sharma@delhipwd.gov.in");
        request.setPassword("officerPass123");
        request.setFullName("Officer Rajesh Sharma");
        request.setDepartment("Delhi PWD");
        request.setOfficerIdCode("PWD-DL-8891");
        request.setJurisdictionName("Delhi Central Circle");
        request.setLatitude(28.6200);
        request.setLongitude(77.2200);
        request.setRadiusKm(10.0);

        MockMultipartFile idCardFile = new MockMultipartFile(
                "idCard", "officer_id.pdf", "application/pdf", "sample-pdf-bytes".getBytes()
        );

        User officerUser = new User(request.getEmail(), "encodedPassword", request.getFullName(), null, Role.ROLE_OFFICER);
        officerUser.setId(202L);

        OfficerProfile profile = new OfficerProfile(officerUser, request.getDepartment(), request.getOfficerIdCode(), "id-cards/key.pdf");
        profile.setId(303L);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(officerUser);
        when(officerProfileRepository.save(any(OfficerProfile.class))).thenReturn(profile);
        when(tokenProvider.generateToken(any(User.class))).thenReturn("jwt-officer-token");

        AuthResponse response = authService.registerOfficer(request, idCardFile);

        assertNotNull(response);
        assertEquals("jwt-officer-token", response.getToken());
        assertEquals(Role.ROLE_OFFICER, response.getRole());
        assertEquals(VerificationStatus.PENDING_VERIFICATION, response.getVerificationStatus());
        verify(minioStorageService).putObject(eq(AuthService.OFFICER_DOCS_BUCKET), anyString(), any(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Should authenticate user and return JWT on valid login")
    void login_Success() {
        AuthRequest request = new AuthRequest("aman.kumar@example.com", "password123");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(tokenProvider.generateToken(sampleUser)).thenReturn("jwt-token-sample");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token-sample", response.getToken());
        assertEquals("aman.kumar@example.com", response.getEmail());
    }
}
