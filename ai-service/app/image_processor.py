import logging
from typing import List, Tuple
import cv2
import numpy as np
import onnxruntime as ort

from app.schemas import (
    BoundingBox,
    DetectionItem,
    ImageDetectionResponse,
    ImageDimension,
    ModelInfo,
)

logger = logging.getLogger("pothole_ai_service.image_processor")

POTHOLE_CLASS_ID = 0
POTHOLE_CLASS_NAME = "pothole"
MODEL_NAME = "peterhdd/pothole-detection-yolov8"
MODEL_VERSION = "YOLOv8s"


def decode_image_bytes(image_bytes: bytes) -> np.ndarray:
    """Decode raw image bytes into an OpenCV BGR numpy array.

    Raises ValueError if image bytes are empty or corrupted.
    """
    if not image_bytes or len(image_bytes) == 0:
        raise ValueError("Received empty image payload.")

    nparr = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    if img is None or img.size == 0 or img.shape[0] == 0 or img.shape[1] == 0:
        raise ValueError(
            "Unable to decode uploaded image. Supported formats include JPEG, PNG, and WebP."
        )
    return img


def letterbox_image(
    image: np.ndarray, target_shape: Tuple[int, int] = (640, 640)
) -> Tuple[np.ndarray, float, Tuple[float, float]]:
    """Resize and pad image while maintaining aspect ratio (YOLO letterboxing).

    Returns:
        padded_image: 640x640 BGR image with border padding
        scale: Scaling ratio used
        (pad_x, pad_y): Left/Top padding offsets in pixels
    """
    orig_h, orig_w = image.shape[:2]
    target_h, target_w = target_shape

    scale = min(target_w / orig_w, target_h / orig_h)
    new_w = int(round(orig_w * scale))
    new_h = int(round(orig_h * scale))

    resized = cv2.resize(
        image, (new_w, new_h), interpolation=cv2.INTER_LINEAR
    )

    dw = target_w - new_w
    dh = target_h - new_h

    pad_x = dw / 2.0
    pad_y = dh / 2.0

    top, bottom = int(round(pad_y - 0.1)), int(round(pad_y + 0.1))
    left, right = int(round(pad_x - 0.1)), int(round(pad_x + 0.1))

    padded = cv2.copyMakeBorder(
        resized,
        top,
        bottom,
        left,
        right,
        cv2.BORDER_CONSTANT,
        value=(114, 114, 114),
    )
    return padded, scale, (pad_x, pad_y)


def preprocess_for_onnx(
    image: np.ndarray, target_shape: Tuple[int, int] = (640, 640)
) -> Tuple[np.ndarray, int, int, float, Tuple[float, float]]:
    """Preprocess OpenCV BGR image for YOLOv8 ONNX input.

    Input shape: [1, 3, 640, 640], Float32, RGB, normalized [0.0, 1.0].
    """
    orig_h, orig_w = image.shape[:2]
    padded, scale, (pad_x, pad_y) = letterbox_image(image, target_shape)

    # Convert BGR to RGB
    rgb = cv2.cvtColor(padded, cv2.COLOR_BGR2RGB)

    # Normalize to [0.0, 1.0] and transpose to NCHW
    normalized = rgb.astype(np.float32) / 255.0
    tensor = np.transpose(normalized, (2, 0, 1))  # [3, 640, 640]
    tensor = np.expand_dims(tensor, axis=0)  # [1, 3, 640, 640]

    return tensor, orig_w, orig_h, scale, (pad_x, pad_y)


def slice_image_for_tiling(
    image: np.ndarray, tile_size: int = 640, overlap_ratio: float = 0.25
) -> List[Tuple[np.ndarray, int, int]]:
    """Slice an image into overlapping grid tiles for high-resolution inference.

    Returns:
        List of (crop_bgr, offset_x, offset_y)
    """
    orig_h, orig_w = image.shape[:2]
    if orig_w <= tile_size and orig_h <= tile_size:
        return []

    step_x = max(1, int(tile_size * (1.0 - overlap_ratio)))
    step_y = max(1, int(tile_size * (1.0 - overlap_ratio)))

    x_starts = list(range(0, max(1, orig_w - tile_size + 1), step_x))
    if len(x_starts) == 0 or x_starts[-1] + tile_size < orig_w:
        x_starts.append(max(0, orig_w - tile_size))

    y_starts = list(range(0, max(1, orig_h - tile_size + 1), step_y))
    if len(y_starts) == 0 or y_starts[-1] + tile_size < orig_h:
        y_starts.append(max(0, orig_h - tile_size))

    x_starts = sorted(list(set(x_starts)))
    y_starts = sorted(list(set(y_starts)))

    tiles: List[Tuple[np.ndarray, int, int]] = []
    for ys in y_starts:
        for xs in x_starts:
            ye = min(orig_h, ys + tile_size)
            xe = min(orig_w, xs + tile_size)
            crop = image[ys:ye, xs:xe]
            tiles.append((crop, xs, ys))

    return tiles


def infer_crop_candidates(
    session: ort.InferenceSession,
    crop: np.ndarray,
    input_name: str = "images",
    output_names: List[str] = None,
    conf_threshold: float = 0.15,
    target_class_id: int = POTHOLE_CLASS_ID,
) -> List[Tuple[float, float, float, float, float]]:
    """Run model inference on a single crop/image and return raw candidate boxes in crop coordinates.

    Returns:
        List of (x1, y1, x2, y2, score) in local crop coordinates
    """
    orig_h, orig_w = crop.shape[:2]
    tensor, _, _, scale, (pad_x, pad_y) = preprocess_for_onnx(
        crop, target_shape=(640, 640)
    )

    if output_names is None:
        output_names = [out.name for out in session.get_outputs()]

    outputs = session.run(output_names, {input_name: tensor})
    preds = outputs[0][0].T  # [8400, channels]

    if preds.shape[1] == 5:
        scores = preds[:, 4]
    elif preds.shape[1] > 5:
        class_col_idx = min(preds.shape[1] - 1, 4 + target_class_id)
        scores = preds[:, class_col_idx]
    else:
        scores = preds[:, 0]

    valid_mask = scores >= conf_threshold
    valid_preds = preds[valid_mask]
    valid_scores = scores[valid_mask]

    candidates: List[Tuple[float, float, float, float, float]] = []
    for pred, score in zip(valid_preds, valid_scores):
        cx, cy, bw, bh = pred[0], pred[1], pred[2], pred[3]
        x1 = (cx - bw / 2.0 - pad_x) / scale
        y1 = (cy - bh / 2.0 - pad_y) / scale
        x2 = (cx + bw / 2.0 - pad_x) / scale
        y2 = (cy + bh / 2.0 - pad_y) / scale

        x1 = max(0.0, min(float(orig_w), float(x1)))
        y1 = max(0.0, min(float(orig_h), float(y1)))
        x2 = max(0.0, min(float(orig_w), float(x2)))
        y2 = max(0.0, min(float(orig_h), float(y2)))

        if (x2 - x1) >= 1.0 and (y2 - y1) >= 1.0:
            candidates.append((x1, y1, x2, y2, float(score)))

    return candidates


def postprocess_yolov8_output(
    output_tensor: np.ndarray,
    orig_w: int,
    orig_h: int,
    scale: float,
    pad_x: float,
    pad_y: float,
    conf_threshold: float = 0.15,
    iou_threshold: float = 0.45,
    target_class_id: int = POTHOLE_CLASS_ID,
) -> List[DetectionItem]:
    """Post-process YOLOv8 ONNX raw output [1, 5, 8400].

    Extracts bounding boxes, scales back to original resolution, filters for
    the pothole class, and applies Non-Maximum Suppression (NMS).
    """
    preds = output_tensor[0].T  # shape (8400, num_channels)

    boxes_for_nms = []
    confidences = []
    raw_coords = []

    if preds.shape[1] == 5:
        scores = preds[:, 4]
    elif preds.shape[1] > 5:
        class_col_idx = min(preds.shape[1] - 1, 4 + target_class_id)
        scores = preds[:, class_col_idx]
    else:
        scores = preds[:, 0]

    valid_mask = scores >= conf_threshold
    valid_preds = preds[valid_mask]
    valid_scores = scores[valid_mask]

    for pred, score in zip(valid_preds, valid_scores):
        cx, cy, bw, bh = pred[0], pred[1], pred[2], pred[3]

        x1 = (cx - bw / 2.0 - pad_x) / scale
        y1 = (cy - bh / 2.0 - pad_y) / scale
        x2 = (cx + bw / 2.0 - pad_x) / scale
        y2 = (cy + bh / 2.0 - pad_y) / scale

        x1 = max(0.0, min(float(orig_w), float(x1)))
        y1 = max(0.0, min(float(orig_h), float(y1)))
        x2 = max(0.0, min(float(orig_w), float(x2)))
        y2 = max(0.0, min(float(orig_h), float(y2)))

        w_box = x2 - x1
        h_box = y2 - y1

        if w_box >= 1.0 and h_box >= 1.0:
            boxes_for_nms.append([int(round(x1)), int(round(y1)), int(round(w_box)), int(round(h_box))])
            confidences.append(float(score))
            raw_coords.append((x1, y1, x2, y2))

    if not boxes_for_nms:
        return []

    indices = cv2.dnn.NMSBoxes(
        boxes_for_nms,
        confidences,
        score_threshold=conf_threshold,
        nms_threshold=iou_threshold,
    )

    detections: List[DetectionItem] = []
    total_image_area = float(orig_w * orig_h)

    if len(indices) > 0:
        flat_indices = (
            indices.flatten() if hasattr(indices, "flatten") else indices
        )
        for i in flat_indices:
            x1, y1, x2, y2 = raw_coords[i]
            conf = float(confidences[i])
            conf_clamped = max(0.0, min(1.0, conf))

            box_area = (x2 - x1) * (y2 - y1)
            raw_ratio = box_area / total_image_area if total_image_area > 0 else 0.0
            ratio_clamped = max(0.0, min(1.0, raw_ratio))

            detections.append(
                DetectionItem(
                    box=BoundingBox(
                        xmin=int(round(x1)),
                        ymin=int(round(y1)),
                        xmax=int(round(x2)),
                        ymax=int(round(y2)),
                    ),
                    confidence=float(round(conf_clamped, 4)),
                    class_id=target_class_id,
                    class_name=POTHOLE_CLASS_NAME,
                    visual_area_ratio=float(round(ratio_clamped, 6)),
                )
            )

    detections.sort(key=lambda d: d.confidence, reverse=True)
    return detections


def annotate_image_with_detections(
    image: np.ndarray, detections: List[DetectionItem]
) -> np.ndarray:
    """Draw bounding boxes and confidence labels on image using OpenCV."""
    annotated = image.copy()

    for det in detections:
        x1, y1, x2, y2 = det.box.xmin, det.box.ymin, det.box.xmax, det.box.ymax

        cv2.rectangle(
            annotated, (x1, y1), (x2, y2), color=(0, 0, 235), thickness=3
        )

        label = f"Pothole: {det.confidence:.2f} ({det.visual_area_ratio * 100:.1f}%)"
        font = cv2.FONT_HERSHEY_SIMPLEX
        font_scale = 0.55
        thickness = 2
        text_size, _ = cv2.getTextSize(label, font, font_scale, thickness)

        text_w, text_h = text_size
        box_y1 = max(0, y1 - text_h - 8)
        box_y2 = y1
        cv2.rectangle(
            annotated,
            (x1, box_y1),
            (x1 + text_w + 8, box_y2),
            (0, 0, 235),
            -1,
        )

        cv2.putText(
            annotated,
            label,
            (x1 + 4, y1 - 4),
            font,
            font_scale,
            (255, 255, 255),
            thickness,
            cv2.LINE_AA,
        )

    return annotated


def run_image_inference(
    image_bytes: bytes,
    session: ort.InferenceSession,
    input_name: str = "images",
    output_names: List[str] = None,
    conf_threshold: float = 0.15,
    iou_threshold: float = 0.45,
    enable_tiling: bool = True,
    tile_size: int = 640,
    tile_overlap: float = 0.25,
    tile_min_dim: int = 640,
) -> Tuple[ImageDetectionResponse, bytes]:
    """Execute complete image inference pipeline with adaptive high-resolution tiling."""
    # 1. Decode image
    bgr_img = decode_image_bytes(image_bytes)
    orig_h, orig_w = bgr_img.shape[:2]

    if output_names is None:
        output_names = [out.name for out in session.get_outputs()]

    all_boxes_xywh = []
    all_confidences = []
    all_coords = []

    # 2. Single Full-Image Inference
    full_candidates = infer_crop_candidates(
        session=session,
        crop=bgr_img,
        input_name=input_name,
        output_names=output_names,
        conf_threshold=conf_threshold,
        target_class_id=POTHOLE_CLASS_ID,
    )

    for x1, y1, x2, y2, score in full_candidates:
        w_box = x2 - x1
        h_box = y2 - y1
        all_boxes_xywh.append([int(round(x1)), int(round(y1)), int(round(w_box)), int(round(h_box))])
        all_confidences.append(score)
        all_coords.append((x1, y1, x2, y2))

    # 3. High-Resolution Sliced / Tiled Inference
    if enable_tiling and (orig_w > tile_min_dim or orig_h > tile_min_dim):
        tiles = slice_image_for_tiling(
            bgr_img, tile_size=tile_size, overlap_ratio=tile_overlap
        )
        for tile_crop, offset_x, offset_y in tiles:
            tile_candidates = infer_crop_candidates(
                session=session,
                crop=tile_crop,
                input_name=input_name,
                output_names=output_names,
                conf_threshold=conf_threshold,
                target_class_id=POTHOLE_CLASS_ID,
            )
            for lx1, ly1, lx2, ly2, score in tile_candidates:
                gx1 = max(0.0, min(float(orig_w), lx1 + offset_x))
                gy1 = max(0.0, min(float(orig_h), ly1 + offset_y))
                gx2 = max(0.0, min(float(orig_w), lx2 + offset_x))
                gy2 = max(0.0, min(float(orig_h), ly2 + offset_y))

                w_box = gx2 - gx1
                h_box = gy2 - gy1
                if w_box >= 1.0 and h_box >= 1.0:
                    all_boxes_xywh.append([int(round(gx1)), int(round(gy1)), int(round(w_box)), int(round(h_box))])
                    all_confidences.append(score)
                    all_coords.append((gx1, gy1, gx2, gy2))

    # 4. Global Non-Maximum Suppression
    detections: List[DetectionItem] = []
    total_image_area = float(orig_w * orig_h)

    if all_boxes_xywh:
        indices = cv2.dnn.NMSBoxes(
            all_boxes_xywh,
            all_confidences,
            score_threshold=conf_threshold,
            nms_threshold=iou_threshold,
        )

        if len(indices) > 0:
            flat_indices = (
                indices.flatten() if hasattr(indices, "flatten") else indices
            )
            for i in flat_indices:
                x1, y1, x2, y2 = all_coords[i]
                conf = float(all_confidences[i])
                conf_clamped = max(0.0, min(1.0, conf))

                box_area = (x2 - x1) * (y2 - y1)
                raw_ratio = box_area / total_image_area if total_image_area > 0 else 0.0
                ratio_clamped = max(0.0, min(1.0, raw_ratio))

                detections.append(
                    DetectionItem(
                        box=BoundingBox(
                            xmin=int(round(x1)),
                            ymin=int(round(y1)),
                            xmax=int(round(x2)),
                            ymax=int(round(y2)),
                        ),
                        confidence=float(round(conf_clamped, 4)),
                        class_id=POTHOLE_CLASS_ID,
                        class_name=POTHOLE_CLASS_NAME,
                        visual_area_ratio=float(round(ratio_clamped, 6)),
                    )
                )

    detections.sort(key=lambda d: d.confidence, reverse=True)

    # 5. Aggregate metrics
    pothole_count = len(detections)
    max_confidence = (
        max([d.confidence for d in detections]) if detections else 0.0
    )
    max_area_ratio = (
        max([d.visual_area_ratio for d in detections]) if detections else 0.0
    )

    # 6. Annotate Image
    annotated_bgr = annotate_image_with_detections(bgr_img, detections)
    success, encoded_jpg = cv2.imencode(
        ".jpg", annotated_bgr, [cv2.IMWRITE_JPEG_QUALITY, 92]
    )
    if not success:
        raise RuntimeError("Failed to encode annotated image to JPEG.")

    response_data = ImageDetectionResponse(
        model=ModelInfo(name=MODEL_NAME, version=MODEL_VERSION),
        image=ImageDimension(width=orig_w, height=orig_h),
        pothole_count=pothole_count,
        max_confidence=float(round(max_confidence, 4)),
        max_area_ratio=float(round(max_area_ratio, 6)),
        detections=detections,
    )

    return response_data, encoded_jpg.tobytes()
