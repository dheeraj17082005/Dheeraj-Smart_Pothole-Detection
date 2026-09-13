package com.pothole.client.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pothole.dto.ai.AiImageDetectionResponse;
import com.pothole.exception.AiInferenceFailedException;
import com.pothole.exception.AiInvalidImageException;
import com.pothole.exception.AiServiceTimeoutException;
import com.pothole.exception.AiServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FastApiInferenceClientTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;
    private FastApiInferenceClient client;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder().baseUrl("http://localhost:8000");
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        objectMapper = new ObjectMapper();
        client = new FastApiInferenceClient(restClientBuilder.build(), objectMapper);
    }

    @Test
    void detectImage_successfulResponseMapping() {
        String jsonResponse = """
                {
                  "model": {
                    "name": "vinothvikas1987/pothole-detection-yolov8",
                    "version": "YOLOv8s-RDD"
                  },
                  "image": {
                    "width": 1920,
                    "height": 1080
                  },
                  "pothole_count": 1,
                  "max_confidence": 0.885,
                  "max_area_ratio": 0.042,
                  "detections": [
                    {
                      "box": {
                        "xmin": 120,
                        "ymin": 85,
                        "xmax": 340,
                        "ymax": 290
                      },
                      "confidence": 0.885,
                      "class_id": 3,
                      "class_name": "pothole",
                      "visual_area_ratio": 0.042
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("http://localhost:8000/detect/image"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        ByteArrayResource resource = new ByteArrayResource(new byte[]{1, 2, 3});
        AiImageDetectionResponse response = client.detectImage(resource, "test.jpg", null);

        assertThat(response).isNotNull();
        assertThat(response.model().name()).isEqualTo("vinothvikas1987/pothole-detection-yolov8");
        assertThat(response.image().width()).isEqualTo(1920);
        assertThat(response.potholeCount()).isEqualTo(1);
        assertThat(response.maxConfidence()).isEqualTo(0.885);
        assertThat(response.detections()).hasSize(1);
        assertThat(response.detections().get(0).box().xmin()).isEqualTo(120);
        assertThat(response.detections().get(0).classId()).isEqualTo(3);
        assertThat(response.detections().get(0).className()).isEqualTo("pothole");

        mockServer.verify();
    }

    @Test
    void detectImage_emptyDetectionsResponseMapping() {
        String jsonResponse = """
                {
                  "model": {
                    "name": "vinothvikas1987/pothole-detection-yolov8",
                    "version": "YOLOv8s-RDD"
                  },
                  "image": {
                    "width": 640,
                    "height": 640
                  },
                  "pothole_count": 0,
                  "max_confidence": 0.0,
                  "max_area_ratio": 0.0,
                  "detections": []
                }
                """;

        mockServer.expect(requestTo("http://localhost:8000/detect/image?confidence_threshold=0.35"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        ByteArrayResource resource = new ByteArrayResource(new byte[]{1, 2, 3});
        AiImageDetectionResponse response = client.detectImage(resource, "clean.jpg", 0.35);

        assertThat(response).isNotNull();
        assertThat(response.potholeCount()).isZero();
        assertThat(response.detections()).isEmpty();
        assertThat(response.maxConfidence()).isZero();

        mockServer.verify();
    }

    @Test
    void detectImage_fastApi400ThrowsAiInvalidImageException() {
        String errorJson = """
                {
                  "error": {
                    "code": "INVALID_IMAGE",
                    "message": "Unable to decode uploaded image"
                  }
                }
                """;

        mockServer.expect(requestTo("http://localhost:8000/detect/image"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body(errorJson).contentType(MediaType.APPLICATION_JSON));

        ByteArrayResource resource = new ByteArrayResource(new byte[]{1, 2, 3});
        assertThatThrownBy(() -> client.detectImage(resource, "bad.jpg", null))
                .isInstanceOf(AiInvalidImageException.class)
                .hasMessageContaining("Unable to decode uploaded image");

        mockServer.verify();
    }

    @Test
    void detectImage_fastApi503ThrowsAiServiceUnavailableException() {
        String errorJson = """
                {
                  "error": {
                    "code": "MODEL_NOT_READY",
                    "message": "ONNX model artifact is not loaded"
                  }
                }
                """;

        mockServer.expect(requestTo("http://localhost:8000/detect/image"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body(errorJson).contentType(MediaType.APPLICATION_JSON));

        ByteArrayResource resource = new ByteArrayResource(new byte[]{1, 2, 3});
        assertThatThrownBy(() -> client.detectImage(resource, "test.jpg", null))
                .isInstanceOf(AiServiceUnavailableException.class)
                .hasMessageContaining("ONNX model artifact is not loaded");

        mockServer.verify();
    }

    @Test
    void detectImage_fastApi500ThrowsAiInferenceFailedException() {
        String errorJson = """
                {
                  "error": {
                    "code": "INFERENCE_FAILED",
                    "message": "Internal ONNX execution failed"
                  }
                }
                """;

        mockServer.expect(requestTo("http://localhost:8000/detect/image"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body(errorJson).contentType(MediaType.APPLICATION_JSON));

        ByteArrayResource resource = new ByteArrayResource(new byte[]{1, 2, 3});
        assertThatThrownBy(() -> client.detectImage(resource, "test.jpg", null))
                .isInstanceOf(AiInferenceFailedException.class)
                .hasMessageContaining("Internal ONNX execution failed");

        mockServer.verify();
    }

    @Test
    void detectAndAnnotateImage_successfulResponseMapping() {
        String jsonResponse = """
                {
                  "model": {
                    "name": "vinothvikas1987/pothole-detection-yolov8",
                    "version": "YOLOv8s-RDD"
                  },
                  "image": {
                    "width": 1920,
                    "height": 1080
                  },
                  "pothole_count": 1,
                  "max_confidence": 0.885,
                  "max_area_ratio": 0.042,
                  "detections": [
                    {
                      "box": {
                        "xmin": 120,
                        "ymin": 85,
                        "xmax": 340,
                        "ymax": 290
                      },
                      "confidence": 0.885,
                      "class_id": 3,
                      "class_name": "pothole",
                      "visual_area_ratio": 0.042
                    }
                  ]
                }
                """;

        byte[] fakeJpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("X-Detection-Metadata", jsonResponse);
        headers.setContentType(MediaType.IMAGE_JPEG);

        mockServer.expect(requestTo("http://localhost:8000/detect/image/annotate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(fakeJpeg, MediaType.IMAGE_JPEG).headers(headers));

        ByteArrayResource resource = new ByteArrayResource(new byte[]{1, 2, 3});
        com.pothole.dto.ai.AiAnnotatedDetectionResult result = client.detectAndAnnotateImage(resource, "sample.jpg", null);

        assertThat(result).isNotNull();
        assertThat(result.detectionResponse().potholeCount()).isEqualTo(1);
        assertThat(result.annotatedImageBytes()).isEqualTo(fakeJpeg);

        mockServer.verify();
    }

    @Test
    void detectImage_connectionFailureThrowsAiServiceUnavailableException() {
        RestClient brokenClient = RestClient.builder().baseUrl("http://127.0.0.1:59999").build();
        FastApiInferenceClient brokenInferenceClient = new FastApiInferenceClient(brokenClient, objectMapper);

        ByteArrayResource resource = new ByteArrayResource(new byte[]{1, 2, 3});
        assertThatThrownBy(() -> brokenInferenceClient.detectImage(resource, "test.jpg", null))
                .isInstanceOf(AiServiceUnavailableException.class)
                .hasMessageContaining("Unable to connect to AI service");
    }
}
