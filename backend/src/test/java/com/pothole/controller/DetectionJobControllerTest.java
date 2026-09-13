package com.pothole.controller;

import com.pothole.dto.JobDetailResponse;
import com.pothole.dto.JobResultSummary;
import com.pothole.exception.GlobalExceptionHandler;
import com.pothole.exception.ObjectNotFoundException;
import com.pothole.model.enums.DetectionJobStatus;
import com.pothole.service.detection.VideoDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DetectionJobControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VideoDetectionService videoDetectionService;

    @InjectMocks
    private DetectionJobController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/detection-jobs/{id} returns 200 OK with completed job details and result summary")
    void testGetCompletedJobStatus() throws Exception {
        UUID jobId = UUID.randomUUID();
        JobResultSummary summary = new JobResultSummary(20, 6, 2, 0);
        JobDetailResponse response = new JobDetailResponse(
                jobId,
                DetectionJobStatus.COMPLETED,
                Instant.now().minusSeconds(10),
                Instant.now(),
                1.0,
                summary,
                null
        );

        when(videoDetectionService.getJobDetail(jobId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/detection-jobs/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progress").value(1.0))
                .andExpect(jsonPath("$.result.totalFramesSampled").value(20))
                .andExpect(jsonPath("$.result.framesWithPotholes").value(6))
                .andExpect(jsonPath("$.result.potholesCreated").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/detection-jobs/{id} returns 404 Not Found when job does not exist")
    void testGetJobStatusNotFound() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(videoDetectionService.getJobDetail(jobId))
                .thenThrow(new ObjectNotFoundException("Detection job not found with ID: " + jobId));

        mockMvc.perform(get("/api/v1/detection-jobs/{jobId}", jobId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Object Not Found"));
    }
}
