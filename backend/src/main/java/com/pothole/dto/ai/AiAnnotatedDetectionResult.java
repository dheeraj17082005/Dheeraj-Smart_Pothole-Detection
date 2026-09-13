package com.pothole.dto.ai;

public record AiAnnotatedDetectionResult(
        AiImageDetectionResponse detectionResponse,
        byte[] annotatedImageBytes
) {}
