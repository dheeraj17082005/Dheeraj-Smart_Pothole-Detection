package com.pothole.client.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pothole.dto.ai.AiImageDetectionResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.net.Socket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
class FastApiInferenceClientLiveIT {

    private boolean isAiServiceReachable() {
        try (Socket socket = new Socket("127.0.0.1", 8000)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void testRealFastApiInference_whenServiceRunning() {
        assumeTrue(isAiServiceReachable(), "FastAPI AI service is not running on localhost:8000, skipping live integration test.");

        RestClient restClient = RestClient.builder().baseUrl("http://localhost:8000").build();
        FastApiInferenceClient client = new FastApiInferenceClient(restClient, new ObjectMapper());

        File testFile = new File("../test-data/images/pothole_sample.jpg");
        assumeTrue(testFile.exists(), "Test fixture pothole_sample.jpg does not exist");

        AiImageDetectionResponse response = client.detectImage(
                new FileSystemResource(testFile),
                "pothole_sample.jpg",
                0.20
        );

        assertThat(response).isNotNull();
        assertThat(response.model().name()).isEqualTo("peterhdd/pothole-detection-yolov8");
        assertThat(response.image().width()).isEqualTo(1920);
        assertThat(response.image().height()).isEqualTo(1920);
        assertThat(response.potholeCount()).isGreaterThanOrEqualTo(1);
        assertThat(response.detections()).isNotEmpty();
        assertThat(response.detections().get(0).classId()).isEqualTo(0);
        assertThat(response.detections().get(0).className()).isEqualTo("pothole");
    }
}
