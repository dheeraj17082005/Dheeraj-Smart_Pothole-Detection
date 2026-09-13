package com.pothole.service.detection;

import com.pothole.dto.*;
import com.pothole.dto.ai.AiAnnotatedDetectionResult;
import com.pothole.dto.ai.AiBoundingBox;
import com.pothole.exception.InvalidMediaException;
import com.pothole.model.CivicAuthority;
import com.pothole.model.Detection;
import com.pothole.model.Pothole;
import com.pothole.model.User;
import com.pothole.service.ai.AiDetectionService;
import com.pothole.service.authority.AuthorityResolutionResult;
import com.pothole.service.authority.AuthorityResolverService;
import com.pothole.service.deduplication.DeduplicationResult;
import com.pothole.service.deduplication.DeduplicationService;
import com.pothole.service.severity.SeverityResult;
import com.pothole.service.severity.SeverityService;
import com.pothole.service.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageDetectionService {

    private static final Logger log = LoggerFactory.getLogger(ImageDetectionService.class);
    private static final long MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024; // 25 MB
    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final StorageService storageService;
    private final AiDetectionService aiDetectionService;
    private final ImageDetectionPersistenceService persistenceService;
    private final SeverityService severityService;
    private final AuthorityResolverService authorityResolverService;
    private final DeduplicationService deduplicationService;
    private final com.pothole.service.reporting.AuthorityReportingService authorityReportingService;

    public ImageDetectionService(
            StorageService storageService,
            AiDetectionService aiDetectionService,
            ImageDetectionPersistenceService persistenceService,
            SeverityService severityService,
            AuthorityResolverService authorityResolverService,
            DeduplicationService deduplicationService,
            com.pothole.service.reporting.AuthorityReportingService authorityReportingService
    ) {
        this.storageService = storageService;
        this.aiDetectionService = aiDetectionService;
        this.persistenceService = persistenceService;
        this.severityService = severityService;
        this.authorityResolverService = authorityResolverService;
        this.deduplicationService = deduplicationService;
        this.authorityReportingService = authorityReportingService;
    }

    public DetectImageResponse processImageDetection(DetectImageRequest request) {
        return processImageDetection(request, null);
    }

    public DetectImageResponse processImageDetection(DetectImageRequest request, User user) {
        // 1. Validate request & media
        validateMediaFile(request.file());
        validateCoordinates(request.latitude(), request.longitude());

        MultipartFile file = request.file();
        String originalFilename = file.getOriginalFilename();
        String contentType = (file.getContentType() != null) ? file.getContentType() : "image/jpeg";
        long fileSize = file.getSize();

        // 2. Upload raw image to MinIO (pothole-raw)
        String rawKey = storageService.generateRawObjectKey(originalFilename);
        log.info("Storing raw uploaded image in [{}/{}]", storageService.getRawBucket(), rawKey);
        try {
            storageService.putObject(
                    storageService.getRawBucket(),
                    rawKey,
                    file.getInputStream(),
                    fileSize,
                    contentType
            );
        } catch (IOException e) {
            throw new InvalidMediaException("Failed to read image stream for upload: " + e.getMessage());
        }

        // 3. Call AI service for inference & annotation
        Instant startedAt = Instant.now();
        AiAnnotatedDetectionResult aiResult;
        try {
            aiResult = aiDetectionService.detectAndAnnotateImage(
                    file.getResource(),
                    originalFilename,
                    request.confidenceThreshold()
            );
        } catch (Exception e) {
            log.error("AI detection failed for [{}]: {}", rawKey, e.getMessage());
            persistenceService.persistDetectionFailure(rawKey, contentType, fileSize, startedAt, e.getMessage());
            throw e;
        }
        Instant completedAt = Instant.now();

        int potholeCount = aiResult.detectionResponse().potholeCount();
        log.info("AI inference completed for [{}]. Potholes detected: {}", rawKey, potholeCount);

        // 4. Branch based on pothole count
        if (potholeCount == 0) {
            var bundle = persistenceService.persistNoPotholeFound(
                    rawKey,
                    contentType,
                    fileSize,
                    startedAt,
                    completedAt
            );

            JobResponse jobResponse = new JobResponse(
                    bundle.detectionJob().getId(),
                    bundle.detectionJob().getStatus(),
                    bundle.detectionJob().getStartedAt(),
                    bundle.detectionJob().getCompletedAt(),
                    bundle.detectionJob().getErrorSummary()
            );

            return new DetectImageResponse(
                    jobResponse,
                    bundle.mediaAsset().getId(),
                    0,
                    false,
                    null,
                    "No potholes detected in the uploaded image."
            );
        }

        // 5. Store annotated image in MinIO (pothole-annotated)
        String annotatedKey = storageService.generateAnnotatedObjectKey();
        if (aiResult.annotatedImageBytes() != null && aiResult.annotatedImageBytes().length > 0) {
            log.info("Storing annotated image in [{}/{}]", storageService.getAnnotatedBucket(), annotatedKey);
            storageService.putObject(
                    storageService.getAnnotatedBucket(),
                    annotatedKey,
                    aiResult.annotatedImageBytes(),
                    "image/jpeg"
            );
        }

        // 6. Calculate Visual Severity
        int imgWidth = (aiResult.detectionResponse().image() != null) ? aiResult.detectionResponse().image().width() : 1920;
        int imgHeight = (aiResult.detectionResponse().image() != null) ? aiResult.detectionResponse().image().height() : 1080;
        SeverityResult severityResult = severityService.calculateAggregateSeverity(
                aiResult.detectionResponse().detections(),
                imgWidth,
                imgHeight
        );
        log.info("Calculated pothole severity: score={}, class={}", severityResult.severityScore(), severityResult.severityClass());

        // 7. Resolve Civic Authority using PostGIS
        AuthorityResolutionResult authorityResult = authorityResolverService.resolveAuthority(
                request.latitude(),
                request.longitude()
        );
        CivicAuthority resolvedAuthority = authorityResult.authority();

        // 8. Deduplication Check (Type B: Inter-report citizen duplicate detection)
        Instant detectionTime = (request.capturedAt() != null) ? request.capturedAt() : startedAt;
        DeduplicationResult deduplicationResult = deduplicationService.evaluateDuplicate(
                request.latitude(),
                request.longitude(),
                detectionTime
        );

        // 9. Persist domain entities in transaction
        var bundle = persistenceService.persistDetectionSuccess(
                rawKey,
                contentType,
                fileSize,
                request,
                aiResult,
                annotatedKey,
                severityResult,
                resolvedAuthority,
                deduplicationResult,
                startedAt,
                completedAt,
                user
        );

        // 10. Automated Authority Reporting Workflow (STEP 15)
        // Trigger report dispatch if non-duplicate pothole and civic authority resolved
        if (bundle.pothole() != null
                && !Boolean.TRUE.equals(bundle.pothole().getIsDuplicate())
                && bundle.pothole().getCivicAuthority() != null) {
            try {
                authorityReportingService.dispatchReportForPothole(bundle.pothole());
            } catch (Exception e) {
                log.error("Failed to automatically dispatch report for pothole [{}]: {}",
                        bundle.pothole().getId(), e.getMessage());
            }
        }

        // 11. Generate presigned URL for representative annotated image
        String presignedImageUrl = null;
        try {
            presignedImageUrl = storageService.generatePresignedUrl(
                    storageService.getAnnotatedBucket(),
                    annotatedKey
            );
        } catch (Exception e) {
            log.warn("Could not generate presigned URL for annotated image [{}]: {}", annotatedKey, e.getMessage());
        }

        // 11. Map to DTO response
        Pothole pothole = bundle.pothole();
        List<DetectionResponse> detectionResponses = bundle.detections().stream()
                .map(d -> new DetectionResponse(
                        d.getId(),
                        new AiBoundingBox(d.getBoxXmin(), d.getBoxYmin(), d.getBoxXmax(), d.getBoxYmax()),
                        d.getConfidence(),
                        d.getVisualAreaRatio(),
                        d.getFrameIndex(),
                        d.getFrameTimestampSec()
                ))
                .toList();

        AuthorityResponse authorityResponse = (resolvedAuthority != null)
                ? new AuthorityResponse(
                        resolvedAuthority.getId(),
                        resolvedAuthority.getName(),
                        resolvedAuthority.getCode(),
                        resolvedAuthority.getDepartmentType()
                  )
                : null;

        UUID duplicateOfId = (pothole.getDuplicateOf() != null) ? pothole.getDuplicateOf().getId() : null;

        PotholeResponse potholeResponse = new PotholeResponse(
                pothole.getId(),
                pothole.getLocation().getY(), // latitude (Y)
                pothole.getLocation().getX(), // longitude (X)
                pothole.getAddressText(),
                pothole.getFirstDetectedAt(),
                pothole.getSeverityScore(),
                pothole.getSeverityClass(),
                pothole.getMaxConfidence(),
                pothole.getStatus(),
                pothole.getIsDuplicate(),
                duplicateOfId,
                authorityResponse,
                authorityResult.authorityCode(),
                pothole.getRepresentativeKey(),
                presignedImageUrl,
                detectionResponses,
                pothole.getCreatedAt(),
                pothole.getUpdatedAt()
        );

        JobResponse jobResponse = new JobResponse(
                bundle.detectionJob().getId(),
                bundle.detectionJob().getStatus(),
                bundle.detectionJob().getStartedAt(),
                bundle.detectionJob().getCompletedAt(),
                bundle.detectionJob().getErrorSummary()
        );

        return new DetectImageResponse(
                jobResponse,
                bundle.mediaAsset().getId(),
                potholeCount,
                true,
                potholeResponse,
                "Pothole detected and registered successfully."
        );
    }

    private void validateMediaFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidMediaException("Image file must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidMediaException(
                    "Image file size (" + file.getSize() + " bytes) exceeds maximum allowed (" + MAX_FILE_SIZE_BYTES + " bytes)"
            );
        }
        String contentType = file.getContentType();
        if (contentType != null && !SUPPORTED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidMediaException(
                    "Unsupported image content type: " + contentType + ". Supported types: " + SUPPORTED_MIME_TYPES
            );
        }
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || latitude < -90.0 || latitude > 90.0) {
            throw new InvalidMediaException("Latitude must be between -90.0 and 90.0");
        }
        if (longitude == null || longitude < -180.0 || longitude > 180.0) {
            throw new InvalidMediaException("Longitude must be between -180.0 and 180.0");
        }
    }
}
