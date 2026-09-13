import argparse
import os
import sys
import torch
from ultralytics import YOLO

def parse_args():
    parser = argparse.ArgumentParser(description="Train custom YOLO pothole detection model.")
    parser.add_argument("--dataset", type=str, default="ai-service/training/dataset.yaml", help="Path to dataset.yaml")
    parser.add_argument("--model", type=str, default="yolov8s.pt", help="Base model architecture (e.g., yolov8s.pt, yolov8n.pt)")
    parser.add_argument("--epochs", type=int, default=30, help="Number of training epochs")
    parser.add_argument("--batch", type=int, default=16, help="Batch size")
    parser.add_argument("--imgsz", type=int, default=640, help="Image resolution for training")
    parser.add_argument("--project", type=str, default="ai-service/training/runs", help="Project runs directory")
    parser.add_argument("--name", type=str, default="custom_yolov8s_pothole", help="Experiment name")
    parser.add_argument("--smoke-test", action="store_true", help="Run 1-epoch smoke test")
    parser.add_argument("--device", type=str, default="cpu", help="Device to train on (e.g. cpu, mps, 0)")
    return parser.parse_args()

def main():
    torch.set_num_threads(8)
    args = parse_args()
    
    if args.smoke_test:
        args.epochs = 1
        args.name = "smoke_test_run"
        print("=== EXECUTING 1-EPOCH SMOKE TEST ===")

    print(f"Loading base architecture '{args.model}'...")
    model = YOLO(args.model)

    dataset_path = os.path.abspath(args.dataset)
    project_path = os.path.abspath(args.project)

    print(f"Starting training run '{args.name}'...")
    print(f"  Dataset YAML: {dataset_path}")
    print(f"  Image Size: {args.imgsz}")
    print(f"  Epochs: {args.epochs}")
    print(f"  Batch Size: {args.batch}")
    print(f"  Device: {args.device}")

    results = model.train(
        data=dataset_path,
        epochs=args.epochs,
        batch=args.batch,
        imgsz=args.imgsz,
        project=project_path,
        name=args.name,
        seed=42,
        deterministic=True,
        verbose=True,
        device=args.device,
        workers=0, # Process in main thread to eliminate macOS Python 3.14 multiprocessing lock
        plots=False, # Disable Matplotlib plot generation lock
        amp=False, # Disable AMP for PyTorch MPS compatibility on Apple Silicon
        # Augmentation hyperparameters
        hsv_h=0.015,
        hsv_s=0.7,
        hsv_v=0.4,
        translate=0.1,
        scale=0.5,
        fliplr=0.5,
        mosaic=1.0,
        erasing=0.2,
    )

    print(f"Training completed! Results saved to {os.path.join(project_path, args.name)}")
    return results

if __name__ == "__main__":
    main()
