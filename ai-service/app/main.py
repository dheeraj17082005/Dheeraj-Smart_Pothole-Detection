import logging
from contextlib import asynccontextmanager
from typing import Optional
from fastapi import FastAPI, File, HTTPException, Query, Request, Response, UploadFile, status
from fastapi.responses import JSONResponse

from app.config import get_settings
from app.image_processor import run_image_inference
from app.video_processor import extract_annotated_frame, run_video_inference
from app.model import pothole_model
from app.schemas import (
    ErrorDetail,
    ErrorResponse,
    HealthResponse,
    ImageDetectionResponse,
    VideoDetectionResponse,
)

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("pothole_ai_service")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan context manager: loads model once at startup."""
    settings = get_settings()
    logger.info(f"Starting {settings.app_name} v{settings.app_version}...")
    logger.info(f"Configured model path: {settings.model_path}")

    # Load ONNX model exactly once during startup
    loaded = pothole_model.load_model(settings.model_path)
    if loaded:
        logger.info(
            f"ONNX Pothole model loaded and ready. Input shape: {pothole_model.input_shape}, "
            f"Classes: {pothole_model.classes}"
        )
    else:
        logger.warning(
            "Service started WITHOUT loaded model. Detection endpoints will require model artifact."
        )

    yield

    logger.info(f"Shutting down {settings.app_name}...")


settings = get_settings()
app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description=(
        "Stateless Computer Vision Microservice for Pothole Detection. "
        "Provides high-performance inference using YOLOv8 ONNX runtime."
    ),
    lifespan=lifespan,
)

# Internal backend microservice: direct external browser CORS access disabled



def create_error_response(status_code: int, error_code: str, message: str) -> JSONResponse:
    """Create a standardized structured JSON error response."""
    payload = ErrorResponse(error=ErrorDetail(code=error_code, message=message))
    return JSONResponse(status_code=status_code, content=payload.model_dump())


@app.get(
    "/health",
    response_model=HealthResponse,
    tags=["Health"],
    summary="Health check and model readiness",
    description="Returns the operational status of the service, model loading state, and model architecture metadata.",
)
def health_check() -> HealthResponse:
    """Return health status, model readiness state, and model metadata."""
    current_settings = get_settings()
    return HealthResponse(
        status="healthy",
        model_loaded=pothole_model.is_loaded,
        model_path=pothole_model.model_path or current_settings.model_path,
        version=current_settings.app_version,
        model_metadata=pothole_model.metadata,
    )


@app.post(
    "/detect/image",
    response_model=ImageDetectionResponse,
    tags=["Detection"],
    summary="Detect potholes in an image (Structured JSON)",
    description=(
        "Accepts an image file (multipart/form-data), runs YOLOv8 ONNX inference, "
        "applies Non-Maximum Suppression (NMS), filters for Pothole class (Class 3), "
        "and returns structured JSON detection metadata with exact bounding box coordinates and area ratios."
    ),
    responses={
        200: {
            "model": ImageDetectionResponse,
            "description": "Structured pothole detections and image metadata.",
        },
        400: {
            "model": ErrorResponse,
            "description": "Invalid, missing, or corrupted image payload (INVALID_IMAGE).",
        },
        500: {
            "model": ErrorResponse,
            "description": "Internal model inference failure (INFERENCE_FAILED).",
        },
        503: {
            "model": ErrorResponse,
            "description": "Model is not loaded or ready (MODEL_NOT_READY).",
        },
    },
)
async def detect_image(
    file: UploadFile = File(..., description="Image file to analyze (JPEG/PNG/WebP, max 25MB)"),
    confidence_threshold: float = Query(
        default=None,
        ge=0.01,
        le=1.0,
        description="Optional confidence threshold override (default: configured value, 0.25)",
    ),
):
    """Process uploaded image and return structured detection results."""
    if not pothole_model.is_loaded:
        return create_error_response(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            error_code="MODEL_NOT_READY",
            message="ONNX model artifact is not loaded. Verify the model file exists at MODEL_PATH.",
        )

    if not file or not file.filename:
        return create_error_response(
            status_code=status.HTTP_400_BAD_REQUEST,
            error_code="INVALID_IMAGE",
            message="No image file provided in request.",
        )

    # Read binary stream
    try:
        image_bytes = await file.read()
        if len(image_bytes) == 0:
            return create_error_response(
                status_code=status.HTTP_400_BAD_REQUEST,
                error_code="INVALID_IMAGE",
                message="Uploaded image file is empty (0 bytes).",
            )
    except Exception as e:
        return create_error_response(
            status_code=status.HTTP_400_BAD_REQUEST,
            error_code="INVALID_IMAGE",
            message=f"Failed to read image stream: {e}",
        )

    # Determine confidence threshold
    current_settings = get_settings()
    conf_thresh = (
        confidence_threshold
        if confidence_threshold is not None
        else current_settings.confidence_threshold
    )

    # Execute inference
    try:
        detection_response, _ = run_image_inference(
            image_bytes=image_bytes,
            session=pothole_model.get_session(),
            input_name=pothole_model.input_name or "images",
            output_names=pothole_model.output_names or ["output0"],
            conf_threshold=conf_thresh,
            iou_threshold=current_settings.nms_iou_threshold,
            enable_tiling=current_settings.tiling_enabled,
            tile_size=current_settings.tile_size,
            tile_overlap=current_settings.tile_overlap,
            tile_min_dim=current_settings.tile_min_dimension,
        )
        return detection_response
    except ValueError as val_err:
        return create_error_response(
            status_code=status.HTTP_400_BAD_REQUEST,
            error_code="INVALID_IMAGE",
            message=str(val_err),
        )
    except Exception as err:
        logger.error(f"Image inference error: {err}", exc_info=True)
        return create_error_response(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            error_code="INFERENCE_FAILED",
            message=f"Inference execution failed: {err}",
        )


@app.post(
    "/detect/image/annotate",
    tags=["Detection"],
    summary="Detect potholes and return annotated binary JPEG",
    description=(
        "Accepts an image file, executes ONNX inference, draws bounding boxes and confidence labels, "
        "and returns the annotated binary JPEG image with full detection JSON in the 'X-Detection-Metadata' header."
    ),
    responses={
        200: {
            "content": {"image/jpeg": {}},
            "description": "Annotated image binary with bounding box overlays.",
        },
        400: {
            "model": ErrorResponse,
            "description": "Invalid image payload.",
        },
        503: {
            "model": ErrorResponse,
            "description": "Model not ready.",
        },
    },
)
async def detect_and_annotate_image(
    file: UploadFile = File(..., description="Image file to analyze (JPEG/PNG/WebP)"),
    confidence_threshold: float = Query(
        default=None,
        ge=0.01,
        le=1.0,
        description="Optional confidence threshold override",
    ),
):
    """Process uploaded image and return annotated JPEG binary stream."""
    if not pothole_model.is_loaded:
        return create_error_response(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            error_code="MODEL_NOT_READY",
            message="ONNX model artifact is not loaded.",
        )

    try:
        image_bytes = await file.read()
        if len(image_bytes) == 0:
            return create_error_response(
                status_code=status.HTTP_400_BAD_REQUEST,
                error_code="INVALID_IMAGE",
                message="Uploaded image file is empty.",
            )
    except Exception as e:
        return create_error_response(
            status_code=status.HTTP_400_BAD_REQUEST,
            error_code="INVALID_IMAGE",
            message=f"Failed to read image stream: {e}",
        )

    current_settings = get_settings()
    conf_thresh = (
        confidence_threshold
        if confidence_threshold is not None
        else current_settings.confidence_threshold
    )

    try:
        detection_response, annotated_jpeg_bytes = run_image_inference(
            image_bytes=image_bytes,
            session=pothole_model.get_session(),
            input_name=pothole_model.input_name or "images",
            output_names=pothole_model.output_names or ["output0"],
            conf_threshold=conf_thresh,
            iou_threshold=current_settings.nms_iou_threshold,
            enable_tiling=current_settings.tiling_enabled,
            tile_size=current_settings.tile_size,
            tile_overlap=current_settings.tile_overlap,
            tile_min_dim=current_settings.tile_min_dimension,
        )

        return Response(
            content=annotated_jpeg_bytes,
            media_type="image/jpeg",
            headers={
                "X-Pothole-Count": str(detection_response.pothole_count),
                "X-Max-Confidence": str(detection_response.max_confidence),
                "X-Max-Area-Ratio": str(detection_response.max_area_ratio),
                "X-Detection-Metadata": detection_response.model_dump_json(),
            },
        )
    except ValueError as val_err:
        return create_error_response(
            status_code=status.HTTP_400_BAD_REQUEST,
            error_code="INVALID_IMAGE",
            message=str(val_err),
        )
    except Exception as err:
        logger.error(f"Image annotation error: {err}", exc_info=True)
        return create_error_response(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            error_code="INFERENCE_FAILED",
            message=f"Image annotation execution failed: {err}",
        )


@app.post(
    "/detect/video",
    response_model=VideoDetectionResponse,
    tags=["Detection"],
    summary="Sample frames and detect potholes across a video stream (Structured JSON)",
    description=(
        "Accepts a video file (MP4/WebM), samples frames at configurable FPS (default 2.0 FPS), "
        "runs YOLOv8 ONNX inference on sampled frames, and returns structured frame-level detections "
        "with frame indices, timestamps, and keyframe metadata."
    ),
    responses={
        200: {
            "model": VideoDetectionResponse,
            "description": "Structured frame detections and video metadata.",
        },
        400: {
            "model": ErrorResponse,
            "description": "Invalid, empty, or unreadable video payload.",
        },
        503: {
            "model": ErrorResponse,
            "description": "Model not ready.",
        },
    },
)
async def detect_video(
    file: UploadFile = File(..., description="Video file to analyze (MP4/WebM)"),
    sample_fps: float = Query(
        default=2.0,
        ge=0.1,
        le=30.0,
        description="Frame sampling rate in frames per second (default: 2.0)",
    ),
    confidence_threshold: float = Query(
        default=None,
        ge=0.01,
        le=1.0,
        description="Optional confidence threshold override",
    ),
):
    """Process uploaded video and return structured frame-level detection results."""
    if not pothole_model.is_loaded:
        return create_error_response(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            error_code="MODEL_NOT_READY",
            message="ONNX model artifact is not loaded. Verify the model file exists at MODEL_PATH.",
        )

    if not file or not file.filename:
        return create_error_response(
            status_code=status.HTTP_400_BAD_REQUEST,
            error_code="INVALID_VIDEO",
            message="No video file provided in request.",
        )

    try:
        video_bytes = await file.read()
        if len(video_bytes) == 0:
            return create_error_response(
                status_code=status.HTTP_400_BAD_REQUEST,
                error_code="INVALID_VIDEO",
                message="Uploaded video file is empty (0 bytes).",
            )
    except Exception as e:
        return create_error_response(
            status_code=status.HTTP_400_BAD_REQUEST,
            error_code="INVALID_VIDEO",
            message=f"Failed to read video stream: {e}",
        )

    current_settings = get_settings()
    conf_thresh = (
        confidence_threshold
        if confidence_threshold is not None
        else current_settings.confidence_threshold
    )

    try:
        response_data = run_video_inference(
            video_bytes=video_bytes,
            session=pothole_model.get_session(),
            input_name=pothole_model.input_name or "images",
            output_names=pothole_model.output_names or ["output0"],
            sample_fps=sample_fps,
            conf_threshold=conf_thresh,
            iou_threshold=current_settings.nms_iou_threshold,
        )
        return response_data
    except ValueError as val_err:
        return create_error_response(
            status_code=status.HTTP_400_BAD_REQUEST,
            error_code="INVALID_VIDEO",
            message=str(val_err),
        )
    except Exception as err:
        logger.error(f"Video inference error: {err}", exc_info=True)
        return create_error_response(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            error_code="INFERENCE_FAILED",
            message=f"Video inference execution failed: {err}",
        )


@app.post(
    "/detect/video/annotate-frame",
    tags=["Detection"],
    summary="Extract and annotate representative video keyframe as binary JPEG",
    description=(
        "Extracts a specific frame index from the uploaded video file, performs ONNX inference, "
        "draws bounding boxes, and returns the annotated binary JPEG."
    ),
    responses={
        200: {
            "content": {"image/jpeg": {}},
            "description": "Annotated keyframe binary JPEG image.",
        },
        400: {
            "model": ErrorResponse,
            "description": "Invalid video payload or invalid frame index.",
        },
        503: {
            "model": ErrorResponse,
            "description": "Model not ready.",
        },
    },
)
async def annotate_video_frame(
    file: UploadFile = File(..., description="Video file (MP4/WebM)"),
    frame_index: Optional[int] = Query(
        default=None,
        ge=0,
        description="Frame index to extract and annotate",
    ),
    confidence_threshold: float = Query(
        default=None,
        ge=0.01,
        le=1.0,
        description="Optional confidence threshold override",
    ),
):
    """Extract and annotate a specific frame from uploaded video and return binary JPEG."""
    if not pothole_model.is_loaded:
        return create_error_response(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            error_code="MODEL_NOT_READY",
            message="ONNX model artifact is not loaded.",
        )

    try:
        video_bytes = await file.read()
        if len(video_bytes) == 0:
            return create_error_response(
                status_code=status.HTTP_400_BAD_REQUEST,
                error_code="INVALID_VIDEO",
                message="Uploaded video file is empty.",
            )
    except Exception as e:
        return create_error_response(
            status_code=status.HTTP_400_BAD_REQUEST,
            error_code="INVALID_VIDEO",
            message=f"Failed to read video stream: {e}",
        )

    current_settings = get_settings()
    conf_thresh = (
        confidence_threshold
        if confidence_threshold is not None
        else current_settings.confidence_threshold
    )

    try:
        annotated_jpg, actual_idx, timestamp_sec = extract_annotated_frame(
            video_bytes=video_bytes,
            frame_index=frame_index,
            session=pothole_model.get_session(),
            input_name=pothole_model.input_name or "images",
            output_names=pothole_model.output_names or ["output0"],
            conf_threshold=conf_thresh,
            iou_threshold=current_settings.nms_iou_threshold,
        )

        return Response(
            content=annotated_jpg,
            media_type="image/jpeg",
            headers={
                "X-Frame-Index": str(actual_idx),
                "X-Timestamp-Sec": str(timestamp_sec),
            },
        )
    except ValueError as val_err:
        return create_error_response(
            status_code=status.HTTP_400_BAD_REQUEST,
            error_code="INVALID_VIDEO",
            message=str(val_err),
        )
    except Exception as err:
        logger.error(f"Video frame annotation error: {err}", exc_info=True)
        return create_error_response(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            error_code="INFERENCE_FAILED",
            message=f"Video frame annotation execution failed: {err}",
        )

