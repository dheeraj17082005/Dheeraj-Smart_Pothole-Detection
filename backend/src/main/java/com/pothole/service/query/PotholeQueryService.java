package com.pothole.service.query;

import com.pothole.dto.*;
import com.pothole.dto.ai.AiBoundingBox;
import com.pothole.dto.reporting.ReportResponse;
import com.pothole.exception.ObjectNotFoundException;
import com.pothole.model.*;
import com.pothole.model.enums.PotholeStatus;
import com.pothole.model.enums.SeverityClass;
import com.pothole.repository.*;
import com.pothole.service.storage.StorageService;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PotholeQueryService {

    private static final Logger log = LoggerFactory.getLogger(PotholeQueryService.class);

    private final PotholeRepository potholeRepository;
    private final PotholeStatusHistoryRepository historyRepository;
    private final DetectionRepository detectionRepository;
    private final CivicAuthorityRepository authorityRepository;
    private final ReportRepository reportRepository;
    private final StorageService storageService;

    public PotholeQueryService(
            PotholeRepository potholeRepository,
            PotholeStatusHistoryRepository historyRepository,
            DetectionRepository detectionRepository,
            CivicAuthorityRepository authorityRepository,
            ReportRepository reportRepository,
            StorageService storageService
    ) {
        this.potholeRepository = potholeRepository;
        this.historyRepository = historyRepository;
        this.detectionRepository = detectionRepository;
        this.authorityRepository = authorityRepository;
        this.reportRepository = reportRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public List<PotholeMapMarkerResponse> getPotholesMap(Double minLat, Double minLng, Double maxLat, Double maxLng, Integer limit) {
        int maxResults = (limit != null && limit > 0) ? Math.min(limit, 500) : 200;
        List<Pothole> potholes = potholeRepository.findWithinBoundingBox(minLng, minLat, maxLng, maxLat, maxResults);

        return potholes.stream().map(p -> new PotholeMapMarkerResponse(
                p.getId(),
                p.getLocation().getY(),
                p.getLocation().getX(),
                p.getSeverityClass(),
                p.getSeverityScore(),
                p.getStatus(),
                p.getMaxConfidence(),
                p.getFirstDetectedAt(),
                (p.getCivicAuthority() != null) ? p.getCivicAuthority().getName() : null,
                (p.getCivicAuthority() != null) ? p.getCivicAuthority().getCode() : "UNKNOWN_AUTHORITY",
                p.getIsDuplicate()
        )).toList();
    }

    @Transactional(readOnly = true)
    public Page<PotholeResponse> getPotholes(
            PotholeStatus status,
            SeverityClass severity,
            String authority,
            Instant fromDate,
            Instant toDate,
            Pageable pageable
    ) {
        Specification<Pothole> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (severity != null) {
                predicates.add(cb.equal(root.get("severityClass"), severity));
            }
            if (authority != null && !authority.isBlank()) {
                if ("UNKNOWN_AUTHORITY".equalsIgnoreCase(authority)) {
                    predicates.add(cb.isNull(root.get("civicAuthority")));
                } else {
                    var authorityJoin = root.join("civicAuthority", JoinType.LEFT);
                    predicates.add(cb.or(
                            cb.equal(cb.lower(authorityJoin.get("code")), authority.toLowerCase()),
                            cb.equal(cb.lower(authorityJoin.get("name")), authority.toLowerCase())
                    ));
                }
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("firstDetectedAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("firstDetectedAt"), toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Pothole> potholePage = potholeRepository.findAll(spec, pageable);
        return potholePage.map(this::mapToPotholeResponse);
    }

    @Transactional(readOnly = true)
    public PotholeDetailResponse getPotholeDetail(UUID id) {
        Pothole pothole = potholeRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Pothole not found with ID: " + id));

        return buildPotholeDetail(pothole);
    }

    @Transactional(readOnly = true)
    public List<PotholeStatusHistoryResponse> getPotholeHistory(UUID id) {
        if (!potholeRepository.existsById(id)) {
            throw new ObjectNotFoundException("Pothole not found with ID: " + id);
        }
        return historyRepository.findByPotholeIdOrderByChangedAtDesc(id).stream()
                .map(PotholeStatusHistoryResponse::fromEntity)
                .toList();
    }

    @Transactional
    public PotholeDetailResponse updatePotholeStatus(UUID id, UpdatePotholeStatusRequest request) {
        Pothole pothole = potholeRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Pothole not found with ID: " + id));

        PotholeStatus currentStatus = pothole.getStatus();
        PotholeStatus requestedStatus = request.newStatus();

        boolean validTransition = switch (currentStatus) {
            case SUBMITTED, PENDING_OFFICER_REVIEW, REPORTED -> requestedStatus == PotholeStatus.ACCEPTED || requestedStatus == PotholeStatus.ACKNOWLEDGED || requestedStatus == PotholeStatus.REJECTED;
            case ACCEPTED, ACKNOWLEDGED -> requestedStatus == PotholeStatus.IN_PROGRESS || requestedStatus == PotholeStatus.REJECTED;
            case IN_PROGRESS -> requestedStatus == PotholeStatus.RESOLVED;
            case RESOLVED, REJECTED -> false;
            default -> false;
        };

        if (!validTransition) {
            throw new IllegalArgumentException(
                    "Invalid status transition from " + currentStatus + " to " + requestedStatus +
                    ". Allowed flow: REPORTED -> ACKNOWLEDGED -> IN_PROGRESS -> RESOLVED."
            );
        }

        PotholeStatusHistory history = new PotholeStatusHistory(
                pothole,
                currentStatus,
                requestedStatus,
                (request.changedBy() != null && !request.changedBy().isBlank()) ? request.changedBy() : "OPERATOR",
                request.notes()
        );
        historyRepository.save(history);

        pothole.setStatus(requestedStatus);
        Pothole saved = potholeRepository.save(pothole);

        log.info("Transitioned pothole [{}] status from {} to {} by {}", id, currentStatus, requestedStatus, history.getChangedBy());

        return buildPotholeDetail(saved);
    }

    @Transactional(readOnly = true)
    public List<AuthorityResponse> getAllAuthorities() {
        return authorityRepository.findAll().stream()
                .map(a -> new AuthorityResponse(a.getId(), a.getName(), a.getCode(), a.getDepartmentType()))
                .toList();
    }

    private PotholeResponse mapToPotholeResponse(Pothole pothole) {
        String presignedImageUrl = null;
        if (pothole.getRepresentativeKey() != null) {
            try {
                presignedImageUrl = storageService.generatePresignedUrl(
                        storageService.getAnnotatedBucket(),
                        pothole.getRepresentativeKey()
                );
            } catch (Exception e) {
                log.warn("Could not generate presigned URL for pothole [{}]: {}", pothole.getId(), e.getMessage());
            }
        }

        AuthorityResponse authorityResponse = (pothole.getCivicAuthority() != null)
                ? new AuthorityResponse(
                        pothole.getCivicAuthority().getId(),
                        pothole.getCivicAuthority().getName(),
                        pothole.getCivicAuthority().getCode(),
                        pothole.getCivicAuthority().getDepartmentType()
                  )
                : null;

        String authorityCode = (pothole.getCivicAuthority() != null)
                ? pothole.getCivicAuthority().getCode()
                : "UNKNOWN_AUTHORITY";

        UUID duplicateOfId = (pothole.getDuplicateOf() != null) ? pothole.getDuplicateOf().getId() : null;

        List<Detection> detections = detectionRepository.findByPothole(pothole);
        List<DetectionResponse> detectionResponses = detections.stream()
                .map(d -> new DetectionResponse(
                        d.getId(),
                        new AiBoundingBox(d.getBoxXmin(), d.getBoxYmin(), d.getBoxXmax(), d.getBoxYmax()),
                        d.getConfidence(),
                        d.getVisualAreaRatio(),
                        d.getFrameIndex(),
                        d.getFrameTimestampSec()
                ))
                .toList();

        return new PotholeResponse(
                pothole.getId(),
                pothole.getLocation().getY(),
                pothole.getLocation().getX(),
                pothole.getAddressText(),
                pothole.getFirstDetectedAt(),
                pothole.getSeverityScore(),
                pothole.getSeverityClass(),
                pothole.getMaxConfidence(),
                pothole.getStatus(),
                pothole.getIsDuplicate(),
                duplicateOfId,
                authorityResponse,
                authorityCode,
                pothole.getRepresentativeKey(),
                presignedImageUrl,
                detectionResponses,
                pothole.getCreatedAt(),
                pothole.getUpdatedAt()
        );
    }

    private PotholeDetailResponse buildPotholeDetail(Pothole pothole) {
        String repImageUrl = null;
        if (pothole.getRepresentativeKey() != null) {
            try {
                repImageUrl = storageService.generatePresignedUrl(
                        storageService.getAnnotatedBucket(),
                        pothole.getRepresentativeKey()
                );
            } catch (Exception e) {
                log.warn("Could not presign annotated key [{}]: {}", pothole.getRepresentativeKey(), e.getMessage());
            }
        }

        List<Detection> detections = detectionRepository.findByPothole(pothole);

        String rawMediaUrl = null;
        String rawKey = null;
        String mediaType = "IMAGE";

        if (!detections.isEmpty()) {
            Detection firstDetection = detections.get(0);
            if (firstDetection.getDetectionJob() != null && firstDetection.getDetectionJob().getMediaAsset() != null) {
                MediaAsset mediaAsset = firstDetection.getDetectionJob().getMediaAsset();
                rawKey = mediaAsset.getRawObjectKey();
                if (mediaAsset.getMediaType() != null) {
                    mediaType = mediaAsset.getMediaType().name();
                }
                if (rawKey != null) {
                    try {
                        rawMediaUrl = storageService.generatePresignedUrl(storageService.getRawBucket(), rawKey);
                    } catch (Exception e) {
                        log.warn("Could not presign raw media key [{}]: {}", rawKey, e.getMessage());
                    }
                }
            }
        }

        PotholeDetailResponse.PotholeEvidence evidence = new PotholeDetailResponse.PotholeEvidence(
                repImageUrl,
                pothole.getRepresentativeKey(),
                rawMediaUrl,
                rawKey,
                mediaType
        );

        AuthorityResponse authorityDto = (pothole.getCivicAuthority() != null)
                ? new AuthorityResponse(
                        pothole.getCivicAuthority().getId(),
                        pothole.getCivicAuthority().getName(),
                        pothole.getCivicAuthority().getCode(),
                        pothole.getCivicAuthority().getDepartmentType()
                  )
                : null;

        String authorityCode = (pothole.getCivicAuthority() != null)
                ? pothole.getCivicAuthority().getCode()
                : "UNKNOWN_AUTHORITY";

        UUID duplicateOfId = (pothole.getDuplicateOf() != null) ? pothole.getDuplicateOf().getId() : null;

        Optional<Report> reportOpt = reportRepository.findByPotholeIdWithDetails(pothole.getId());
        ReportResponse reportDto = reportOpt.map(ReportResponse::fromEntity).orElse(null);

        List<DetectionResponse> detectionResponses = detections.stream()
                .map(d -> new DetectionResponse(
                        d.getId(),
                        new AiBoundingBox(d.getBoxXmin(), d.getBoxYmin(), d.getBoxXmax(), d.getBoxYmax()),
                        d.getConfidence(),
                        d.getVisualAreaRatio(),
                        d.getFrameIndex(),
                        d.getFrameTimestampSec()
                ))
                .toList();

        return new PotholeDetailResponse(
                pothole.getId(),
                pothole.getLocation().getY(),
                pothole.getLocation().getX(),
                pothole.getAddressText(),
                pothole.getFirstDetectedAt(),
                pothole.getSeverityScore(),
                pothole.getSeverityClass(),
                pothole.getMaxConfidence(),
                pothole.getStatus(),
                pothole.getIsDuplicate(),
                duplicateOfId,
                authorityDto,
                authorityCode,
                evidence,
                reportDto,
                detectionResponses,
                pothole.getCreatedAt(),
                pothole.getUpdatedAt()
        );
    }
}
