import os
import pytest
from fastapi.testclient import TestClient
from app.config import Settings
from app.main import app
from app.model import PotholeModel, pothole_model


@pytest.fixture
def client():
    with TestClient(app) as test_client:
        yield test_client


def test_health_endpoint_with_loaded_model(client):
    """Verify /health returns 200 OK and reports model_loaded=True when model is present."""
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["model_loaded"] is True
    assert "pothole_yolov8.onnx" in data["model_path"]
    assert data["version"] == "0.1.0"

    # Check model metadata
    metadata = data["model_metadata"]
    assert metadata is not None
    assert metadata["model_name"] == "peterhdd/pothole-detection-yolov8"
    assert metadata["model_format"] == "ONNX"
    assert metadata["input_tensor_name"] == "images"
    assert metadata["input_shape"] == [1, 3, 640, 640]
    assert metadata["output_tensor_name"] == "output0"
    assert metadata["output_shape"] == [1, 5, 8400]
    assert metadata["pothole_class_id"] == 0
    assert metadata["classes"]["0"] == "pothole"


def test_onnx_model_session_initialization():
    """Verify ONNX model session is initialized and accessible."""
    session = pothole_model.get_session()
    assert session is not None
    assert len(session.get_inputs()) == 1
    assert session.get_inputs()[0].name == "images"
    assert session.get_inputs()[0].shape == [1, 3, 640, 640]
    assert len(session.get_outputs()) == 1
    assert session.get_outputs()[0].name == "output0"
    assert session.get_outputs()[0].shape == [1, 5, 8400]


def test_config_defaults():
    """Verify configuration loads expected default values."""
    settings = Settings()
    assert settings.confidence_threshold == 0.15
    assert settings.nms_iou_threshold == 0.45
    assert settings.video_sample_fps == 2.0
    assert "pothole_yolov8.onnx" in settings.model_path


def test_config_validation():
    """Verify configuration validation rejects invalid parameters."""
    with pytest.raises(ValueError):
        Settings(CONFIDENCE_THRESHOLD=1.5)

    with pytest.raises(ValueError):
        Settings(CONFIDENCE_THRESHOLD=-0.1)

    with pytest.raises(ValueError):
        Settings(VIDEO_SAMPLE_FPS=-1.0)


def test_model_missing_artifact_handling():
    """Verify custom PotholeModel instance handles non-existent paths cleanly."""
    model = PotholeModel()
    assert not model.is_loaded

    result = model.load_model("/path/to/nonexistent/pothole_model.onnx")
    assert result is False
    assert model.is_loaded is False

    with pytest.raises(RuntimeError) as exc_info:
        model.get_session()
    assert "ONNX model is not loaded" in str(exc_info.value)
