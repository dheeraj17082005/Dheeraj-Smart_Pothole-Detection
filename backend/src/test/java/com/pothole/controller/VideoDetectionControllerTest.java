package com.pothole.controller;

import com.pothole.dto.DetectVideoAcceptedResponse;
import com.pothole.exception.GlobalExceptionHandler;
import com.pothole.exception.InvalidMediaException;
import com.pothole.model.enums.DetectionJobStatus;
import com.pothole.service.detection.VideoDetectionService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VideoDetectionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VideoDetectionService videoDetectionService;

    @InjectMocks
    private VideoDetectionController controller;

    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        auth = new UsernamePasswordAuthenticationToken(
                "citizen@test.com", "password", List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("POST /api/v1/potholes/detect-video returns HTTP 202 Accepted with jobId and pollUrl")
    void testDetectVideoReturns202() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        DetectVideoAcceptedResponse acceptedResponse = new DetectVideoAcceptedResponse(
                jobId,
                DetectionJobStatus.PENDING,
                mediaId,
                "/api/v1/detection-jobs/" + jobId
        );

        when(videoDetectionService.initiateVideoJob(any(), any(), any(), any(), any()))
                .thenReturn(acceptedResponse);

        MockMultipartFile videoFile = new MockMultipartFile(
                "file", "dashcam.mp4", "video/mp4", new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/v1/potholes/detect-video")
                        .file(videoFile)
                        .principal(auth)
                        .param("latitude", "28.62")
                        .param("longitude", "77.22")
                        .param("addressText", "Ring Road"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.mediaAssetId").value(mediaId.toString()))
                .andExpect(jsonPath("$.pollUrl").value("/api/v1/detection-jobs/" + jobId));
    }

    @Test
    @DisplayName("POST /api/v1/potholes/detect-video returns HTTP 400 Bad Request on invalid coordinates")
    void testDetectVideoInvalidCoordinates() throws Exception {
        when(videoDetectionService.initiateVideoJob(any(), any(), any(), any(), any()))
                .thenThrow(new InvalidMediaException("Latitude must be a valid number between -90.0 and 90.0."));

        MockMultipartFile videoFile = new MockMultipartFile(
                "file", "dashcam.mp4", "video/mp4", new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/v1/potholes/detect-video")
                        .file(videoFile)
                        .principal(auth)
                        .param("latitude", "150.0")
                        .param("longitude", "77.22"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Media"));
    }
}
