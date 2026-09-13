package com.pothole.service.video;

import com.pothole.dto.ai.AiDetection;
import com.pothole.dto.ai.AiFrameDetection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class VideoAggregationService {

    private static final Logger log = LoggerFactory.getLogger(VideoAggregationService.class);

    public static final double MAX_TEMPORAL_GAP_SEC = 3.0;
    public static final double IOU_MATCH_THRESHOLD = 0.30;
    public static final double CENTROID_DIST_THRESHOLD = 0.20;

    /**
     * Aggregates raw frame-level detections across a video stream into physical pothole tracks (Type A).
     *
     * @param frames List of sampled frame detections from the AI service in chronological order
     * @return List of aggregated PotholeTrack clusters, each representing a unique physical road defect
     */
    public List<PotholeTrack> aggregateDetectionsIntoTracks(List<AiFrameDetection> frames) {
        if (frames == null || frames.isEmpty()) {
            return List.of();
        }

        // Sort frames chronologically by timestamp
        List<AiFrameDetection> sortedFrames = frames.stream()
                .sorted(Comparator.comparingDouble(AiFrameDetection::timestampSec))
                .toList();

        List<PotholeTrack> activeTracks = new ArrayList<>();

        for (AiFrameDetection frame : sortedFrames) {
            int frameIdx = frame.frameIndex() != null ? frame.frameIndex() : 0;
            double timeSec = frame.timestampSec() != null ? frame.timestampSec() : 0.0;
            int width = frame.imageWidth() != null ? frame.imageWidth() : 1920;
            int height = frame.imageHeight() != null ? frame.imageHeight() : 1080;

            if (frame.detections() == null || frame.detections().isEmpty()) {
                continue;
            }

            for (AiDetection det : frame.detections()) {
                if (det.box() == null) {
                    continue;
                }

                TrackedDetection newDet = new TrackedDetection(
                        frameIdx,
                        timeSec,
                        width,
                        height,
                        det.box().xmin(),
                        det.box().ymin(),
                        det.box().xmax(),
                        det.box().ymax(),
                        det.confidence(),
                        det.visualAreaRatio()
                );

                // Find best matching active track
                PotholeTrack bestTrack = null;
                double bestMatchScore = -1.0;

                for (PotholeTrack track : activeTracks) {
                    TrackedDetection lastDet = track.getLastDetection();
                    if (lastDet == null) {
                        continue;
                    }

                    double timeGap = newDet.timestampSec() - lastDet.timestampSec();
                    // Must be within temporal window
                    if (timeGap < 0.0 || timeGap > MAX_TEMPORAL_GAP_SEC) {
                        continue;
                    }

                    double iou = calculateIoU(newDet, lastDet);
                    double normDist = calculateNormalizedCentroidDistance(newDet, lastDet);

                    boolean isMatch = (iou >= IOU_MATCH_THRESHOLD) || (normDist <= CENTROID_DIST_THRESHOLD);

                    if (isMatch) {
                        // Higher score for higher IoU or lower centroid distance
                        double score = iou + (1.0 - normDist);
                        if (score > bestMatchScore) {
                            bestMatchScore = score;
                            bestTrack = track;
                        }
                    }
                }

                if (bestTrack != null) {
                    bestTrack.addDetection(newDet);
                    log.debug("Matched detection at t={}s to existing track [size={}] (score={})",
                            timeSec, bestTrack.size(), bestMatchScore);
                } else {
                    PotholeTrack newTrack = new PotholeTrack(newDet);
                    activeTracks.add(newTrack);
                    log.debug("Created new pothole track at t={}s", timeSec);
                }
            }
        }

        log.info("Type A Aggregation complete: Processed {} frames, formed {} distinct pothole track(s)",
                sortedFrames.size(), activeTracks.size());

        return activeTracks;
    }

    /**
     * Calculates Intersection over Union (IoU) between two bounding boxes.
     */
    public double calculateIoU(TrackedDetection d1, TrackedDetection d2) {
        int interLeft = Math.max(d1.xmin(), d2.xmin());
        int interTop = Math.max(d1.ymin(), d2.ymin());
        int interRight = Math.min(d1.xmax(), d2.xmax());
        int interBottom = Math.min(d1.ymax(), d2.ymax());

        int interWidth = Math.max(0, interRight - interLeft);
        int interHeight = Math.max(0, interBottom - interTop);
        double interArea = (double) interWidth * interHeight;

        double unionArea = d1.area() + d2.area() - interArea;
        if (unionArea <= 0.0) {
            return 0.0;
        }

        return interArea / unionArea;
    }

    /**
     * Calculates Euclidean distance between box centroids normalized to image dimensions.
     */
    public double calculateNormalizedCentroidDistance(TrackedDetection d1, TrackedDetection d2) {
        double imgW = Math.max(d1.imageWidth(), 1);
        double imgH = Math.max(d1.imageHeight(), 1);

        double dx = (d1.centroidX() - d2.centroidX()) / imgW;
        double dy = (d1.centroidY() - d2.centroidY()) / imgH;

        return Math.sqrt(dx * dx + dy * dy);
    }
}
