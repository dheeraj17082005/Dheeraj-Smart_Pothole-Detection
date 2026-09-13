package com.pothole.service.severity;

import com.pothole.dto.ai.AiBoundingBox;
import com.pothole.dto.ai.AiDetection;
import com.pothole.model.enums.SeverityClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeverityServiceTest {

    private SeverityService severityService;

    @BeforeEach
    void setUp() {
        severityService = new SeverityService();
    }

    @Test
    @DisplayName("1. Very small pothole produces LOW severity")
    void testSmallPotholeLowSeverity() {
        // R = 0.01 (1% frame area), C = 0.85 -> S = 0.01 * 1000 * 0.85 = 8.5 < 20.0 (LOW)
        SeverityResult result = severityService.calculateSeverity(0.01, 0.85);

        assertThat(result.severityScore()).isEqualTo(8.5);
        assertThat(result.severityClass()).isEqualTo(SeverityClass.LOW);
    }

    @Test
    @DisplayName("2. Medium visual area produces MEDIUM severity")
    void testMediumPotholeMediumSeverity() {
        // R = 0.04 (4% frame area), C = 0.88 -> S = 0.04 * 1000 * 0.88 = 35.2 (MEDIUM: 20 <= S < 50)
        SeverityResult result = severityService.calculateSeverity(0.04, 0.88);

        assertThat(result.severityScore()).isEqualTo(35.2);
        assertThat(result.severityClass()).isEqualTo(SeverityClass.MEDIUM);
    }

    @Test
    @DisplayName("3. Large visual area produces HIGH severity")
    void testLargePotholeHighSeverity() {
        // R = 0.08 (8% frame area), C = 0.90 -> S = 0.08 * 1000 * 0.90 = 72.0 (HIGH: S >= 50)
        SeverityResult result = severityService.calculateSeverity(0.08, 0.90);

        assertThat(result.severityScore()).isEqualTo(72.0);
        assertThat(result.severityClass()).isEqualTo(SeverityClass.HIGH);
    }

    @Test
    @DisplayName("4. Confidence score scales the severity score proportionally")
    void testConfidenceScalesSeverity() {
        // R = 0.05, C = 0.40 -> S = 0.05 * 1000 * 0.40 = 20.0
        SeverityResult lowConf = severityService.calculateSeverity(0.05, 0.40);
        // R = 0.05, C = 0.80 -> S = 0.05 * 1000 * 0.80 = 40.0
        SeverityResult highConf = severityService.calculateSeverity(0.05, 0.80);

        assertThat(lowConf.severityScore()).isEqualTo(20.0);
        assertThat(highConf.severityScore()).isEqualTo(40.0);
        assertThat(highConf.severityScore()).isGreaterThan(lowConf.severityScore());
    }

    @Test
    @DisplayName("5. Severity score is capped at 100.0")
    void testScoreCappedAt100() {
        // R = 0.25 (25% frame area), C = 0.95 -> raw = 237.5 -> capped = 100.0
        SeverityResult result = severityService.calculateSeverity(0.25, 0.95);

        assertThat(result.severityScore()).isEqualTo(100.0);
        assertThat(result.severityClass()).isEqualTo(SeverityClass.HIGH);
    }

    @Test
    @DisplayName("6. Boundary test: score exactly 20.0 produces MEDIUM")
    void testBoundaryExactly20() {
        // R = 0.02, C = 1.0 -> S = 20.0
        SeverityResult result = severityService.calculateSeverity(0.02, 1.0);

        assertThat(result.severityScore()).isEqualTo(20.0);
        assertThat(result.severityClass()).isEqualTo(SeverityClass.MEDIUM);
    }

    @Test
    @DisplayName("7. Boundary test: score exactly 50.0 produces HIGH")
    void testBoundaryExactly50() {
        // R = 0.05, C = 1.0 -> S = 50.0
        SeverityResult result = severityService.calculateSeverity(0.05, 1.0);

        assertThat(result.severityScore()).isEqualTo(50.0);
        assertThat(result.severityClass()).isEqualTo(SeverityClass.HIGH);
    }

    @Test
    @DisplayName("8. Multiple pothole detections escalate to HIGH when count >= 3")
    void testMultiplePotholeDetectionsCluster() {
        // 3 small detections each R=0.01, C=0.5 -> S_i = 5.0 -> S_agg = 15.0 (normally LOW, but count=3 >= 3 -> HIGH)
        List<AiDetection> detections = List.of(
                new AiDetection(new AiBoundingBox(10, 10, 100, 100), 0.5, 3, "pothole", 0.01),
                new AiDetection(new AiBoundingBox(110, 10, 200, 100), 0.5, 3, "pothole", 0.01),
                new AiDetection(new AiBoundingBox(210, 10, 300, 100), 0.5, 3, "pothole", 0.01)
        );

        SeverityResult result = severityService.calculateAggregateSeverity(detections, 1920, 1080);

        assertThat(result.severityScore()).isEqualTo(15.0);
        assertThat(result.severityClass()).isEqualTo(SeverityClass.HIGH);
    }

    @Test
    @DisplayName("9. Invalid confidence throws IllegalArgumentException")
    void testInvalidConfidence() {
        assertThatThrownBy(() -> severityService.calculateSeverity(0.05, -0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Confidence must be between 0.0 and 1.0");

        assertThatThrownBy(() -> severityService.calculateSeverity(0.05, 1.05))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Confidence must be between 0.0 and 1.0");
    }

    @Test
    @DisplayName("10. Invalid visual area ratio throws IllegalArgumentException")
    void testInvalidAreaRatio() {
        assertThatThrownBy(() -> severityService.calculateSeverity(-0.01, 0.8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Visual area ratio must be between 0.0 and 1.0");

        assertThatThrownBy(() -> severityService.calculateSeverity(1.2, 0.8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Visual area ratio must be between 0.0 and 1.0");
    }

    @Test
    @DisplayName("11. Invalid image dimensions throw IllegalArgumentException")
    void testInvalidImageDimensions() {
        assertThatThrownBy(() -> severityService.calculateVisualAreaRatio(0, 0, 100, 100, 0, 1080))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Image dimensions must be strictly positive");

        assertThatThrownBy(() -> severityService.calculateVisualAreaRatio(0, 0, 100, 100, 1920, -10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Image dimensions must be strictly positive");
    }

    @Test
    @DisplayName("12. Invalid bounding box coordinates throw IllegalArgumentException")
    void testInvalidBoundingBox() {
        // Negative coordinate
        assertThatThrownBy(() -> severityService.calculateVisualAreaRatio(-10, 0, 100, 100, 1920, 1080))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bounding box coordinates cannot be negative");

        // xmax <= xmin
        assertThatThrownBy(() -> severityService.calculateVisualAreaRatio(100, 0, 50, 100, 1920, 1080))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid bounding box dimensions");

        // Exceeds image boundary
        assertThatThrownBy(() -> severityService.calculateVisualAreaRatio(0, 0, 2000, 100, 1920, 1080))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bounding box exceeds image dimensions");
    }
}
