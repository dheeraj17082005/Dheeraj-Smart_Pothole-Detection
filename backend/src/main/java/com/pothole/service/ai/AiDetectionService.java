package com.pothole.service.ai;

import com.pothole.client.ai.AiInferenceClient;
import com.pothole.dto.ai.AiAnnotatedDetectionResult;
import com.pothole.dto.ai.AiImageDetectionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class AiDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AiDetectionService.class);

    private final AiInferenceClient aiInferenceClient;

    public AiDetectionService(AiInferenceClient aiInferenceClient) {
        this.aiInferenceClient = aiInferenceClient;
    }

    /**
     * Call the AI inference engine to detect potholes on the provided image resource.
     *
     * @param imageResource       the binary image resource (e.g. from MultipartFile or S3/MinIO)
     * @param filename            the original filename
     * @param confidenceThreshold optional confidence override (e.g. 0.25)
     * @return structured AI detection response
     */
    public AiImageDetectionResponse detectImage(Resource imageResource, String filename, Double confidenceThreshold) {
        log.debug("Delegating image detection to AI client for file: {}", filename);
        return aiInferenceClient.detectImage(imageResource, filename, confidenceThreshold);
    }

    /**
     * Call the AI inference engine to detect potholes and retrieve the annotated image binary.
     *
     * @param imageResource       the binary image resource
     * @param filename            the original filename
     * @param confidenceThreshold optional confidence override
     * @return detection metadata and annotated JPEG byte array
     */
    public AiAnnotatedDetectionResult detectAndAnnotateImage(Resource imageResource, String filename, Double confidenceThreshold) {
        log.debug("Delegating image detection & annotation to AI client for file: {}", filename);
        return aiInferenceClient.detectAndAnnotateImage(imageResource, filename, confidenceThreshold);
    }

    /**
     * Call the AI inference engine to sample and detect potholes across a video stream.
     */
    public com.pothole.dto.ai.AiVideoDetectionResponse detectVideo(Resource videoResource, String filename, Double sampleFps, Double confidenceThreshold) {
        log.debug("Delegating video detection to AI client for file: {} (sampleFps: {})", filename, sampleFps);
        return aiInferenceClient.detectVideo(videoResource, filename, sampleFps, confidenceThreshold);
    }

    /**
     * Extract and annotate a specific frame from the video stream as binary JPEG.
     */
    public byte[] annotateVideoFrame(Resource videoResource, String filename, Integer frameIndex, Double confidenceThreshold) {
        log.debug("Delegating video frame annotation to AI client for file: {} (frameIndex: {})", filename, frameIndex);
        return aiInferenceClient.annotateVideoFrame(videoResource, filename, frameIndex, confidenceThreshold);
    }
}
