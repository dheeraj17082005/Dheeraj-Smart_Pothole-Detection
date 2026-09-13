package com.pothole.controller.internal;

import com.pothole.dto.ai.*;
import com.pothole.exception.AiInvalidImageException;
import com.pothole.exception.GlobalExceptionHandler;
import com.pothole.service.ai.AiDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiTestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AiDetectionService aiDetectionService;

    @InjectMocks
    private AiTestController aiTestController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(aiTestController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testImageInference_success() throws Exception {
        AiImageDetectionResponse mockResponse = new AiImageDetectionResponse(
                new AiModelMetadata("vinothvikas1987/pothole-detection-yolov8", "YOLOv8s-RDD"),
                new AiImageMetadata(1920, 1080),
                1,
                0.885,
                0.042,
                List.of(new AiDetection(
                        new AiBoundingBox(120, 85, 340, 290),
                        0.885,
                        3,
                        "pothole",
                        0.042
                ))
        );

        when(aiDetectionService.detectImage(any(), eq("test.jpg"), eq(0.35)))
                .thenReturn(mockResponse);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/internal/ai-test/image")
                        .file(file)
                        .param("confidenceThreshold", "0.35"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model.name").value("vinothvikas1987/pothole-detection-yolov8"))
                .andExpect(jsonPath("$.image.width").value(1920))
                .andExpect(jsonPath("$.pothole_count").value(1))
                .andExpect(jsonPath("$.max_confidence").value(0.885))
                .andExpect(jsonPath("$.detections[0].class_name").value("pothole"))
                .andExpect(jsonPath("$.detections[0].box.xmin").value(120));
    }

    @Test
    void testImageInference_invalidImage_returnsBadRequest() throws Exception {
        when(aiDetectionService.detectImage(any(), any(), any()))
                .thenThrow(new AiInvalidImageException("Invalid image header"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "corrupted.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/internal/ai-test/image").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Image Payload"))
                .andExpect(jsonPath("$.detail").value("Invalid image header"));
    }
}
