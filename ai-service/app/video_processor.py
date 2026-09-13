import logging
import os
import tempfile
from typing import List, Optional, Tuple
import cv2
import numpy as np
import onnxruntime as ort

from app.image_processor import (
    MODEL_NAME,
    MODEL_VERSION,
    POTHOLE_CLASS_ID,
    annotate_image_with_detections,
    postprocess_yolov8_output,
    preprocess_for_onnx,
)
from app.schemas import (
    DetectionItem,
    FrameDetection,
    ModelInfo,
    RepresentativeFrameMetadata,
    VideoDetectionResponse,
    VideoMetadata,
)

logger = logging.getLogger("pothole_ai_service.video_processor")


def run_video_inference(
    video_bytes: bytes,
    session: ort.InferenceSession,
    input_name: str = "images",
    output_names: Optional[List[str]] = None,
    sample_fps: float = 2.0,
    conf_threshold: float = 0.35,
    iou_threshold: float = 0.45,
) -> VideoDetectionResponse:
    """Process uploaded video bytes, sample frames at configured sample_fps, and return structured detections."""
    if not video_bytes or len(video_bytes) == 0:
        raise ValueError("Received empty video payload.")

    if output_names is None:
        output_names = [out.name for out in session.get_outputs()]

    # Write video to temporary file so OpenCV can seek and decode
    with tempfile.NamedTemporaryFile(suffix=".mp4", delete=False) as temp_file:
        temp_file.write(video_bytes)
        temp_video_path = temp_file.name

    cap = None
    try:
        cap = cv2.VideoCapture(temp_video_path)
        if not cap.isOpened():
            raise ValueError("Unable to open video file. Ensure format is valid MP4 or WebM.")

        original_fps = cap.get(cv2.CAP_PROP_FPS)
        if original_fps <= 0.0 or np.isnan(original_fps):
            original_fps = 30.0

        total_frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        orig_width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        orig_height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))

        duration_sec = total_frame_count / original_fps if total_frame_count > 0 else 0.0

        if sample_fps <= 0.0:
            sample_fps = 2.0

        step = max(1, int(round(original_fps / sample_fps)))
        logger.info(
            f"Processing video: {orig_width}x{orig_height}, {total_frame_count} frames, "
            f"{original_fps:.2f} FPS, duration {duration_sec:.2f}s, sampling every {step} frames ({sample_fps} FPS)"
        )

        frames: List[FrameDetection] = []
        best_score = -1.0
        best_frame_idx: Optional[int] = None
        best_timestamp: Optional[float] = None

        frame_idx = 0
        sampled_count = 0

        while True:
            ret, frame = cap.read()
            if not ret or frame is None:
                break

            if frame_idx % step == 0:
                sampled_count += 1
                timestamp_sec = round(frame_idx / original_fps, 3)

                # Preprocess frame
                tensor, w, h, scale, (pad_x, pad_y) = preprocess_for_onnx(
                    frame, target_shape=(640, 640)
                )

                # ONNX Inference
                outputs = session.run(output_names, {input_name: tensor})

                # Post-process & NMS
                detections = postprocess_yolov8_output(
                    outputs[0],
                    orig_w=w,
                    orig_h=h,
                    scale=scale,
                    pad_x=pad_x,
                    pad_y=pad_y,
                    conf_threshold=conf_threshold,
                    iou_threshold=iou_threshold,
                    target_class_id=POTHOLE_CLASS_ID,
                )

                frame_detection = FrameDetection(
                    frame_index=frame_idx,
                    timestamp_sec=timestamp_sec,
                    image_width=w,
                    image_height=h,
                    detections=detections,
                )
                frames.append(frame_detection)

                # Track candidate with strongest visual evidence
                if detections:
                    for det in detections:
                        score = det.visual_area_ratio * det.confidence
                        if score > best_score:
                            best_score = score
                            best_frame_idx = frame_idx
                            best_timestamp = timestamp_sec

            frame_idx += 1

        rep_frame = None
        if best_frame_idx is not None and best_timestamp is not None:
            rep_frame = RepresentativeFrameMetadata(
                frame_index=best_frame_idx,
                timestamp_sec=best_timestamp,
                reason="MAX_VISUAL_AREA",
            )

        video_meta = VideoMetadata(
            fps=float(round(original_fps, 2)),
            duration_seconds=float(round(duration_sec, 2)),
            sample_fps=float(sample_fps),
            total_frames_sampled=sampled_count,
        )

        return VideoDetectionResponse(
            model=ModelInfo(name=MODEL_NAME, version=MODEL_VERSION),
            video=video_meta,
            frames=frames,
            representative_frame=rep_frame,
        )

    finally:
        if cap is not None:
            cap.release()
        if os.path.exists(temp_video_path):
            try:
                os.remove(temp_video_path)
            except Exception as e:
                logger.warning(f"Failed to remove temp video file {temp_video_path}: {e}")


def extract_annotated_frame(
    video_bytes: bytes,
    frame_index: Optional[int],
    session: ort.InferenceSession,
    input_name: str = "images",
    output_names: Optional[List[str]] = None,
    conf_threshold: float = 0.35,
    iou_threshold: float = 0.45,
) -> Tuple[bytes, int, float]:
    """Extract a specific frame from the video, run inference, draw bounding boxes, and return JPEG bytes."""
    if not video_bytes or len(video_bytes) == 0:
        raise ValueError("Received empty video payload.")

    if output_names is None:
        output_names = [out.name for out in session.get_outputs()]

    with tempfile.NamedTemporaryFile(suffix=".mp4", delete=False) as temp_file:
        temp_file.write(video_bytes)
        temp_video_path = temp_file.name

    cap = None
    try:
        cap = cv2.VideoCapture(temp_video_path)
        if not cap.isOpened():
            raise ValueError("Unable to open video file.")

        original_fps = cap.get(cv2.CAP_PROP_FPS)
        if original_fps <= 0.0 or np.isnan(original_fps):
            original_fps = 30.0

        target_idx = frame_index if frame_index is not None and frame_index >= 0 else 0

        cap.set(cv2.CAP_PROP_POS_FRAMES, target_idx)
        ret, frame = cap.read()
        if not ret or frame is None:
            # Fallback to frame 0 if target index out of range
            cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
            ret, frame = cap.read()
            target_idx = 0
            if not ret or frame is None:
                raise ValueError("Could not read any valid frame from video.")

        timestamp_sec = round(target_idx / original_fps, 3)

        # Preprocess & inference
        tensor, w, h, scale, (pad_x, pad_y) = preprocess_for_onnx(
            frame, target_shape=(640, 640)
        )
        outputs = session.run(output_names, {input_name: tensor})
        detections = postprocess_yolov8_output(
            outputs[0],
            orig_w=w,
            orig_h=h,
            scale=scale,
            pad_x=pad_x,
            pad_y=pad_y,
            conf_threshold=conf_threshold,
            iou_threshold=iou_threshold,
            target_class_id=POTHOLE_CLASS_ID,
        )

        # Annotate
        annotated_bgr = annotate_image_with_detections(frame, detections)
        success, encoded_jpg = cv2.imencode(
            ".jpg", annotated_bgr, [cv2.IMWRITE_JPEG_QUALITY, 92]
        )
        if not success:
            raise RuntimeError("Failed to encode annotated frame to JPEG.")

        return encoded_jpg.tobytes(), target_idx, timestamp_sec

    finally:
        if cap is not None:
            cap.release()
        if os.path.exists(temp_video_path):
            try:
                os.remove(temp_video_path)
            except Exception as e:
                logger.warning(f"Failed to remove temp video file {temp_video_path}: {e}")
