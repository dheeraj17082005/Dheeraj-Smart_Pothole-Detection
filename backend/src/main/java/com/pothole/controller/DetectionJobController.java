package com.pothole.controller;

import com.pothole.dto.JobDetailResponse;
import com.pothole.service.detection.VideoDetectionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/detection-jobs")
public class DetectionJobController {

    private final VideoDetectionService videoDetectionService;

    public DetectionJobController(VideoDetectionService videoDetectionService) {
        this.videoDetectionService = videoDetectionService;
    }

    /**
     * Poll detection job progress, status, and aggregated results.
     */
    @GetMapping(value = "/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JobDetailResponse> getJobStatus(@PathVariable("jobId") UUID jobId) {
        JobDetailResponse response = videoDetectionService.getJobDetail(jobId);
        return ResponseEntity.ok(response);
    }
}
