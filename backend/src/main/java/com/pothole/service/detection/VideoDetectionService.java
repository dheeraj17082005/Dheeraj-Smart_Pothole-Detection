package com.pothole.service.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pothole.dto.DetectVideoAcceptedResponse;
import com.pothole.dto.JobDetailResponse;
import com.pothole.dto.JobResultSummary;
import com.pothole.dto.ai.*;
import com.pothole.exception.InvalidMediaException;
import com.pothole.exception.ObjectNotFoundException;
import com.pothole.model.*;
import com.pothole.model.enums.DetectionJobStatus;
import com.pothole.model.enums.MediaType;
import com.pothole.model.enums.PotholeStatus;
import com.pothole.repository.*;
import com.pothole.service.ai.AiDetectionService;
import com.pothole.service.authority.AuthorityResolutionResult;
import com.pothole.service.authority.AuthorityResolverService;
import com.pothole.service.deduplication.DeduplicationResult;
import com.pothole.service.deduplication.DeduplicationService;
import com.pothole.service.reporting.AuthorityReportingService;
import com.pothole.service.severity.SeverityResult;
import com.pothole.service.severity.SeverityService;
import com.pothole.service.storage.StorageService;
import com.pothole.service.video.PotholeTrack;
import com.pothole.service.video.TrackedDetection;
import com.pothole.service.video.VideoAggregationService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executor;

@Service
public class VideoDetectionService {

    private static final Logger log = LoggerFactory.getLogger(VideoDetectionService.class);
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    public static final long MAX_VIDEO_SIZE_BYTES = 200 * 1024 * 1024; // 200 MB
    public static final Set<String> SUPPORTED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/webm", "video/quicktime", "video/x-matroska", "application/octet-stream"
    );

    private final StorageService storageService;
    private final AiDetectionService aiDetectionService;
    private final VideoAggregationService aggregationService;
    private final SeverityService severityService;
    private final AuthorityResolverService authorityResolverService;
    private final DeduplicationService deduplicationService;
    private final AuthorityReportingService authorityReportingService;
    private final MediaAssetRepository mediaAssetRepository;
    private final DetectionJobRepository detectionJobRepository;
    private final DetectionRepository detectionRepository;
    private final PotholeRepository potholeRepository;
    private final PotholeStatusHistoryRepository statusHistoryRepository;
    private final Executor videoProcessingExecutor;
    private final ObjectMapper objectMapper;

    public VideoDetectionService(
            StorageService storageService,
            AiDetectionService aiDetectionService,
            VideoAggregationService aggregationService,
            SeverityService severityService,
            AuthorityResolverService authorityResolverService,
            DeduplicationService deduplicationService,
            AuthorityReportingService authorityReportingService,
            MediaAssetRepository mediaAssetRepository,
            DetectionJobRepository detectionJobRepository,
            DetectionRepository detectionRepository,
            PotholeRepository potholeRepository,
            PotholeStatusHistoryRepository statusHistoryRepository,
            @Qualifier("videoProcessingExecutor") Executor videoProcessingExecutor,
            ObjectMapper objectMapper
    ) {
        this.storageService = storageService;
        this.aiDetectionService = aiDetectionService;
        this.aggregationService = aggregationService;
        this.severityService = severityService;
        this.authorityResolverService = authorityResolverService;
        this.deduplicationService = deduplicationService;
        this.authorityReportingService = authorityReportingService;
        this.mediaAssetRepository = mediaAssetRepository;
        this.detectionJobRepository = detectionJobRepository;
        this.detectionRepository = detectionRepository;
        this.potholeRepository = potholeRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.videoProcessingExecutor = videoProcessingExecutor;
        this.objectMapper = objectMapper;
    }

    /**
     * Validates uploaded video, stores raw media in MinIO, creates PENDING job, and triggers asynchronous execution.
     * Returns immediately with HTTP 202 payload without waiting for video inference.
     */
    public DetectVideoAcceptedResponse initiateVideoJob(
            MultipartFile file,
            Double latitude,
            Double longitude,
            Instant capturedAt,
            String addressText
    ) {
        validateVideoUpload(file, latitude, longitude);

        byte[] videoBytes;
        try {
            videoBytes = file.getBytes();
        } catch (IOException e) {
            throw new InvalidMediaException("Could not read uploaded video bytes: " + e.getMessage());
        }

        String rawObjectKey = storageService.generateRawObjectKey(file.getOriginalFilename());
        String contentType = (file.getContentType() != null) ? file.getContentType() : "video/mp4";

        log.info("Storing uploaded raw video [size={} bytes] in MinIO bucket [{}/{}]",
                file.getSize(), storageService.getRawBucket(), rawObjectKey);

        storageService.putObject(
                storageService.getRawBucket(),
                rawObjectKey,
                videoBytes,
                contentType
        );

        // Persist initial MediaAsset and DetectionJob in short transaction
        DetectionJob pendingJob = createPendingJob(rawObjectKey, contentType, file.getSize());

        // Dispatch background processing to ThreadPoolTaskExecutor
        final UUID jobId = pendingJob.getId();
        final String originalFilename = file.getOriginalFilename();
        final Instant effectiveCaptureTime = (capturedAt != null) ? capturedAt : Instant.now();

        videoProcessingExecutor.execute(() -> {
            try {
                processVideoJobAsync(jobId, videoBytes, originalFilename, rawObjectKey, latitude, longitude, effectiveCaptureTime, addressText);
            } catch (Exception e) {
                log.error("Fatal unhandled error in async video processing for job [{}]: {}", jobId, e.getMessage(), e);
                markJobFailed(jobId, "Video processing failed: " + e.getMessage());
            }
        });

        return new DetectVideoAcceptedResponse(
                jobId,
                DetectionJobStatus.PENDING,
                pendingJob.getMediaAsset().getId(),
                "/api/v1/detection-jobs/" + jobId
        );
    }

    @Transactional
    public DetectionJob createPendingJob(String rawObjectKey, String contentType, long fileSize) {
        MediaAsset mediaAsset = new MediaAsset(MediaType.VIDEO, rawObjectKey, contentType, fileSize);
        MediaAsset savedMedia = mediaAssetRepository.save(mediaAsset);

        DetectionJob job = new DetectionJob(savedMedia, DetectionJobStatus.PENDING);
        return detectionJobRepository.save(job);
    }

    /**
     * Executes the end-to-end asynchronous video inference and Type A aggregation workflow.
     */
    public void processVideoJobAsync(
            UUID jobId,
            byte[] videoBytes,
            String originalFilename,
            String rawObjectKey,
            Double latitude,
            Double longitude,
            Instant capturedAt,
            String addressText
    ) {
        log.info("Starting asynchronous processing for video detection job [{}]", jobId);

        // 1. Atomically transition job status PENDING -> PROCESSING
        if (!transitionJobToProcessing(jobId)) {
            log.warn("Job [{}] was not in PENDING status. Skipping duplicate execution.", jobId);
            return;
        }

        try {
            // 2. Call FastAPI AI microservice POST /detect/video (2.0 FPS sampling)
            ByteArrayResource videoResource = new ByteArrayResource(videoBytes);
            AiVideoDetectionResponse aiResponse = aiDetectionService.detectVideo(
                    videoResource,
                    originalFilename,
                    2.0,
                    0.25
            );

            log.info("AI video detection completed for job [{}]. Sampled {} frames.",
                    jobId, (aiResponse.video() != null ? aiResponse.video().totalFramesSampled() : aiResponse.frames().size()));

            // 3. Perform Type A Intra-Video Aggregation
            List<PotholeTrack> tracks = aggregationService.aggregateDetectionsIntoTracks(aiResponse.frames());

            int totalSampled = (aiResponse.video() != null && aiResponse.video().totalFramesSampled() != null)
                    ? aiResponse.video().totalFramesSampled()
                    : aiResponse.frames().size();

            int framesWithPotholes = (int) aiResponse.frames().stream()
                    .filter(f -> f.detections() != null && !f.detections().isEmpty())
                    .count();

            // 4. Persist aggregated Potholes, Detections, and evaluate reporting in transaction
            JobResultSummary summary = persistAggregatedResults(
                    jobId,
                    videoBytes,
                    originalFilename,
                    tracks,
                    latitude,
                    longitude,
                    capturedAt,
                    addressText,
                    totalSampled,
                    framesWithPotholes
            );

            // 5. Mark DetectionJob COMPLETED with result metrics
            markJobCompleted(jobId, summary);
            log.info("Video detection job [{}] successfully COMPLETED. Potholes created: {}, Duplicates: {}",
                    jobId, summary.potholesCreated(), summary.duplicatesDetected());

        } catch (Exception e) {
            log.error("Failed executing video detection job [{}]: {}", jobId, e.getMessage(), e);
            markJobFailed(jobId, e.getMessage());
        }
    }

    @Transactional
    public boolean transitionJobToProcessing(UUID jobId) {
        DetectionJob job = detectionJobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != DetectionJobStatus.PENDING) {
            return false;
        }
        job.setStatus(DetectionJobStatus.PROCESSING);
        job.setStartedAt(Instant.now());
        detectionJobRepository.saveAndFlush(job);
        return true;
    }

    @Transactional
    public JobResultSummary persistAggregatedResults(
            UUID jobId,
            byte[] videoBytes,
            String originalFilename,
            List<PotholeTrack> tracks,
            Double latitude,
            Double longitude,
            Instant capturedAt,
            String addressText,
            int totalFramesSampled,
            int framesWithPotholes
    ) {
        DetectionJob job = detectionJobRepository.findById(jobId)
                .orElseThrow(() -> new ObjectNotFoundException("Job not found: " + jobId));

        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        int potholesCreated = 0;
        int duplicatesDetected = 0;

        for (PotholeTrack track : tracks) {
            TrackedDetection repDet = track.getRepresentativeDetection();
            if (repDet == null) {
                continue;
            }

            // Extract annotated keyframe from FastAPI and store in MinIO (pothole-annotated)
            String annotatedKey = storageService.generateAnnotatedObjectKey();
            try {
                byte[] annotatedFrameJpg = aiDetectionService.annotateVideoFrame(
                        new ByteArrayResource(videoBytes),
                        originalFilename,
                        repDet.frameIndex(),
                        0.25
                );
                storageService.putObject(
                        storageService.getAnnotatedBucket(),
                        annotatedKey,
                        annotatedFrameJpg,
                        "image/jpeg"
                );
            } catch (Exception e) {
                log.warn("Could not annotate representative keyframe {} for job [{}]: {}",
                        repDet.frameIndex(), jobId, e.getMessage());
            }

            // Calculate Visual Severity for the physical pothole using representative detection metrics
            double repRatio = repDet.visualAreaRatio() > 0.0
                    ? repDet.visualAreaRatio()
                    : severityService.calculateVisualAreaRatio(repDet.xmin(), repDet.ymin(), repDet.xmax(), repDet.ymax(), repDet.imageWidth(), repDet.imageHeight());
            SeverityResult severityResult = severityService.calculateSeverity(
                    repRatio,
                    repDet.confidence()
            );

            // Resolve Civic Authority via PostGIS
            AuthorityResolutionResult authResult = authorityResolverService.resolveAuthority(latitude, longitude);
            CivicAuthority resolvedAuthority = authResult.authority();

            // Calculate effective timestamp
            Instant firstDetectedAt = capturedAt.plusMillis((long) (track.getFirstTimestampSec() * 1000));

            // Evaluate Type B Deduplication (Inter-report citizen duplicate detection)
            DeduplicationResult dedupResult = deduplicationService.evaluateDuplicate(latitude, longitude, firstDetectedAt);

            // Persist Pothole
            Pothole pothole = new Pothole(
                    location,
                    severityResult.severityScore(),
                    severityResult.severityClass(),
                    track.getMaxConfidence(),
                    annotatedKey
            );
            pothole.setAddressText(addressText);
            pothole.setFirstDetectedAt(firstDetectedAt);
            pothole.setStatus(PotholeStatus.REPORTED);
            pothole.setCivicAuthority(resolvedAuthority);

            if (dedupResult.isDuplicate()) {
                pothole.setIsDuplicate(true);
                pothole.setDuplicateOf(dedupResult.parentPothole());
                duplicatesDetected++;
            } else {
                pothole.setIsDuplicate(false);
            }

            Pothole savedPothole = potholeRepository.save(pothole);
            potholesCreated++;

            // Persist frame-level Detection rows linked to DetectionJob and Pothole
            for (TrackedDetection td : track.getDetections()) {
                Detection detection = new Detection(
                        job,
                        td.xmin(),
                        td.ymin(),
                        td.xmax(),
                        td.ymax(),
                        td.confidence(),
                        td.visualAreaRatio()
                );
                detection.setPothole(savedPothole);
                detection.setFrameIndex(td.frameIndex());
                detection.setFrameTimestampSec(td.timestampSec());
                detectionRepository.save(detection);
            }

            // Status Audit History
            String auditNotes = dedupResult.isDuplicate()
                    ? "Duplicate submission of existing pothole " + dedupResult.duplicateOfId() + " from video job " + jobId
                    : "Detected from video upload job " + jobId;

            PotholeStatusHistory history = new PotholeStatusHistory(
                    savedPothole,
                    null,
                    PotholeStatus.REPORTED,
                    "SYSTEM",
                    auditNotes
            );
            statusHistoryRepository.save(history);

            // Automated Authority Reporting (if unique and authority resolved)
            if (!Boolean.TRUE.equals(savedPothole.getIsDuplicate()) && savedPothole.getCivicAuthority() != null) {
                try {
                    authorityReportingService.dispatchReportForPothole(savedPothole);
                } catch (Exception e) {
                    log.error("Failed to dispatch report for video-detected pothole [{}]: {}",
                            savedPothole.getId(), e.getMessage());
                }
            }
        }

        return new JobResultSummary(totalFramesSampled, framesWithPotholes, potholesCreated, duplicatesDetected);
    }

    @Transactional
    public void markJobCompleted(UUID jobId, JobResultSummary summary) {
        DetectionJob job = detectionJobRepository.findById(jobId).orElse(null);
        if (job != null) {
            job.setStatus(DetectionJobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            try {
                job.setResultSummary(objectMapper.writeValueAsString(summary));
            } catch (Exception e) {
                log.warn("Could not serialize job result summary: {}", e.getMessage());
            }
            detectionJobRepository.saveAndFlush(job);
        }
    }

    @Transactional
    public void markJobFailed(UUID jobId, String errorMessage) {
        DetectionJob job = detectionJobRepository.findById(jobId).orElse(null);
        if (job != null) {
            job.setStatus(DetectionJobStatus.FAILED);
            job.setCompletedAt(Instant.now());
            job.setErrorSummary(errorMessage);
            detectionJobRepository.saveAndFlush(job);
        }
    }

    /**
     * Polls the status, progress, and results of an ongoing or completed video detection job.
     */
    @Transactional(readOnly = true)
    public JobDetailResponse getJobDetail(UUID jobId) {
        DetectionJob job = detectionJobRepository.findById(jobId)
                .orElseThrow(() -> new ObjectNotFoundException("Detection job not found with ID: " + jobId));

        Double progress = switch (job.getStatus()) {
            case PENDING -> 0.0;
            case PROCESSING -> 0.5;
            case COMPLETED, FAILED -> 1.0;
        };

        JobResultSummary summary = null;
        if (job.getResultSummary() != null && !job.getResultSummary().isBlank()) {
            try {
                summary = objectMapper.readValue(job.getResultSummary(), JobResultSummary.class);
            } catch (Exception ignored) {
            }
        }

        return new JobDetailResponse(
                job.getId(),
                job.getStatus(),
                job.getStartedAt(),
                job.getCompletedAt(),
                progress,
                summary,
                job.getErrorSummary()
        );
    }

    private void validateVideoUpload(MultipartFile file, Double latitude, Double longitude) {
        if (file == null || file.isEmpty()) {
            throw new InvalidMediaException("Video file is required and cannot be empty.");
        }

        if (file.getSize() > MAX_VIDEO_SIZE_BYTES) {
            throw new InvalidMediaException("Video file exceeds the maximum limit of " + (MAX_VIDEO_SIZE_BYTES / (1024 * 1024)) + " MB.");
        }

        String filename = (file.getOriginalFilename() != null) ? file.getOriginalFilename().toLowerCase() : "";
        String contentType = (file.getContentType() != null) ? file.getContentType().toLowerCase() : "";

        boolean validExt = filename.endsWith(".mp4") || filename.endsWith(".webm") || filename.endsWith(".mov");
        boolean validMime = SUPPORTED_VIDEO_TYPES.contains(contentType);

        if (!validExt && !validMime) {
            throw new InvalidMediaException("Unsupported video format. Only MP4 and WebM videos are supported.");
        }

        if (latitude == null || latitude < -90.0 || latitude > 90.0) {
            throw new InvalidMediaException("Latitude must be a valid number between -90.0 and 90.0.");
        }

        if (longitude == null || longitude < -180.0 || longitude > 180.0) {
            throw new InvalidMediaException("Longitude must be a valid number between -180.0 and 180.0.");
        }
    }
}
