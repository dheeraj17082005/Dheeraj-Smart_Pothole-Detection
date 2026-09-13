package com.pothole.service.detection;

import com.pothole.dto.DetectImageRequest;
import com.pothole.dto.ai.AiAnnotatedDetectionResult;
import com.pothole.dto.ai.AiDetection;
import com.pothole.model.*;
import com.pothole.model.enums.*;
import com.pothole.repository.*;
import com.pothole.service.deduplication.DeduplicationResult;
import com.pothole.service.severity.SeverityResult;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImageDetectionPersistenceService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final MediaAssetRepository mediaAssetRepository;
    private final DetectionJobRepository detectionJobRepository;
    private final PotholeRepository potholeRepository;
    private final DetectionRepository detectionRepository;
    private final PotholeStatusHistoryRepository statusHistoryRepository;

    public ImageDetectionPersistenceService(
            MediaAssetRepository mediaAssetRepository,
            DetectionJobRepository detectionJobRepository,
            PotholeRepository potholeRepository,
            DetectionRepository detectionRepository,
            PotholeStatusHistoryRepository statusHistoryRepository
    ) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.detectionJobRepository = detectionJobRepository;
        this.potholeRepository = potholeRepository;
        this.detectionRepository = detectionRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    public record SavedDetectionBundle(
            MediaAsset mediaAsset,
            DetectionJob detectionJob,
            Pothole pothole,
            List<Detection> detections,
            PotholeStatusHistory statusHistory
    ) {}

    @Transactional
    public SavedDetectionBundle persistDetectionSuccess(
            String rawObjectKey,
            String mimeType,
            long fileSize,
            DetectImageRequest request,
            AiAnnotatedDetectionResult aiResult,
            String representativeKey,
            SeverityResult severityResult,
            CivicAuthority resolvedAuthority,
            DeduplicationResult deduplicationResult,
            Instant startedAt,
            Instant completedAt,
            User user
    ) {
        // 1. Persist MediaAsset
        MediaAsset mediaAsset = new MediaAsset(MediaType.IMAGE, rawObjectKey, mimeType, fileSize);
        MediaAsset savedMedia = mediaAssetRepository.save(mediaAsset);

        // 2. Persist DetectionJob
        DetectionJob detectionJob = new DetectionJob(savedMedia, DetectionJobStatus.COMPLETED);
        detectionJob.setStartedAt(startedAt);
        detectionJob.setCompletedAt(completedAt);
        DetectionJob savedJob = detectionJobRepository.save(detectionJob);

        // 3. Persist Pothole
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(request.longitude(), request.latitude()));
        Instant detectionTime = (request.capturedAt() != null) ? request.capturedAt() : startedAt;

        Pothole pothole = new Pothole(
                location,
                severityResult.severityScore(),
                severityResult.severityClass(),
                aiResult.detectionResponse().maxConfidence(),
                representativeKey
        );
        pothole.setAddressText(request.addressText());
        pothole.setFirstDetectedAt(detectionTime);
        pothole.setStatus(PotholeStatus.REPORTED);
        pothole.setCivicAuthority(resolvedAuthority);
        pothole.setUser(user);

        if (deduplicationResult != null && deduplicationResult.isDuplicate()) {
            pothole.setIsDuplicate(true);
            pothole.setDuplicateOf(deduplicationResult.parentPothole());
        } else {
            pothole.setIsDuplicate(false);
            pothole.setDuplicateOf(null);
        }

        Pothole savedPothole = potholeRepository.save(pothole);

        // 4. Persist Detections linked to DetectionJob and Pothole
        List<Detection> savedDetections = new ArrayList<>();
        for (AiDetection aiDet : aiResult.detectionResponse().detections()) {
            Detection detection = new Detection(
                    savedJob,
                    aiDet.box().xmin(),
                    aiDet.box().ymin(),
                    aiDet.box().xmax(),
                    aiDet.box().ymax(),
                    aiDet.confidence(),
                    aiDet.visualAreaRatio()
            );
            detection.setPothole(savedPothole);
            detection.setFrameIndex(0);
            detection.setFrameTimestampSec(0.0);
            savedDetections.add(detectionRepository.save(detection));
        }

        // 5. Persist Initial Status History
        String auditNotes = (deduplicationResult != null && deduplicationResult.isDuplicate())
                ? "Duplicate submission of existing pothole " + deduplicationResult.duplicateOfId()
                : "Initial detection from image upload";

        PotholeStatusHistory history = new PotholeStatusHistory(
                savedPothole,
                null,
                PotholeStatus.REPORTED,
                "SYSTEM",
                auditNotes
        );
        PotholeStatusHistory savedHistory = statusHistoryRepository.save(history);

        return new SavedDetectionBundle(savedMedia, savedJob, savedPothole, savedDetections, savedHistory);
    }

    @Transactional
    public SavedDetectionBundle persistNoPotholeFound(
            String rawObjectKey,
            String mimeType,
            long fileSize,
            Instant startedAt,
            Instant completedAt
    ) {
        MediaAsset mediaAsset = new MediaAsset(MediaType.IMAGE, rawObjectKey, mimeType, fileSize);
        MediaAsset savedMedia = mediaAssetRepository.save(mediaAsset);

        DetectionJob detectionJob = new DetectionJob(savedMedia, DetectionJobStatus.COMPLETED);
        detectionJob.setStartedAt(startedAt);
        detectionJob.setCompletedAt(completedAt);
        DetectionJob savedJob = detectionJobRepository.save(detectionJob);

        return new SavedDetectionBundle(savedMedia, savedJob, null, List.of(), null);
    }

    @Transactional
    public void persistDetectionFailure(
            String rawObjectKey,
            String mimeType,
            long fileSize,
            Instant startedAt,
            String errorSummary
    ) {
        MediaAsset mediaAsset = new MediaAsset(MediaType.IMAGE, rawObjectKey, mimeType, fileSize);
        MediaAsset savedMedia = mediaAssetRepository.save(mediaAsset);

        DetectionJob detectionJob = new DetectionJob(savedMedia, DetectionJobStatus.FAILED);
        detectionJob.setStartedAt(startedAt);
        detectionJob.setCompletedAt(Instant.now());
        detectionJob.setErrorSummary(errorSummary);
        detectionJobRepository.save(detectionJob);
    }
}
