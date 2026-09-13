package com.pothole.controller.internal;

import com.pothole.dto.ai.AiImageDetectionResponse;
import com.pothole.service.ai.AiDetectionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * TEMPORARY integration verification controller.
 * Used exclusively for validating Spring Boot <-> FastAPI AI service cross-service communication.
 * This endpoint will be superseded by the domain-level PotholeController in subsequent milestones.
 */
@RestController
@RequestMapping("/api/v1/internal/ai-test")
public class AiTestController {

    private final AiDetectionService aiDetectionService;

    public AiTestController(AiDetectionService aiDetectionService) {
        this.aiDetectionService = aiDetectionService;
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiImageDetectionResponse> testImageInference(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confidenceThreshold", required = false) Double confidenceThreshold
    ) {
        AiImageDetectionResponse response = aiDetectionService.detectImage(
                file.getResource(),
                file.getOriginalFilename(),
                confidenceThreshold
        );
        return ResponseEntity.ok(response);
    }
}
