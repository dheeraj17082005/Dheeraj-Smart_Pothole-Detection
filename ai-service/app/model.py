import ast
import logging
import os
from typing import Dict, List, Optional
import onnxruntime as ort

from app.schemas import ModelMetadata

logger = logging.getLogger("pothole_ai_service.model")

DEFAULT_CLASSES: Dict[int, str] = {
    0: "pothole",
}


class PotholeModel:
    """Wrapper for loading and holding the YOLOv8 ONNX model session."""

    def __init__(self):
        self._session: Optional[ort.InferenceSession] = None
        self._model_path: Optional[str] = None
        self._input_name: Optional[str] = None
        self._input_shape: List[int] = [1, 3, 640, 640]
        self._output_names: list[str] = []
        self._output_shape: List[int] = [1, 5, 8400]
        self._classes: Dict[int, str] = DEFAULT_CLASSES
        self._metadata: Optional[ModelMetadata] = None

    @property
    def is_loaded(self) -> bool:
        """Return True if ONNX model is loaded and ready for inference."""
        return self._session is not None

    @property
    def model_path(self) -> Optional[str]:
        return self._model_path

    @property
    def input_name(self) -> Optional[str]:
        return self._input_name

    @property
    def input_shape(self) -> List[int]:
        return self._input_shape

    @property
    def output_names(self) -> list[str]:
        return self._output_names

    @property
    def classes(self) -> Dict[int, str]:
        return self._classes

    @property
    def metadata(self) -> Optional[ModelMetadata]:
        return self._metadata

    def load_model(self, model_path: str) -> bool:
        """Load ONNX model session exactly once.

        If the model file does not exist, log a clear warning and remain
        in an unloaded state.
        """
        self._model_path = model_path

        if not os.path.exists(model_path):
            logger.warning(
                f"Model artifact not found at '{model_path}'. "
                f"The AI service will start in a non-ready state (model_loaded=False). "
                f"Place the 'vinothvikas1987/pothole-detection-yolov8' ONNX artifact at "
                f"'{model_path}' before running detection inference."
            )
            self._session = None
            self._metadata = None
            return False

        try:
            logger.info(f"Loading ONNX model artifact from '{model_path}'...")
            session_options = ort.SessionOptions()
            session_options.graph_optimization_level = (
                ort.GraphOptimizationLevel.ORT_ENABLE_ALL
            )
            # Use CPU execution provider for deterministic local execution
            self._session = ort.InferenceSession(
                model_path,
                sess_options=session_options,
                providers=["CPUExecutionProvider"],
            )

            # Inspect tensor properties
            inputs = self._session.get_inputs()
            self._input_name = inputs[0].name
            self._input_shape = list(inputs[0].shape)

            outputs = self._session.get_outputs()
            self._output_names = [out.name for out in outputs]
            self._output_shape = list(outputs[0].shape)

            # Extract embedded metadata if available
            custom_meta = self._session.get_modelmeta().custom_metadata_map
            if "names" in custom_meta:
                try:
                    parsed_classes = ast.literal_eval(custom_meta["names"])
                    if isinstance(parsed_classes, dict):
                        if len(parsed_classes) == 1 and list(parsed_classes.values()) == ["0"]:
                            self._classes = {0: "pothole"}
                        else:
                            self._classes = {
                                int(k): str(v) for k, v in parsed_classes.items()
                            }
                except Exception as parse_err:
                    logger.warning(
                        f"Could not parse custom metadata names: {parse_err}. Using default class map."
                    )

            # Determine pothole class ID from classes dictionary
            pothole_id = 0
            for cid, cname in self._classes.items():
                if "pothole" in cname.lower():
                    pothole_id = cid
                    break

            # Build metadata representation
            self._metadata = ModelMetadata(
                model_name="peterhdd/pothole-detection-yolov8",
                model_version="YOLOv8s",
                model_format="ONNX",
                input_tensor_name=self._input_name or "images",
                input_shape=self._input_shape,
                output_tensor_name=self._output_names[0]
                if self._output_names
                else "output0",
                output_shape=self._output_shape,
                classes=self._classes,
                pothole_class_id=pothole_id,
            )

            logger.info(
                f"ONNX model loaded successfully. Input: {self._input_name} {self._input_shape}, "
                f"Output: {self._output_names} {self._output_shape}, Classes: {self._classes}"
            )
            return True
        except Exception as e:
            logger.error(
                f"Failed to load ONNX model from '{model_path}': {e}",
                exc_info=True,
            )
            self._session = None
            self._metadata = None
            return False

    def get_session(self) -> ort.InferenceSession:
        """Return the active ONNX inference session or raise error if unloaded."""
        if not self.is_loaded or self._session is None:
            raise RuntimeError(
                "ONNX model is not loaded. Ensure the model artifact exists at the configured MODEL_PATH."
            )
        return self._session


# Singleton model instance for application lifespan
pothole_model = PotholeModel()
