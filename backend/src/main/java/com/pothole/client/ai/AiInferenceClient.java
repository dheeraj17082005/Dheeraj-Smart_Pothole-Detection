package com.pothole.client.ai;

import com.pothole.dto.ai.AiAnnotatedDetectionResult;
import com.pothole.dto.ai.AiImageDetectionResponse;
import org.springframework.core.io.Resource;

public interface AiInferenceClient {

    AiImageDetectionResponse detectImage(Resource imageResource, String filename, Double confidenceThreshold);

    AiAnnotatedDetectionResult detectAndAnnotateImage(Resource imageResource, String filename, Double confidenceThreshold);

    com.pothole.dto.ai.AiVideoDetectionResponse detectVideo(Resource videoResource, String filename, Double sampleFps, Double confidenceThreshold);

    byte[] annotateVideoFrame(Resource videoResource, String filename, Integer frameIndex, Double confidenceThreshold);
}
