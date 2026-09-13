package com.pothole.controller;

import com.pothole.dto.*;
import com.pothole.model.enums.PotholeStatus;
import com.pothole.model.enums.SeverityClass;
import com.pothole.service.query.PotholeQueryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PotholeController {

    private final PotholeQueryService potholeQueryService;

    public PotholeController(PotholeQueryService potholeQueryService) {
        this.potholeQueryService = potholeQueryService;
    }

    /**
     * Bounding box viewport query for Leaflet map markers.
     */
    @GetMapping(value = "/potholes/map", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PotholeMapMarkerResponse>> getPotholesMap(
            @RequestParam("minLat") Double minLat,
            @RequestParam("minLng") Double minLng,
            @RequestParam("maxLat") Double maxLat,
            @RequestParam("maxLng") Double maxLng,
            @RequestParam(value = "limit", required = false, defaultValue = "200") Integer limit
    ) {
        if (minLat == null || minLng == null || maxLat == null || maxLng == null) {
            throw new IllegalArgumentException("Bounding box parameters minLat, minLng, maxLat, maxLng are required.");
        }
        if (minLat < -90.0 || maxLat > 90.0 || minLng < -180.0 || maxLng > 180.0) {
            throw new IllegalArgumentException("Coordinates out of bounds (-90 to 90 lat, -180 to 180 lng).");
        }
        if (minLat > maxLat) {
            throw new IllegalArgumentException("minLat must be less than or equal to maxLat.");
        }

        List<PotholeMapMarkerResponse> markers = potholeQueryService.getPotholesMap(minLat, minLng, maxLat, maxLng, limit);
        return ResponseEntity.ok(markers);
    }

    /**
     * Paginated and filtered query for pothole table and list views.
     */
    @GetMapping(value = "/potholes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PageResponse<PotholeResponse>> getPotholes(
            @RequestParam(value = "status", required = false) PotholeStatus status,
            @RequestParam(value = "severity", required = false) SeverityClass severity,
            @RequestParam(value = "authority", required = false) String authority,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "firstDetectedAt,desc") String sortParam
    ) {
        Sort sort = Sort.by(Sort.Direction.DESC, "firstDetectedAt");
        if (sortParam != null && sortParam.contains(",")) {
            String[] parts = sortParam.split(",");
            Sort.Direction direction = "asc".equalsIgnoreCase(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, parts[0]);
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), sort);
        Page<PotholeResponse> responsePage = potholeQueryService.getPotholes(status, severity, authority, fromDate, toDate, pageable);
        return ResponseEntity.ok(PageResponse.fromPage(responsePage));
    }

    /**
     * Fetch full pothole detail, presigned evidence URLs, and dispatch reports.
     */
    @GetMapping(value = "/potholes/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PotholeDetailResponse> getPotholeDetail(@PathVariable("id") UUID id) {
        PotholeDetailResponse detail = potholeQueryService.getPotholeDetail(id);
        return ResponseEntity.ok(detail);
    }

    /**
     * Retrieve status history audit trail for a pothole.
     */
    @GetMapping(value = "/potholes/{id}/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PotholeStatusHistoryResponse>> getPotholeHistory(@PathVariable("id") UUID id) {
        List<PotholeStatusHistoryResponse> history = potholeQueryService.getPotholeHistory(id);
        return ResponseEntity.ok(history);
    }

    /**
     * Update pothole status and append to status history timeline (Officer only).
     */
    @PatchMapping(value = "/potholes/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_OFFICER')")
    public ResponseEntity<PotholeDetailResponse> updatePotholeStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdatePotholeStatusRequest request
    ) {
        PotholeDetailResponse updated = potholeQueryService.updatePotholeStatus(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Retrieve all civic authorities for filter selection.
     */
    @GetMapping(value = "/authorities", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AuthorityResponse>> getAuthorities() {
        return ResponseEntity.ok(potholeQueryService.getAllAuthorities());
    }
}
