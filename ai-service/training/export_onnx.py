import os
import sys
import time
import hashlib
import numpy as np
import onnx
import onnxruntime as ort
from ultralytics import YOLO

def export_and_benchmark(weights_path, output_onnx_path, imgsz=640):
    print(f"=== EXPORTING {weights_path} TO ONNX ===")
    if not os.path.exists(weights_path):
        raise FileNotFoundError(f"Weights file not found: {weights_path}")
    
    os.makedirs(os.path.dirname(output_onnx_path), exist_ok=True)
    
    model = YOLO(weights_path)
    exported_path = model.export(format="onnx", imgsz=imgsz, dynamic=False, opset=12, simplify=True)
    
    if os.path.exists(output_onnx_path):
        os.remove(output_onnx_path)
    os.rename(exported_path, output_onnx_path)
    
    # Calculate file size & SHA256
    file_size_mb = os.path.getsize(output_onnx_path) / (1024 * 1024)
    sha256_hash = hashlib.sha256()
    with open(output_onnx_path, "rb") as f:
        for byte_block in iter(lambda: f.read(4096), b""):
            sha256_hash.update(byte_block)
    sha256 = sha256_hash.hexdigest()
    
    print(f"\n=== ONNX FILE METADATA ===")
    print(f"Path: {output_onnx_path}")
    print(f"File Size: {file_size_mb:.2f} MB")
    print(f"SHA-256: {sha256}")
    
    # Inspect ONNX Model
    onnx_model = onnx.load(output_onnx_path)
    onnx.checker.check_model(onnx_model)
    
    session = ort.InferenceSession(output_onnx_path, providers=['CPUExecutionProvider'])
    inputs = session.get_inputs()
    outputs = session.get_outputs()
    
    input_name = inputs[0].name
    input_shape = inputs[0].shape
    output_name = outputs[0].name
    output_shape = outputs[0].shape
    
    print(f"Input Name: {input_name}, Input Shape: {input_shape}")
    print(f"Output Name: {output_name}, Output Shape: {output_shape}")
    
    # Latency Benchmark
    dummy_input = np.random.randn(1, 3, imgsz, imgsz).astype(np.float32)
    # Warmup
    for _ in range(5):
        session.run([output_name], {input_name: dummy_input})
        
    num_runs = 30
    start_time = time.time()
    for _ in range(num_runs):
        session.run([output_name], {input_name: dummy_input})
    total_time = time.time() - start_time
    avg_latency_ms = (total_time / num_runs) * 1000
    fps = 1000 / avg_latency_ms
    
    print(f"CPU Average Latency: {avg_latency_ms:.2f} ms ({fps:.2f} FPS)")
    
    return {
        "output_onnx_path": output_onnx_path,
        "file_size_mb": file_size_mb,
        "sha256": sha256,
        "input_shape": input_shape,
        "output_shape": output_shape,
        "avg_latency_ms": avg_latency_ms,
        "fps": fps
    }

if __name__ == "__main__":
    weights = sys.argv[1] if len(sys.argv) > 1 else "ai-service/training/runs/custom_yolov8s_pothole/weights/best.pt"
    out_onnx = sys.argv[2] if len(sys.argv) > 2 else "ai-service/models/experimental/custom_yolov8s_pothole.onnx"
    export_and_benchmark(weights, out_onnx)
