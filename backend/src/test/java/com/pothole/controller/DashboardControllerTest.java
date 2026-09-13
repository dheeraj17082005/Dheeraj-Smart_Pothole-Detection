package com.pothole.controller;

import com.pothole.dto.dashboard.DashboardStatsResponse;
import com.pothole.exception.GlobalExceptionHandler;
import com.pothole.service.dashboard.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/stats returns 200 OK with correct counters")
    void testGetDashboardStats() throws Exception {
        DashboardStatsResponse stats = new DashboardStatsResponse(25, 10, 5, 6, 4, 8);
        when(dashboardService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPotholes").value(25))
                .andExpect(jsonPath("$.reportedCount").value(10))
                .andExpect(jsonPath("$.acknowledgedCount").value(5))
                .andExpect(jsonPath("$.inProgressCount").value(6))
                .andExpect(jsonPath("$.resolvedCount").value(4))
                .andExpect(jsonPath("$.highSeverityCount").value(8));
    }
}
