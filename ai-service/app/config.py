import os
from functools import lru_cache
from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Configuration settings for the Pothole AI Service."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = Field(default="Smart Pothole AI Service", alias="APP_NAME")
    app_version: str = Field(default="0.1.0", alias="APP_VERSION")
    model_path: str = Field(
        default=os.path.join(
            os.path.dirname(os.path.dirname(__file__)),
            "models",
            "pothole_yolov8.onnx",
        ),
        alias="MODEL_PATH",
    )
    confidence_threshold: float = Field(
        default=0.15, alias="CONFIDENCE_THRESHOLD"
    )
    nms_iou_threshold: float = Field(
        default=0.45, alias="NMS_IOU_THRESHOLD"
    )
    video_sample_fps: float = Field(default=2.0, alias="VIDEO_SAMPLE_FPS")
    tiling_enabled: bool = Field(default=True, alias="TILING_ENABLED")
    tile_size: int = Field(default=640, alias="TILE_SIZE")
    tile_overlap: float = Field(default=0.25, alias="TILE_OVERLAP")
    tile_min_dimension: int = Field(default=1200, alias="TILE_MIN_DIMENSION")

    @field_validator("confidence_threshold")
    @classmethod
    def validate_confidence(cls, v: float) -> float:
        if not (0.0 < v <= 1.0):
            raise ValueError(
                f"Confidence threshold must be between 0.0 and 1.0, got {v}"
            )
        return v

    @field_validator("nms_iou_threshold")
    @classmethod
    def validate_iou(cls, v: float) -> float:
        if not (0.0 < v <= 1.0):
            raise ValueError(
                f"NMS IoU threshold must be between 0.0 and 1.0, got {v}"
            )
        return v

    @field_validator("tile_overlap")
    @classmethod
    def validate_tile_overlap(cls, v: float) -> float:
        if not (0.0 <= v < 1.0):
            raise ValueError(
                f"Tile overlap must be between 0.0 and 1.0, got {v}"
            )
        return v

    @field_validator("video_sample_fps")
    @classmethod
    def validate_fps(cls, v: float) -> float:
        if v <= 0.0:
            raise ValueError(
                f"Video sample FPS must be greater than 0.0, got {v}"
            )
        return v


@lru_cache()
def get_settings() -> Settings:
    """Return cached application settings instance."""
    return Settings()
