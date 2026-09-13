package com.pothole.service.severity;

import com.pothole.dto.ai.AiDetection;
import com.pothole.model.enums.SeverityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for computing visual 2D severity metrics from computer vision detections.
 *
 * NOTE: Severity is a 2D optical surface heuristic calculated from camera perspective.
 * It does NOT measure physical pothole depth, 3D displacement, or vehicle impact dynamics.
 */
@Service
public class SeverityService {

    private static final Logger log = LoggerFactory.getLogger(SeverityService.class);

    public static final double LOW_THRESHOLD = 20.0;
    public static final double HIGH_THRESHOLD = 50.0;
    public static final double MAX_SEVERITY_SCORE = 100.0;
    public static final int MULTIPLE_POTHOLES_HIGH_SEVERITY_COUNT = 3;

    /**
     * Calculate visual area ratio from bounding box and original image dimensions.
     * R = (boxWidth * boxHeight) / (imageWidth * imageHeight)
     */
    public double calculateVisualAreaRatio(int boxXmin, int boxYmin, int boxXmax, int boxYmax, int imageWidth, int imageHeight) {
        validateImageDimensions(imageWidth, imageHeight);
        validateBoundingBox(boxXmin, boxYmin, boxXmax, boxYmax, imageWidth, imageHeight);

        long boxArea = (long) (boxXmax - boxXmin) * (boxYmax - boxYmin);
        long imageArea = (long) imageWidth * imageHeight;

        double ratio = (double) boxArea / (double) imageArea;
        return Math.min(1.0, Math.max(0.0, ratio));
    }

    /**
     * Calculate single-detection severity score and classification.
     * S = min(100.0, R * 1000 * C)
     */
    public SeverityResult calculateSeverity(double visualAreaRatio, double confidence) {
        validateVisualAreaRatio(visualAreaRatio);
        validateConfidence(confidence);

        double rawScore = visualAreaRatio * 1000.0 * confidence;
        double cappedScore = Math.min(MAX_SEVERITY_SCORE, Math.max(0.0, rawScore));
        double roundedScore = Math.round(cappedScore * 100.0) / 100.0;

        SeverityClass severityClass = classify(roundedScore);
        return new SeverityResult(roundedScore, severityClass);
    }

    /**
     * Calculate single-detection severity from bounding box, image dimensions, and confidence.
     */
    public SeverityResult calculateSeverity(int boxXmin, int boxYmin, int boxXmax, int boxYmax, int imageWidth, int imageHeight, double confidence) {
        double ratio = calculateVisualAreaRatio(boxXmin, boxYmin, boxXmax, boxYmax, imageWidth, imageHeight);
        return calculateSeverity(ratio, confidence);
    }

    /**
     * Calculate aggregate severity score and classification for an image with multiple detections.
     *
     * Aggregate Score: S_agg = min(100.0, sum(S_i))
     * Classification: HIGH if count >= 3 or S_agg >= 50.0; MEDIUM if 20.0 <= S_agg < 50.0; LOW if S_agg < 20.0.
     */
    public SeverityResult calculateAggregateSeverity(List<AiDetection> detections, int imageWidth, int imageHeight) {
        if (detections == null || detections.isEmpty()) {
            return new SeverityResult(0.0, SeverityClass.LOW);
        }

        validateImageDimensions(imageWidth, imageHeight);

        double totalScore = 0.0;
        for (AiDetection det : detections) {
            validateConfidence(det.confidence());
            double ratio = det.visualAreaRatio();
            if (ratio <= 0.0 && det.box() != null) {
                ratio = calculateVisualAreaRatio(
                        det.box().xmin(), det.box().ymin(), det.box().xmax(), det.box().ymax(),
                        imageWidth, imageHeight
                );
            }
            validateVisualAreaRatio(ratio);

            double detScore = Math.min(MAX_SEVERITY_SCORE, ratio * 1000.0 * det.confidence());
            totalScore += detScore;
        }

        double cappedTotalScore = Math.min(MAX_SEVERITY_SCORE, Math.max(0.0, totalScore));
        double roundedScore = Math.round(cappedTotalScore * 100.0) / 100.0;

        SeverityClass severityClass;
        if (detections.size() >= MULTIPLE_POTHOLES_HIGH_SEVERITY_COUNT || roundedScore >= HIGH_THRESHOLD) {
            severityClass = SeverityClass.HIGH;
        } else if (roundedScore >= LOW_THRESHOLD) {
            severityClass = SeverityClass.MEDIUM;
        } else {
            severityClass = SeverityClass.LOW;
        }

        return new SeverityResult(roundedScore, severityClass);
    }

    private SeverityClass classify(double score) {
        if (score >= HIGH_THRESHOLD) {
            return SeverityClass.HIGH;
        } else if (score >= LOW_THRESHOLD) {
            return SeverityClass.MEDIUM;
        } else {
            return SeverityClass.LOW;
        }
    }

    public void validateImageDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image dimensions must be strictly positive: width=" + width + ", height=" + height);
        }
    }

    public void validateConfidence(double confidence) {
        if (Double.isNaN(confidence) || confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0 inclusive: " + confidence);
        }
    }

    public void validateVisualAreaRatio(double ratio) {
        if (Double.isNaN(ratio) || ratio < 0.0 || ratio > 1.0) {
            throw new IllegalArgumentException("Visual area ratio must be between 0.0 and 1.0 inclusive: " + ratio);
        }
    }

    public void validateBoundingBox(int xmin, int ymin, int xmax, int ymax, int imgWidth, int imgHeight) {
        if (xmin < 0 || ymin < 0) {
            throw new IllegalArgumentException("Bounding box coordinates cannot be negative: xmin=" + xmin + ", ymin=" + ymin);
        }
        if (xmax <= xmin || ymax <= ymin) {
            throw new IllegalArgumentException("Invalid bounding box dimensions: xmin=" + xmin + ", xmax=" + xmax + ", ymin=" + ymin + ", ymax=" + ymax);
        }
        if (xmax > imgWidth || ymax > imgHeight) {
            throw new IllegalArgumentException("Bounding box exceeds image dimensions: (" + xmax + "x" + ymax + ") > (" + imgWidth + "x" + imgHeight + ")");
        }
    }
}
