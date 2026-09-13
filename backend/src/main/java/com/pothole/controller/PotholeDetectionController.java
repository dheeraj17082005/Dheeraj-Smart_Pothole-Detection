package com.pothole.controller;

import com.pothole.dto.DetectImageRequest;
import com.pothole.dto.DetectImageResponse;
import com.pothole.service.detection.ImageDetectionService;
import com.pothole.model.User;
import com.pothole.service.AuthService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/potholes")
public class PotholeDetectionController {

    private final ImageDetectionService imageDetectionService;
    private final AuthService authService;

    public PotholeDetectionController(ImageDetectionService imageDetectionService, AuthService authService) {
        this.imageDetectionService = imageDetectionService;
        this.authService = authService;
    }

    /**
     * Submit an image for pothole detection, spatial aggregation, and registration.
     */
    @PostMapping(value = "/detect-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DetectImageResponse> detectPotholesInImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam(value = "capturedAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant capturedAt,
            @RequestParam(value = "addressText", required = false) String addressText,
            @RequestParam(value = "confidenceThreshold", required = false) Double confidenceThreshold
    ) {
        DetectImageRequest request = new DetectImageRequest(
                file,
                latitude,
                longitude,
                capturedAt,
                addressText,
                confidenceThreshold
        );

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to submit pothole evidence.");
        }
        User currentUser = authService.getUserByEmail(authentication.getName());

        DetectImageResponse response = imageDetectionService.processImageDetection(request, currentUser);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
