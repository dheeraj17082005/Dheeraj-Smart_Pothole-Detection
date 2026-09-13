package com.pothole.service.video;

import com.pothole.dto.ai.AiBoundingBox;
import com.pothole.dto.ai.AiDetection;
import com.pothole.dto.ai.AiFrameDetection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VideoAggregationServiceTest {

    private VideoAggregationService aggregationService;

    @BeforeEach
    void setUp() {
        aggregationService = new VideoAggregationService();
    }

    @Test
    @DisplayName("Empty or null frames produce 0 tracks")
    void testEmptyFrames() {
        assertThat(aggregationService.aggregateDetectionsIntoTracks(null)).isEmpty();
        assertThat(aggregationService.aggregateDetectionsIntoTracks(List.of())).isEmpty();
    }

    @Test
    @DisplayName("6 consecutive frame detections within 2.5s with overlapping boxes aggregate to 1 PotholeTrack")
    void testSixConsecutiveDetectionsAggregateToOneTrack() {
        List<AiFrameDetection> frames = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            double timestamp = i * 0.5; // 0.0, 0.5, 1.0, 1.5, 2.0, 2.5
            // Box shifts slightly down/larger as car approaches
            int xmin = 200 + (i * 5);
            int ymin = 300 + (i * 10);
            int xmax = 400 + (i * 10);
            int ymax = 450 + (i * 15);
            double conf = 0.80 + (i * 0.02);
            double areaRatio = 0.02 + (i * 0.005);

            AiDetection det = new AiDetection(
                    new AiBoundingBox(xmin, ymin, xmax, ymax),
                    conf,
                    3,
                    "pothole",
                    areaRatio
            );
            frames.add(new AiFrameDetection(i * 15, timestamp, 1920, 1080, List.of(det)));
        }

        List<PotholeTrack> tracks = aggregationService.aggregateDetectionsIntoTracks(frames);

        assertThat(tracks).hasSize(1);
        PotholeTrack singleTrack = tracks.get(0);
        assertThat(singleTrack.size()).isEqualTo(6);
        assertThat(singleTrack.getMaxConfidence()).isGreaterThanOrEqualTo(0.90);
        assertThat(singleTrack.getFirstTimestampSec()).isEqualTo(0.0);

        TrackedDetection rep = singleTrack.getRepresentativeDetection();
        assertThat(rep).isNotNull();
        // The last detection has the highest areaRatio and confidence
        assertThat(rep.frameIndex()).isEqualTo(5 * 15);
    }

    @Test
    @DisplayName("Two distinct potholes in the same frame (left and right lanes) form 2 separate tracks")
    void testTwoPotholesInParallelLanes() {
        // Frame 0: Left pothole and Right pothole
        AiDetection leftPotholeF0 = new AiDetection(
                new AiBoundingBox(100, 400, 250, 500), 0.85, 3, "pothole", 0.03
        );
        AiDetection rightPotholeF0 = new AiDetection(
                new AiBoundingBox(800, 400, 950, 500), 0.88, 3, "pothole", 0.03
        );
        AiFrameDetection frame0 = new AiFrameDetection(0, 0.0, 1920, 1080, List.of(leftPotholeF0, rightPotholeF0));

        // Frame 1: Left pothole and Right pothole at t=0.5s
        AiDetection leftPotholeF1 = new AiDetection(
                new AiBoundingBox(110, 420, 270, 530), 0.89, 3, "pothole", 0.04
        );
        AiDetection rightPotholeF1 = new AiDetection(
                new AiBoundingBox(810, 420, 970, 530), 0.91, 3, "pothole", 0.04
        );
        AiFrameDetection frame1 = new AiFrameDetection(15, 0.5, 1920, 1080, List.of(leftPotholeF1, rightPotholeF1));

        List<PotholeTrack> tracks = aggregationService.aggregateDetectionsIntoTracks(List.of(frame0, frame1));

        assertThat(tracks).hasSize(2);
        assertThat(tracks.get(0).size()).isEqualTo(2);
        assertThat(tracks.get(1).size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Detections with temporal gap > 3.0s do not cluster together, forming 2 tracks")
    void testTemporalGapSeparatesTracks() {
        AiDetection det1 = new AiDetection(new AiBoundingBox(200, 300, 400, 450), 0.85, 3, "pothole", 0.03);
        AiFrameDetection frame1 = new AiFrameDetection(0, 0.0, 1920, 1080, List.of(det1));

        // Second detection at exact same coordinate but 4.5 seconds later
        AiDetection det2 = new AiDetection(new AiBoundingBox(200, 300, 400, 450), 0.88, 3, "pothole", 0.03);
        AiFrameDetection frame2 = new AiFrameDetection(135, 4.5, 1920, 1080, List.of(det2));

        List<PotholeTrack> tracks = aggregationService.aggregateDetectionsIntoTracks(List.of(frame1, frame2));

        assertThat(tracks).hasSize(2);
        assertThat(tracks.get(0).size()).isEqualTo(1);
        assertThat(tracks.get(1).size()).isEqualTo(1);
    }
}
