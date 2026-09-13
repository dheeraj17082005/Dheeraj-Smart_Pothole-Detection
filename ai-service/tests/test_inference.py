import os
import cv2
import numpy as np
import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.image_processor import (
    decode_image_bytes,
    letterbox_image,
    preprocess_for_onnx,
    postprocess_yolov8_output,
    run_image_inference,
)
from app.model import pothole_model

candidate_dirs = [
    os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "test-data", "images"),
    "/app/test-data/images",
    "test-data/images",
]
TEST_DATA_DIR = next((d for d in candidate_dirs if os.path.exists(d)), candidate_dirs[0])

candidate_eval_dirs = [
    os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "test-data", "evaluation"),
    "/app/test-data/evaluation",
    "test-data/evaluation",
]
EVAL_DATA_DIR = next((d for d in candidate_eval_dirs if os.path.exists(d)), candidate_eval_dirs[0])

POTHOLE_IMAGE_PATH = os.path.join(TEST_DATA_DIR, "pothole_sample.jpg")
CLEAN_IMAGE_PATH = os.path.join(TEST_DATA_DIR, "clean_road_test.jpg")


@pytest.fixture
def client():
    with TestClient(app) as test_client:
        yield test_client


def test_letterbox_dimensions_and_scaling():
    """Verify letterboxing scales image correctly to 640x640 with proper padding."""
    dummy_img = np.zeros((480, 640, 3), dtype=np.uint8)
    padded, scale, (pad_x, pad_y) = letterbox_image(dummy_img, (640, 640))
    assert padded.shape == (640, 640, 3)
    assert scale == 1.0
    assert pad_x == 0.0
    assert pad_y == 80.0


def test_preprocess_for_onnx():
    """Verify tensor shape, normalization range [0, 1], and channel ordering."""
    dummy_img = np.full((300, 400, 3), 128, dtype=np.uint8)
    tensor, orig_w, orig_h, scale, (pad_x, pad_y) = preprocess_for_onnx(
        dummy_img, (640, 640)
    )
    assert tensor.shape == (1, 3, 640, 640)
    assert tensor.dtype == np.float32
    assert orig_w == 400
    assert orig_h == 300
    assert 0.0 <= tensor.min() <= tensor.max() <= 1.0


def test_detect_image_endpoint_valid_pothole(client):
    """Verify real pothole image produces valid structured detections with nested schema and class_id=3."""
    assert os.path.exists(POTHOLE_IMAGE_PATH), f"Missing test fixture: {POTHOLE_IMAGE_PATH}"
    with open(POTHOLE_IMAGE_PATH, "rb") as f:
        file_bytes = f.read()

    response = client.post(
        "/detect/image",
        files={"file": ("pothole_sample.jpg", file_bytes, "image/jpeg")},
    )

    assert response.status_code == 200
    data = response.json()

    # Verify nested model metadata
    assert "model" in data
    assert data["model"]["name"] == "peterhdd/pothole-detection-yolov8"
    assert data["model"]["version"] == "YOLOv8s"

    # Verify nested image dimensions
    assert "image" in data
    assert data["image"]["width"] > 0
    assert data["image"]["height"] > 0

    assert "pothole_count" in data
    assert "detections" in data

    if data["pothole_count"] > 0:
        det = data["detections"][0]
        assert det["class_id"] == 0
        assert det["class_name"] == "pothole"
        assert 0.0 <= det["confidence"] <= 1.0
        assert 0.0 <= det["visual_area_ratio"] <= 1.0

        # Verify nested box coordinates
        box = det["box"]
        assert 0 <= box["xmin"] < box["xmax"] <= data["image"]["width"]
        assert 0 <= box["ymin"] < box["ymax"] <= data["image"]["height"]

        # Check visual_area_ratio calculation
        box_w = box["xmax"] - box["xmin"]
        box_h = box["ymax"] - box["ymin"]
        expected_ratio = (box_w * box_h) / (
            data["image"]["width"] * data["image"]["height"]
        )
        assert pytest.approx(det["visual_area_ratio"], abs=0.005) == expected_ratio


def test_detect_image_endpoint_clean_road(client):
    """Verify clean asphalt image returns 0 detections cleanly with HTTP 200."""
    assert os.path.exists(CLEAN_IMAGE_PATH), f"Missing test fixture: {CLEAN_IMAGE_PATH}"
    with open(CLEAN_IMAGE_PATH, "rb") as f:
        file_bytes = f.read()

    response = client.post(
        "/detect/image",
        files={"file": ("clean_road_test.jpg", file_bytes, "image/jpeg")},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["pothole_count"] == 0
    assert data["detections"] == []
    assert data["max_confidence"] == 0.0
    assert data["max_area_ratio"] == 0.0


def test_detect_image_confidence_threshold_filtering(client):
    """Verify setting high confidence threshold filters out detections."""
    with open(POTHOLE_IMAGE_PATH, "rb") as f:
        file_bytes = f.read()

    # With extreme confidence threshold of 0.999
    response = client.post(
        "/detect/image?confidence_threshold=0.999",
        files={"file": ("pothole_sample.jpg", file_bytes, "image/jpeg")},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["pothole_count"] == 0
    assert data["detections"] == []


def test_detect_image_invalid_corrupted_payload(client):
    """Verify corrupted non-image bytes return structured HTTP 400 INVALID_IMAGE error."""
    corrupted_bytes = b"This is not a valid JPEG/PNG image binary payload."
    response = client.post(
        "/detect/image",
        files={"file": ("corrupt.jpg", corrupted_bytes, "image/jpeg")},
    )
    assert response.status_code == 400
    data = response.json()
    assert "error" in data
    assert data["error"]["code"] == "INVALID_IMAGE"
    assert "Unable to decode" in data["error"]["message"]


def test_detect_image_empty_payload(client):
    """Verify empty payload returns structured HTTP 400 INVALID_IMAGE error."""
    response = client.post(
        "/detect/image",
        files={"file": ("empty.jpg", b"", "image/jpeg")},
    )
    assert response.status_code == 400
    data = response.json()
    assert "error" in data
    assert data["error"]["code"] == "INVALID_IMAGE"


def test_detect_and_annotate_image_endpoint(client):
    """Verify /detect/image/annotate returns image/jpeg and detection headers."""
    with open(POTHOLE_IMAGE_PATH, "rb") as f:
        file_bytes = f.read()

    response = client.post(
        "/detect/image/annotate",
        files={"file": ("pothole_sample.jpg", file_bytes, "image/jpeg")},
    )

    assert response.status_code == 200
    assert response.headers["content-type"] == "image/jpeg"
    assert "X-Pothole-Count" in response.headers
    assert "X-Detection-Metadata" in response.headers

    # Verify returned JPEG can be decoded
    annotated_img = cv2.imdecode(
        np.frombuffer(response.content, np.uint8), cv2.IMREAD_COLOR
    )
    assert annotated_img is not None
    assert annotated_img.shape[0] > 0 and annotated_img.shape[1] > 0


def test_configured_thresholds_defaults():
    """Verify default Settings have confidence_threshold=0.15 and nms_iou_threshold=0.45."""
    from app.config import Settings
    settings = Settings()
    assert settings.confidence_threshold == 0.15
    assert settings.nms_iou_threshold == 0.45


def test_multi_pothole_recall_survives_nms(client):
    """Verify multi-pothole images correctly yield multiple distinct detections at production threshold."""
    multi_pothole_path = os.path.join(
        EVAL_DATA_DIR,
        "potholes",
        "pothole_rdd_341.jpg"
    )
    if not os.path.exists(multi_pothole_path):
        pytest.skip(f"Test image not found at {multi_pothole_path}")

    with open(multi_pothole_path, "rb") as f:
        file_bytes = f.read()

    response = client.post(
        "/detect/image",
        files={"file": ("pothole_rdd_341.jpg", file_bytes, "image/jpeg")},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["pothole_count"] >= 3
    assert len(data["detections"]) >= 3
    # Check that each detection has a valid bounding box and confidence >= 0.15
    for det in data["detections"]:
        assert det["confidence"] >= 0.15
        assert det["box"]["xmax"] > det["box"]["xmin"]
        assert det["box"]["ymax"] > det["box"]["ymin"]


def test_clean_road_controls_remain_negative(client):
    """Verify multiple clean road controls remain at 0 detections under 0.15 threshold."""
    clean_dir = os.path.join(
        EVAL_DATA_DIR,
        "clean_roads"
    )
    if not os.path.exists(clean_dir):
        pytest.skip("Clean roads directory not found")

    for fname in ["clean_road_highway.jpg", "clean_road_urban.jpg", "clean_road_paved.jpg"]:
        fpath = os.path.join(clean_dir, fname)
        if os.path.exists(fpath):
            with open(fpath, "rb") as f:
                file_bytes = f.read()
            response = client.post(
                "/detect/image",
                files={"file": (fname, file_bytes, "image/jpeg")},
            )
            assert response.status_code == 200
            data = response.json()
            assert data["pothole_count"] == 0, f"False positive detected on {fname}: {data}"


def test_slice_image_for_tiling():
    """Verify grid slicing correctly partitions large images into overlapping crops."""
    from app.image_processor import slice_image_for_tiling

    # Image smaller than tile size -> should return empty list
    small_img = np.zeros((400, 500, 3), dtype=np.uint8)
    tiles_small = slice_image_for_tiling(small_img, tile_size=640, overlap_ratio=0.25)
    assert len(tiles_small) == 0

    # Image larger than tile size (1280x960) -> should return multiple tiles
    large_img = np.zeros((960, 1280, 3), dtype=np.uint8)
    tiles_large = slice_image_for_tiling(large_img, tile_size=640, overlap_ratio=0.25)
    assert len(tiles_large) > 1

    # Verify each tile crop does not exceed tile_size
    for crop, ox, oy in tiles_large:
        h, w = crop.shape[:2]
        assert w <= 640
        assert h <= 640
        assert ox >= 0 and ox + w <= 1280
        assert oy >= 0 and oy + h <= 960


def test_tiled_high_resolution_image_inference(client):
    """Verify high-resolution pothole image (1920x1920) benefits from tiled inference."""
    hi_res_path = os.path.join(
        EVAL_DATA_DIR,
        "potholes",
        "pothole_existing_sample.jpg"
    )
    if not os.path.exists(hi_res_path):
        pytest.skip(f"Test fixture not found: {hi_res_path}")

    with open(hi_res_path, "rb") as f:
        file_bytes = f.read()

    response = client.post(
        "/detect/image",
        files={"file": ("pothole_existing_sample.jpg", file_bytes, "image/jpeg")},
    )

    assert response.status_code == 200
    data = response.json()
    # High-resolution image should detect multiple defect clusters
    assert data["pothole_count"] >= 3
    assert len(data["detections"]) >= 3
    for det in data["detections"]:
        assert det["confidence"] >= 0.15
        assert 0 <= det["box"]["xmin"] < det["box"]["xmax"] <= 1920
        assert 0 <= det["box"]["ymin"] < det["box"]["ymax"] <= 1920

