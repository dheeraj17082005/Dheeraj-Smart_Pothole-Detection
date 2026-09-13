package com.pothole.model;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "detections")
public class Detection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detection_job_id", nullable = false)
    private DetectionJob detectionJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pothole_id")
    private Pothole pothole;

    @Column(name = "frame_index")
    private Integer frameIndex = 0;

    @Column(name = "frame_timestamp_sec")
    private Double frameTimestampSec = 0.0;

    @Column(name = "box_xmin", nullable = false)
    private Integer boxXmin;

    @Column(name = "box_ymin", nullable = false)
    private Integer boxYmin;

    @Column(name = "box_xmax", nullable = false)
    private Integer boxXmax;

    @Column(name = "box_ymax", nullable = false)
    private Integer boxYmax;

    @Column(name = "confidence", nullable = false)
    private Double confidence;

    @Column(name = "visual_area_ratio", nullable = false)
    private Double visualAreaRatio;

    public Detection() {
    }

    public Detection(DetectionJob detectionJob, Integer boxXmin, Integer boxYmin, Integer boxXmax, Integer boxYmax, Double confidence, Double visualAreaRatio) {
        this.detectionJob = detectionJob;
        this.boxXmin = boxXmin;
        this.boxYmin = boxYmin;
        this.boxXmax = boxXmax;
        this.boxYmax = boxYmax;
        this.confidence = confidence;
        this.visualAreaRatio = visualAreaRatio;
        this.frameIndex = 0;
        this.frameTimestampSec = 0.0;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DetectionJob getDetectionJob() {
        return detectionJob;
    }

    public void setDetectionJob(DetectionJob detectionJob) {
        this.detectionJob = detectionJob;
    }

    public Pothole getPothole() {
        return pothole;
    }

    public void setPothole(Pothole pothole) {
        this.pothole = pothole;
    }

    public Integer getFrameIndex() {
        return frameIndex;
    }

    public void setFrameIndex(Integer frameIndex) {
        this.frameIndex = frameIndex;
    }

    public Double getFrameTimestampSec() {
        return frameTimestampSec;
    }

    public void setFrameTimestampSec(Double frameTimestampSec) {
        this.frameTimestampSec = frameTimestampSec;
    }

    public Integer getBoxXmin() {
        return boxXmin;
    }

    public void setBoxXmin(Integer boxXmin) {
        this.boxXmin = boxXmin;
    }

    public Integer getBoxYmin() {
        return boxYmin;
    }

    public void setBoxYmin(Integer boxYmin) {
        this.boxYmin = boxYmin;
    }

    public Integer getBoxXmax() {
        return boxXmax;
    }

    public void setBoxXmax(Integer boxXmax) {
        this.boxXmax = boxXmax;
    }

    public Integer getBoxYmax() {
        return boxYmax;
    }

    public void setBoxYmax(Integer boxYmax) {
        this.boxYmax = boxYmax;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Double getVisualAreaRatio() {
        return visualAreaRatio;
    }

    public void setVisualAreaRatio(Double visualAreaRatio) {
        this.visualAreaRatio = visualAreaRatio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Detection detection = (Detection) o;
        return id != null && Objects.equals(id, detection.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Detection{" +
                "id=" + id +
                ", frameIndex=" + frameIndex +
                ", confidence=" + confidence +
                ", visualAreaRatio=" + visualAreaRatio +
                '}';
    }
}
