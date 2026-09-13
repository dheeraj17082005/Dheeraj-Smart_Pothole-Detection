package com.pothole.service.video;

public record TrackedDetection(
        int frameIndex,
        double timestampSec,
        int imageWidth,
        int imageHeight,
        int xmin,
        int ymin,
        int xmax,
        int ymax,
        double confidence,
        double visualAreaRatio
) {
    public double centroidX() {
        return (xmin + xmax) / 2.0;
    }

    public double centroidY() {
        return (ymin + ymax) / 2.0;
    }

    public double area() {
        return Math.max(0, xmax - xmin) * (double) Math.max(0, ymax - ymin);
    }
}
