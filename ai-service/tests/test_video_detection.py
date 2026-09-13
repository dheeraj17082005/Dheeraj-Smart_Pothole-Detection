import os
import tempfile
import cv2
import numpy as np
import pytest
from fastapi.testclient import TestClient

from app.main import app

@pytest.fixture
def client():
    with TestClient(app) as test_client:
        yield test_client


def create_synthetic_mp4(num_frames=6, width=640, height=480, fps=10.0) -> bytes:
    """Create a temporary MP4 video in memory and return binary content."""
    with tempfile.NamedTemporaryFile(suffix=".mp4", delete=False) as tf:
        temp_path = tf.name
    try:
        fourcc = cv2.VideoWriter_fourcc(*"mp4v")
        out = cv2.VideoWriter(temp_path, fourcc, fps, (width, height))
        for i in range(num_frames):
            frame = np.full((height, width, 3), 120, dtype=np.uint8)
            # Add darker shape in middle to simulate surface variation
            cv2.ellipse(frame, (320, 240), (80, 40), 0, 0, 360, (40, 40, 40), -1)
            out.write(frame)
        out.release()
        with open(temp_path, "rb") as f:
            return f.read()
    finally:
        if os.path.exists(temp_path):
            os.remove(temp_path)


def test_detect_video_success(client):
    """Verify POST /detect/video returns structured VideoDetectionResponse."""
    video_bytes = create_synthetic_mp4(num_frames=10, fps=10.0)
    response = client.post(
        "/detect/video?sample_fps=2.0",
        files={"file": ("test.mp4", video_bytes, "video/mp4")},
    )
    assert response.status_code == 200
    data = response.json()

    assert "model" in data
    assert data["model"]["name"] == "peterhdd/pothole-detection-yolov8"

    assert "video" in data
    video_meta = data["video"]
    assert video_meta["fps"] == 10.0
    assert video_meta["sample_fps"] == 2.0
    assert video_meta["total_frames_sampled"] > 0

    assert "frames" in data
    assert len(data["frames"]) == video_meta["total_frames_sampled"]

    for frame in data["frames"]:
        assert "frame_index" in frame
        assert "timestamp_sec" in frame
        assert "image_width" in frame
        assert "image_height" in frame
        assert "detections" in frame


def test_annotate_video_frame(client):
    """Verify POST /detect/video/annotate-frame extracts frame and returns binary JPEG."""
    video_bytes = create_synthetic_mp4(num_frames=8, fps=10.0)
    response = client.post(
        "/detect/video/annotate-frame?frame_index=2",
        files={"file": ("test.mp4", video_bytes, "video/mp4")},
    )
    assert response.status_code == 200
    assert response.headers["content-type"] == "image/jpeg"
    assert response.headers["x-frame-index"] == "2"
    assert "x-timestamp-sec" in response.headers
    assert len(response.content) > 0


def test_detect_video_empty_payload(client):
    """Verify empty video payload returns HTTP 400 with INVALID_VIDEO error code."""
    response = client.post(
        "/detect/video",
        files={"file": ("empty.mp4", b"", "video/mp4")},
    )
    assert response.status_code == 400
    data = response.json()
    assert data["error"]["code"] == "INVALID_VIDEO"


def test_detect_video_corrupt_payload(client):
    """Verify corrupted video payload returns HTTP 400."""
    response = client.post(
        "/detect/video",
        files={"file": ("corrupt.mp4", b"not_a_video_header_12345", "video/mp4")},
    )
    assert response.status_code == 400
    data = response.json()
    assert data["error"]["code"] == "INVALID_VIDEO"
