package com.pothole.controller;

import com.pothole.dto.DetectVideoAcceptedResponse;
import com.pothole.service.detection.VideoDetectionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/potholes")
public class VideoDetectionController {

    private final VideoDetectionService videoDetectionService;

    public VideoDetectionController(VideoDetectionService videoDetectionService) {
        this.videoDetectionService = videoDetectionService;
    }

    /**
     * Submit a video stream for asynchronous frame sampling, ONNX pothole detection, and Type A aggregation.
     * Returns immediately with HTTP 202 Accepted and job polling reference.
     */
    @PostMapping(value = "/detect-video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DetectVideoAcceptedResponse> detectPotholesInVideo(
            org.springframework.security.core.Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam(value = "capturedAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant capturedAt,
            @RequestParam(value = "addressText", required = false) String addressText
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to submit video evidence.");
        }

        DetectVideoAcceptedResponse response = videoDetectionService.initiateVideoJob(
                file,
                latitude,
                longitude,
                capturedAt,
                addressText
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
