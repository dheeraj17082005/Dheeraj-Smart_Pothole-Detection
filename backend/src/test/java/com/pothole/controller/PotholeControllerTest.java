package com.pothole.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pothole.dto.*;
import com.pothole.exception.GlobalExceptionHandler;
import com.pothole.exception.ObjectNotFoundException;
import com.pothole.model.enums.PotholeStatus;
import com.pothole.model.enums.SeverityClass;
import com.pothole.service.query.PotholeQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PotholeControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PotholeQueryService queryService;

    @InjectMocks
    private PotholeController controller;

    private UUID potholeId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        potholeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /api/v1/potholes/map returns 200 OK with marker list")
    void testGetPotholesMapSuccess() throws Exception {
        PotholeMapMarkerResponse marker = new PotholeMapMarkerResponse(
                potholeId,
                28.6139,
                77.2090,
                SeverityClass.HIGH,
                65.0,
                PotholeStatus.REPORTED,
                0.91,
                Instant.now(),
                "NDMC Central",
                "NDMC_CENTRAL",
                false
        );

        when(queryService.getPotholesMap(eq(28.0), eq(77.0), eq(29.0), eq(78.0), any()))
                .thenReturn(List.of(marker));

        mockMvc.perform(get("/api/v1/potholes/map")
                        .param("minLat", "28.0")
                        .param("minLng", "77.0")
                        .param("maxLat", "29.0")
                        .param("maxLng", "78.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(potholeId.toString()))
                .andExpect(jsonPath("$[0].severity_class").value("HIGH"))
                .andExpect(jsonPath("$[0].status").value("REPORTED"))
                .andExpect(jsonPath("$[0].authority_code").value("NDMC_CENTRAL"));
    }

    @Test
    @DisplayName("GET /api/v1/potholes/map returns 400 Bad Request when coordinates are invalid")
    void testGetPotholesMapInvalidCoordinates() throws Exception {
        mockMvc.perform(get("/api/v1/potholes/map")
                        .param("minLat", "95.0") // invalid > 90
                        .param("minLng", "77.0")
                        .param("maxLat", "29.0")
                        .param("maxLng", "78.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request"));
    }

    @Test
    @DisplayName("GET /api/v1/potholes returns 200 OK paginated page")
    void testGetPotholesPageSuccess() throws Exception {
        PotholeResponse pothole = new PotholeResponse(
                potholeId,
                28.6139,
                77.2090,
                "Rajpath Area",
                Instant.now(),
                45.0,
                SeverityClass.MEDIUM,
                0.88,
                PotholeStatus.REPORTED,
                false,
                null,
                new AuthorityResponse(UUID.randomUUID(), "NDMC Central", "NDMC_CENTRAL", "MUNICIPAL"),
                "NDMC_CENTRAL",
                "rep/key.jpg",
                "http://localhost:9000/pothole-annotated/rep.jpg",
                Collections.emptyList(),
                Instant.now(),
                Instant.now()
        );

        when(queryService.getPotholes(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(pothole), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/potholes")
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "REPORTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(potholeId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/potholes/{id} returns 200 OK with full detail")
    void testGetPotholeDetailSuccess() throws Exception {
        PotholeDetailResponse detail = new PotholeDetailResponse(
                potholeId,
                28.6139,
                77.2090,
                "Rajpath Area",
                Instant.now(),
                45.0,
                SeverityClass.MEDIUM,
                0.88,
                PotholeStatus.REPORTED,
                false,
                null,
                new AuthorityResponse(UUID.randomUUID(), "NDMC Central", "NDMC_CENTRAL", "MUNICIPAL"),
                "NDMC_CENTRAL",
                new PotholeDetailResponse.PotholeEvidence("http://localhost:9000/ann.jpg", "rep/key.jpg", "http://localhost:9000/raw.jpg", "raw/key.jpg", "IMAGE"),
                null,
                Collections.emptyList(),
                Instant.now(),
                Instant.now()
        );

        when(queryService.getPotholeDetail(potholeId)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/potholes/{id}", potholeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(potholeId.toString()))
                .andExpect(jsonPath("$.evidence.representativeImageUrl").value("http://localhost:9000/ann.jpg"))
                .andExpect(jsonPath("$.authorityCode").value("NDMC_CENTRAL"));
    }

    @Test
    @DisplayName("GET /api/v1/potholes/{id} returns 404 when pothole does not exist")
    void testGetPotholeDetailNotFound() throws Exception {
        when(queryService.getPotholeDetail(potholeId))
                .thenThrow(new ObjectNotFoundException("Pothole not found with ID: " + potholeId));

        mockMvc.perform(get("/api/v1/potholes/{id}", potholeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Object Not Found"));
    }

    @Test
    @DisplayName("GET /api/v1/potholes/{id}/history returns 200 OK with audit items")
    void testGetPotholeHistorySuccess() throws Exception {
        PotholeStatusHistoryResponse item = new PotholeStatusHistoryResponse(
                UUID.randomUUID(),
                potholeId,
                PotholeStatus.REPORTED,
                PotholeStatus.ACKNOWLEDGED,
                Instant.now(),
                "Inspector Rajesh",
                "Acknowledged by PWD"
        );

        when(queryService.getPotholeHistory(potholeId)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/potholes/{id}/history", potholeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pothole_id").value(potholeId.toString()))
                .andExpect(jsonPath("$[0].previous_status").value("REPORTED"))
                .andExpect(jsonPath("$[0].new_status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$[0].changed_by").value("Inspector Rajesh"));
    }

    @Test
    @DisplayName("PATCH /api/v1/potholes/{id}/status returns 200 OK with updated detail")
    void testUpdatePotholeStatusSuccess() throws Exception {
        UpdatePotholeStatusRequest request = new UpdatePotholeStatusRequest(
                PotholeStatus.ACKNOWLEDGED,
                "Inspector Rajesh",
                "Work order created"
        );

        PotholeDetailResponse detail = new PotholeDetailResponse(
                potholeId,
                28.6139,
                77.2090,
                "Rajpath Area",
                Instant.now(),
                45.0,
                SeverityClass.MEDIUM,
                0.88,
                PotholeStatus.ACKNOWLEDGED,
                false,
                null,
                null,
                "UNKNOWN_AUTHORITY",
                null,
                null,
                Collections.emptyList(),
                Instant.now(),
                Instant.now()
        );

        when(queryService.updatePotholeStatus(eq(potholeId), any())).thenReturn(detail);

        mockMvc.perform(patch("/api/v1/potholes/{id}/status", potholeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/potholes/{id}/status returns 400 when invalid transition requested")
    void testUpdatePotholeStatusInvalidTransition() throws Exception {
        UpdatePotholeStatusRequest request = new UpdatePotholeStatusRequest(
                PotholeStatus.RESOLVED,
                "Inspector Rajesh",
                "Skipping straight to resolved"
        );

        when(queryService.updatePotholeStatus(eq(potholeId), any()))
                .thenThrow(new IllegalArgumentException("Invalid status transition from REPORTED to RESOLVED."));

        mockMvc.perform(patch("/api/v1/potholes/{id}/status", potholeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Invalid status transition")));
    }

    @Test
    @DisplayName("GET /api/v1/authorities returns 200 OK list of authorities")
    void testGetAuthoritiesSuccess() throws Exception {
        AuthorityResponse auth = new AuthorityResponse(UUID.randomUUID(), "NDMC Central", "NDMC_CENTRAL", "MUNICIPAL");
        when(queryService.getAllAuthorities()).thenReturn(List.of(auth));

        mockMvc.perform(get("/api/v1/authorities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("NDMC Central"))
                .andExpect(jsonPath("$[0].code").value("NDMC_CENTRAL"));
    }
}
