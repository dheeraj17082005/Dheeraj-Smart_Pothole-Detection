from typing import Dict, List, Optional
from pydantic import BaseModel, Field


class ModelInfo(BaseModel):
    """Identification metadata for the deployed computer vision model."""

    name: str = Field(
        default="peterhdd/pothole-detection-yolov8",
        description="Source model identifier",
        examples=["peterhdd/pothole-detection-yolov8"],
    )
    version: str = Field(
        default="YOLOv8s",
        description="Model checkpoint/training version",
        examples=["YOLOv8s"],
    )


class ImageDimension(BaseModel):
    """Dimensions of the analyzed image in pixels."""

    width: int = Field(..., ge=1, description="Original image width in pixels", examples=[1920])
    height: int = Field(..., ge=1, description="Original image height in pixels", examples=[1080])


class BoundingBox(BaseModel):
    """Bounding box coordinates scaled to original image pixel coordinates."""

    xmin: int = Field(..., ge=0, description="Left coordinate in pixels", examples=[120])
    ymin: int = Field(..., ge=0, description="Top coordinate in pixels", examples=[85])
    xmax: int = Field(..., ge=0, description="Right coordinate in pixels", examples=[340])
    ymax: int = Field(..., ge=0, description="Bottom coordinate in pixels", examples=[290])


class DetectionItem(BaseModel):
    """Individual detected pothole event with bounding box and metrics."""

    box: BoundingBox = Field(..., description="Bounding box in original image pixel space")
    confidence: float = Field(
        ...,
        ge=0.0,
        le=1.0,
        description="Detection confidence probability score (0.0 to 1.0)",
        examples=[0.885],
    )
    class_id: int = Field(
        default=0,
        description="Target class ID (Class 0 = Pothole)",
        examples=[0],
    )
    class_name: str = Field(
        default="pothole",
        description="Class label",
        examples=["pothole"],
    )
    visual_area_ratio: float = Field(
        ...,
        ge=0.0,
        le=1.0,
        description="Ratio of bounding box area to total original image area (0.0 to 1.0)",
        examples=[0.042],
    )


class ImageDetectionResponse(BaseModel):
    """Frozen contract response schema for image pothole detection."""

    model: ModelInfo = Field(..., description="Model identification details")
    image: ImageDimension = Field(..., description="Original image dimensions")
    pothole_count: int = Field(
        ...,
        ge=0,
        description="Total count of confirmed potholes passing NMS filtering",
        examples=[1],
    )
    max_confidence: float = Field(
        default=0.0,
        ge=0.0,
        le=1.0,
        description="Maximum confidence score among detected potholes",
        examples=[0.885],
    )
    max_area_ratio: float = Field(
        default=0.0,
        ge=0.0,
        le=1.0,
        description="Maximum visual area ratio among detected potholes",
        examples=[0.042],
    )
    detections: List[DetectionItem] = Field(
        default_factory=list,
        description="List of detected pothole instances sorted by confidence descending",
    )


class ErrorDetail(BaseModel):
    """Structured error payload details."""

    code: str = Field(
        ...,
        description="Machine-readable error code",
        examples=["INVALID_IMAGE", "MODEL_NOT_READY", "INFERENCE_FAILED"],
    )
    message: str = Field(
        ...,
        description="Human-readable explanation of the error",
        examples=["Unable to decode uploaded image bytes"],
    )


class ErrorResponse(BaseModel):
    """Standardized error envelope."""

    error: ErrorDetail = Field(..., description="Structured error details")


class ModelMetadata(BaseModel):
    """Detailed model architecture specifications."""

    model_name: str = Field(
        default="peterhdd/pothole-detection-yolov8",
        description="Source model identifier",
    )
    model_version: str = Field(
        default="YOLOv8s", description="Model training version"
    )
    model_format: str = Field(
        default="ONNX", description="Model runtime format"
    )
    input_tensor_name: str = Field(
        default="images", description="Input tensor name"
    )
    input_shape: List[int] = Field(
        default=[1, 3, 640, 640], description="Input shape [B, C, H, W]"
    )
    output_tensor_name: str = Field(
        default="output0", description="Output tensor name"
    )
    output_shape: List[int] = Field(
        default=[1, 5, 8400], description="Output shape [B, attributes, anchors]"
    )
    classes: Dict[int, str] = Field(
        default={
            0: "pothole",
        },
        description="Target class dictionary",
    )
    pothole_class_id: int = Field(
        default=0, description="Target class index for potholes"
    )


class HealthResponse(BaseModel):
    """Health check and model readiness payload."""

    status: str = Field(default="healthy", description="Service health status")
    model_loaded: bool = Field(
        ..., description="Whether the ONNX model artifact is loaded and ready"
    )
    model_path: Optional[str] = Field(
        None, description="Configured path to the ONNX model"
    )
    version: Optional[str] = Field(None, description="AI service version")
    model_metadata: Optional[ModelMetadata] = Field(
        None, description="Loaded model metadata representation"
    )


class VideoMetadata(BaseModel):
    """Metadata regarding the ingested and sampled video."""

    fps: float = Field(..., description="Original video frame rate (FPS)")
    duration_seconds: float = Field(..., description="Total duration of the video in seconds")
    sample_fps: float = Field(default=2.0, description="Rate at which frames were sampled for inference")
    total_frames_sampled: int = Field(..., description="Total number of frames extracted and analyzed")


class FrameDetection(BaseModel):
    """Structured detections identified in a single sampled video frame."""

    frame_index: int = Field(..., ge=0, description="Exact zero-based frame index from original video")
    timestamp_sec: float = Field(..., ge=0.0, description="Timestamp offset in seconds within the video")
    image_width: int = Field(..., ge=1, description="Frame width in pixels")
    image_height: int = Field(..., ge=1, description="Frame height in pixels")
    detections: List[DetectionItem] = Field(
        default_factory=list,
        description="Potholes identified in this specific frame",
    )


class RepresentativeFrameMetadata(BaseModel):
    """Metadata pointing to the keyframe containing strongest visual evidence."""

    frame_index: int = Field(..., description="Frame index of representative frame")
    timestamp_sec: float = Field(..., description="Timestamp in seconds of representative frame")
    reason: str = Field(default="MAX_VISUAL_AREA", description="Heuristic selection criterion")


class VideoDetectionResponse(BaseModel):
    """Structured response schema for video pothole detection and frame-level tracking."""

    model: ModelInfo = Field(..., description="Model identification details")
    video: VideoMetadata = Field(..., description="Video stream and sampling statistics")
    frames: List[FrameDetection] = Field(
        default_factory=list,
        description="Frame-by-frame detections for all sampled frames",
    )
    representative_frame: Optional[RepresentativeFrameMetadata] = Field(
        None,
        description="Identified representative keyframe containing maximal visual area / confidence",
    )

