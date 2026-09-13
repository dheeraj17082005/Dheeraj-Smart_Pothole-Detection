package com.pothole.controller.internal;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import com.pothole.client.ai.FastApiInferenceClient;
import com.pothole.config.AiClientConfig;
import com.pothole.service.ai.AiDetectionService;

@WebMvcTest(AiTestController.class)
@Import({AiClientConfig.class, FastApiInferenceClient.class, AiDetectionService.class})
@TestPropertySource(properties = {
        "ai.service.url=http://localhost:8000"
})
@Tag("integration")
class AiTestControllerLiveIT {

    @Autowired
    private MockMvc mockMvc;

    private boolean isAiServiceReachable() {
        try (Socket socket = new Socket("127.0.0.1", 8000)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void testEndToEndInternalAiTestImageEndpoint() throws Exception {
        assumeTrue(isAiServiceReachable(), "FastAPI AI service not running on port 8000");

        File sampleFile = new File("../test-data/images/pothole_sample.jpg");
        assumeTrue(sampleFile.exists(), "Sample fixture pothole_sample.jpg does not exist");

        byte[] imageBytes;
        try (FileInputStream fis = new FileInputStream(sampleFile)) {
            imageBytes = fis.readAllBytes();
        }

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "pothole_sample.jpg",
                "image/jpeg",
                imageBytes
        );

        MvcResult result = mockMvc.perform(multipart("/api/v1/internal/ai-test/image")
                        .file(multipartFile)
                        .param("confidenceThreshold", "0.20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model.name").value("peterhdd/pothole-detection-yolov8"))
                .andExpect(jsonPath("$.model.version").value("YOLOv8s"))
                .andExpect(jsonPath("$.image.width").value(1920))
                .andExpect(jsonPath("$.image.height").value(1920))
                .andExpect(jsonPath("$.pothole_count").value(1))
                .andExpect(jsonPath("$.detections[0].class_id").value(0))
                .andExpect(jsonPath("$.detections[0].class_name").value("pothole"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains("pothole");
    }
}
