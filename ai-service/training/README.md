# Experimental Custom Pothole YOLO Training Pipeline

This directory contains the reproducible training, validation, export, and benchmarking scripts for fine-tuning YOLO object detection models on pothole datasets.

## Directory Structure
- `dataset.yaml`: Dataset specification pointing to training/validation splits.
- `train.py`: Main PyTorch training script using Ultralytics YOLOv8s.
- `evaluate.py`: Standalone evaluation script measuring mAP and multi-pothole recall.
- `export_onnx.py`: ONNX export and independent verification script.
- `configs/`: Experiment configuration YAML files.
- `data/`: Local dataset directory.
- `runs/`: Experiment training outputs and weights.

## Execution
```bash
# Smoke test (1-2 epochs)
python training/train.py --smoke-test

# Full training experiment (50 epochs)
python training/train.py --epochs 50 --imgsz 640

# ONNX Export
python training/export_onnx.py --weights training/runs/custom_yolov8s_pothole/weights/best.pt
```
