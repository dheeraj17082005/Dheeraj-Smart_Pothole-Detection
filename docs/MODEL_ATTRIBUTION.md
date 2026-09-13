# Machine Learning Model Attribution & Specification

## Model Overview
- **Model Identifier**: `peterhdd/pothole-detection-yolov8`
- **Model Architecture**: YOLOv8s (Small object detection architecture, ~11.1M parameters)
- **Deployment Format**: Open Neural Network Exchange (ONNX Runtime)
- **Source Repository**: [Hugging Face: peterhdd/pothole-detection-yolov8](https://huggingface.co/peterhdd/pothole-detection-yolov8)
- **License**: Apache-2.0

## Technical Specifications
- **Input Tensor Name**: `images`
- **Input Dimensions**: `[1, 3, 640, 640]` (Batch size 1, 3 RGB channels, 640x640 resolution)
- **Input Datatype**: `float32` (normalized to $[0.0, 1.0]$)
- **Output Tensor Name**: `output0`
- **Output Dimensions**: `[1, 5, 8400]` (5 attributes: 4 bounding box coords $+ 1$ pothole class score across 8,400 anchor predictions)
- **Artifact Size**: ~44.7 MB (`best.onnx`)

## Target Class Mapping
```yaml
0: pothole            # Primary Target Class
```

## Dataset & Training Information
- **Domain**: Pothole and road surface damage detection from vehicle/dashcam and street viewpoints.
- **Execution Provider**: CPUExecutionProvider (ONNX Runtime) for deterministic, cross-platform containerized execution.
- **Empirical Evaluation**: Selected following an empirical multi-model benchmark (see `docs/MODEL_COMPARISON.md`) for 100% recall across 0.10-0.40 thresholds and high confidence (0.8275 avg TP).

## Citation
```bibtex
@misc{peterhdd-pothole-yolov8-2026,
  author = {peterhdd},
  title = {Pothole Detection with YOLOv8s},
  year = {2026},
  publisher = {Hugging Face},
  howpublished = {https://huggingface.co/peterhdd/pothole-detection-yolov8}
}
```
