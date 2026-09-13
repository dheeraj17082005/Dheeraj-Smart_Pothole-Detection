package com.pothole.service.video;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PotholeTrack {

    private final List<TrackedDetection> detections = new ArrayList<>();

    public PotholeTrack() {
    }

    public PotholeTrack(TrackedDetection initialDetection) {
        this.detections.add(initialDetection);
    }

    public void addDetection(TrackedDetection detection) {
        this.detections.add(detection);
    }

    public TrackedDetection getLastDetection() {
        if (detections.isEmpty()) {
            return null;
        }
        return detections.get(detections.size() - 1);
    }

    public List<TrackedDetection> getDetections() {
        return detections;
    }

    public int size() {
        return detections.size();
    }

    public double getMaxConfidence() {
        return detections.stream()
                .mapToDouble(TrackedDetection::confidence)
                .max()
                .orElse(0.0);
    }

    public double getMaxVisualAreaRatio() {
        return detections.stream()
                .mapToDouble(TrackedDetection::visualAreaRatio)
                .max()
                .orElse(0.0);
    }

    public double getFirstTimestampSec() {
        return detections.stream()
                .mapToDouble(TrackedDetection::timestampSec)
                .min()
                .orElse(0.0);
    }

    public TrackedDetection getRepresentativeDetection() {
        if (detections.isEmpty()) {
            return null;
        }
        return detections.stream()
                .max(Comparator.comparingDouble((TrackedDetection d) -> d.visualAreaRatio() * d.confidence())
                        .thenComparingDouble(TrackedDetection::visualAreaRatio)
                        .thenComparingDouble(TrackedDetection::confidence))
                .orElse(detections.get(0));
    }
}
