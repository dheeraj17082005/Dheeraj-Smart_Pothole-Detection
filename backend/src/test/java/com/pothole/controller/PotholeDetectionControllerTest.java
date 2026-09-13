package com.pothole.controller;

import com.pothole.dto.*;
import com.pothole.dto.ai.AiBoundingBox;
import com.pothole.exception.GlobalExceptionHandler;
import com.pothole.exception.InvalidMediaException;
import com.pothole.model.enums.DetectionJobStatus;
import com.pothole.model.enums.PotholeStatus;
import com.pothole.model.enums.SeverityClass;
import com.pothole.service.detection.ImageDetectionService;
import com.pothole.model.User;
import com.pothole.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PotholeDetectionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ImageDetectionService imageDetectionService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private PotholeDetectionController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/potholes/detect-image returns 200 OK with PotholeResponse including authority and deduplication fields")
    void testDetectImageSuccess() throws Exception {
        UUID potholeId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        UUID authorityId = UUID.randomUUID();

        AuthorityResponse authResp = new AuthorityResponse(authorityId, "Delhi PWD", "PWD", "PWD");

        PotholeResponse potholeResponse = new PotholeResponse(
                potholeId,
                12.9716,
                77.5946,
                "MG Road, Bengaluru",
                Instant.now(),
                0.0,
                SeverityClass.LOW,
                0.885,
                PotholeStatus.REPORTED,
                false,
                null,
                authResp,
                "PWD",
                "annotated/2026/09/rep.jpg",
                "http://localhost:9000/annotated/2026/09/rep.jpg",
                List.of(new DetectionResponse(
                        UUID.randomUUID(),
                        new AiBoundingBox(100, 100, 300, 300),
                        0.885,
                        0.042,
                        0,
                        0.0
                )),
                Instant.now(),
                Instant.now()
        );

        JobResponse jobResponse = new JobResponse(jobId, DetectionJobStatus.COMPLETED, Instant.now(), Instant.now(), null);
        DetectImageResponse response = new DetectImageResponse(
                jobResponse, mediaId, 1, true, potholeResponse, "Pothole detected and registered successfully."
        );

        when(imageDetectionService.processImageDetection(any(), any())).thenReturn(response);
        User mockUser = new User();
        mockUser.setEmail("citizen@test.com");
        when(authService.getUserByEmail(any())).thenReturn(mockUser);

        MockMultipartFile file = new MockMultipartFile("file", "pothole.jpg", "image/jpeg", new byte[]{1, 2, 3});
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "citizen@test.com", "password", List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        mockMvc.perform(multipart("/api/v1/potholes/detect-image")
                        .file(file)
                        .principal(auth)
                        .param("latitude", "12.9716")
                        .param("longitude", "77.5946")
                        .param("addressText", "MG Road, Bengaluru")
                        .param("confidenceThreshold", "0.35"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pothole_count").value(1))
                .andExpect(jsonPath("$.pothole_created").value(true))
                .andExpect(jsonPath("$.pothole.id").value(potholeId.toString()))
                .andExpect(jsonPath("$.pothole.latitude").value(12.9716))
                .andExpect(jsonPath("$.pothole.longitude").value(77.5946))
                .andExpect(jsonPath("$.pothole.status").value("REPORTED"))
                .andExpect(jsonPath("$.pothole.is_duplicate").value(false))
                .andExpect(jsonPath("$.pothole.isDuplicate").value(false))
                .andExpect(jsonPath("$.pothole.authority.name").value("Delhi PWD"))
                .andExpect(jsonPath("$.pothole.authority_code").value("PWD"))
                .andExpect(jsonPath("$.job.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/potholes/detect-image returns 400 BAD_REQUEST on invalid media")
    void testDetectImageInvalidMedia() throws Exception {
        User mockUser = new User();
        mockUser.setEmail("citizen@test.com");
        when(authService.getUserByEmail(any())).thenReturn(mockUser);
        when(imageDetectionService.processImageDetection(any(), any()))
                .thenThrow(new InvalidMediaException("Unsupported image content type"));

        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1, 2});
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "citizen@test.com", "password", List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        mockMvc.perform(multipart("/api/v1/potholes/detect-image")
                        .file(file)
                        .principal(auth)
                        .param("latitude", "12.9716")
                        .param("longitude", "77.5946"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Media"))
                .andExpect(jsonPath("$.detail").value("Unsupported image content type"));
    }
}
