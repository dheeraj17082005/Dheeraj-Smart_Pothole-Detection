package com.pothole.service.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pothole.dto.DetectVideoAcceptedResponse;
import com.pothole.dto.ai.*;
import com.pothole.model.*;
import com.pothole.model.enums.*;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoDetectionServiceTest {

    @Mock
    private StorageService storageService;

    @Mock
    private AiDetectionService aiDetectionService;

    @Mock
    private VideoAggregationService aggregationService;

    @Mock
    private SeverityService severityService;

    @Mock
    private AuthorityResolverService authorityResolverService;

    @Mock
    private DeduplicationService deduplicationService;

    @Mock
    private AuthorityReportingService authorityReportingService;

    @Mock
    private MediaAssetRepository mediaAssetRepository;

    @Mock
    private DetectionJobRepository detectionJobRepository;

    @Mock
    private DetectionRepository detectionRepository;

    @Mock
    private PotholeRepository potholeRepository;

    @Mock
    private PotholeStatusHistoryRepository statusHistoryRepository;

    private final Executor directExecutor = Runnable::run; // Synchronous test executor
    private final ObjectMapper objectMapper = new ObjectMapper();

    private VideoDetectionService videoDetectionService;

    @BeforeEach
    void setUp() {
        videoDetectionService = new VideoDetectionService(
                storageService,
                aiDetectionService,
                aggregationService,
                severityService,
                authorityResolverService,
                deduplicationService,
                authorityReportingService,
                mediaAssetRepository,
                detectionJobRepository,
                detectionRepository,
                potholeRepository,
                statusHistoryRepository,
                directExecutor,
                objectMapper
        );
    }

    @Test
    @DisplayName("initiateVideoJob stores raw video in MinIO, saves PENDING job, and returns 202 DTO")
    void testInitiateVideoJob() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "dashcam.mp4", "video/mp4", new byte[]{1, 2, 3, 4, 5}
        );

        when(storageService.generateRawObjectKey("dashcam.mp4")).thenReturn("raw/dashcam.mp4");
        when(storageService.getRawBucket()).thenReturn("pothole-raw");

        MediaAsset mediaAsset = new MediaAsset(MediaType.VIDEO, "raw/dashcam.mp4", "video/mp4", 5L);
        mediaAsset.setId(UUID.randomUUID());
        when(mediaAssetRepository.save(any(MediaAsset.class))).thenReturn(mediaAsset);

        DetectionJob job = new DetectionJob(mediaAsset, DetectionJobStatus.PENDING);
        job.setId(UUID.randomUUID());
        when(detectionJobRepository.save(any(DetectionJob.class))).thenReturn(job);

        // Async execution is synchronous via directExecutor, mock transition returning false to isolate initiation test
        when(detectionJobRepository.findById(job.getId())).thenReturn(Optional.of(job));

        DetectVideoAcceptedResponse response = videoDetectionService.initiateVideoJob(
                file, 28.62, 77.22, Instant.now(), "Ring Road"
        );

        assertThat(response).isNotNull();
        assertThat(response.jobId()).isEqualTo(job.getId());
        assertThat(response.status()).isEqualTo(DetectionJobStatus.PENDING);
        assertThat(response.mediaAssetId()).isEqualTo(mediaAsset.getId());
        assertThat(response.pollUrl()).isEqualTo("/api/v1/detection-jobs/" + job.getId());

        verify(storageService, times(1)).putObject(eq("pothole-raw"), eq("raw/dashcam.mp4"), any(), eq("video/mp4"));
    }

    @Test
    @DisplayName("processVideoJobAsync executes full pipeline: AI sampling -> Type A aggregation -> severity -> authority -> dedup -> COMPLETED")
    void testProcessVideoJobAsyncSuccess() {
        UUID jobId = UUID.randomUUID();
        MediaAsset media = new MediaAsset(MediaType.VIDEO, "raw/video.mp4", "video/mp4", 100L);
        DetectionJob job = new DetectionJob(media, DetectionJobStatus.PENDING);
        job.setId(jobId);

        when(detectionJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        // 1. Mock AI video inference
        AiModelMetadata modelMeta = new AiModelMetadata("vinothvikas1987/pothole-detection-yolov8", "YOLOv8s-RDD");
        AiVideoMetadata videoMeta = new AiVideoMetadata(10.0, 1.0, 2.0, 2);
        AiDetection det1 = new AiDetection(new AiBoundingBox(100, 100, 300, 300), 0.88, 3, "pothole", 0.04);
        AiFrameDetection frame1 = new AiFrameDetection(0, 0.0, 1920, 1080, List.of(det1));
        AiVideoDetectionResponse aiResponse = new AiVideoDetectionResponse(
                modelMeta, videoMeta, List.of(frame1), new AiRepresentativeFrame(0, 0.0, "MAX_VISUAL_AREA")
        );

        when(aiDetectionService.detectVideo(any(), anyString(), anyDouble(), anyDouble()))
                .thenReturn(aiResponse);

        // 2. Mock Type A aggregation
        TrackedDetection td = new TrackedDetection(0, 0.0, 1920, 1080, 100, 100, 300, 300, 0.88, 0.04);
        PotholeTrack track = new PotholeTrack(td);
        when(aggregationService.aggregateDetectionsIntoTracks(any())).thenReturn(List.of(track));

        // 3. Mock keyframe annotation and MinIO storage
        when(aiDetectionService.annotateVideoFrame(any(), anyString(), anyInt(), anyDouble()))
                .thenReturn(new byte[]{1, 2, 3});
        when(storageService.generateAnnotatedObjectKey()).thenReturn("annotated/frame_0.jpg");
        when(storageService.getAnnotatedBucket()).thenReturn("pothole-annotated");

        // 4. Mock Severity
        when(severityService.calculateSeverity(anyDouble(), anyDouble()))
                .thenReturn(new SeverityResult(45.0, SeverityClass.MEDIUM));

        // 5. Mock Authority
        CivicAuthority pwd = new CivicAuthority("Delhi PWD", "DEMO_PWD_ARTERIAL", "pwd@delhi.gov.in", "011-1234", "PWD");
        pwd.setId(UUID.randomUUID());
        when(authorityResolverService.resolveAuthority(anyDouble(), anyDouble()))
                .thenReturn(AuthorityResolutionResult.resolved(pwd));

        // 6. Mock Deduplication (Type B non-duplicate)
        when(deduplicationService.evaluateDuplicate(anyDouble(), anyDouble(), any()))
                .thenReturn(DeduplicationResult.nonDuplicate());

        Pothole savedPothole = new Pothole();
        savedPothole.setId(UUID.randomUUID());
        savedPothole.setIsDuplicate(false);
        savedPothole.setCivicAuthority(pwd);
        when(potholeRepository.save(any(Pothole.class))).thenReturn(savedPothole);

        // Execute async processing
        videoDetectionService.processVideoJobAsync(
                jobId, new byte[]{1, 2, 3}, "video.mp4", "raw/video.mp4", 28.62, 77.22, Instant.now(), "Ring Road"
        );

        // Verify status transitions
        assertThat(job.getStatus()).isEqualTo(DetectionJobStatus.COMPLETED);
        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(job.getResultSummary()).contains("\"potholesCreated\":1");
        assertThat(job.getResultSummary()).contains("\"duplicatesDetected\":0");

        verify(detectionRepository, times(1)).save(any(Detection.class));
        verify(statusHistoryRepository, times(1)).save(any(PotholeStatusHistory.class));
        verify(authorityReportingService, times(1)).dispatchReportForPothole(any(Pothole.class));
    }
}
