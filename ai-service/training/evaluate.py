import os
import sys
import glob
import time
import json
import cv2
import numpy as np
import onnxruntime as ort
from ultralytics import YOLO

# Ground truths for diagnostic images
BENCHMARK_IMAGES = {
    "istockphoto-502561495-612x612.jpg": {"human_count": 4, "path": "test-data/evaluation/potholes/istockphoto-502561495-612x612.jpg"},
    "pothole_rdd_298.jpg": {"human_count": 4, "path": "test-data/evaluation/potholes/pothole_rdd_298.jpg"},
    "pothole_rdd_341.jpg": {"human_count": 3, "path": "test-data/evaluation/potholes/pothole_rdd_341.jpg"},
    "istockphoto_multi.jpg": {"human_count": 2, "path": "test-data/evaluation/potholes/istockphoto_multi.jpg"},
}

def nms(boxes, scores, iou_threshold=0.45):
    if len(boxes) == 0:
        return []
    x1 = boxes[:, 0]
    y1 = boxes[:, 1]
    x2 = boxes[:, 2]
    y2 = boxes[:, 3]
    areas = (x2 - x1) * (y2 - y1)
    order = scores.argsort()[::-1]
    keep = []
    while order.size > 0:
        i = order[0]
        keep.append(i)
        xx1 = np.maximum(x1[i], x1[order[1:]])
        yy1 = np.maximum(y1[i], y1[order[1:]])
        xx2 = np.minimum(x2[i], x2[order[1:]])
        yy2 = np.minimum(y2[i], y2[order[1:]])
        w = np.maximum(0.0, xx2 - xx1)
        h = np.maximum(0.0, yy2 - yy1)
        inter = w * h
        ovr = inter / (areas[i] + areas[order[1:]] - inter)
        inds = np.where(ovr <= iou_threshold)[0]
        order = order[inds + 1]
    return keep

def run_onnx_inference(onnx_path, img_path, conf_thresh=0.25, iou_thresh=0.45):
    session = ort.InferenceSession(onnx_path, providers=['CPUExecutionProvider'])
    input_name = session.get_inputs()[0].name
    
    image = cv2.imread(img_path)
    if image is None:
        return [], 0.0
    h_orig, w_orig = image.shape[:2]
    
    img_resized = cv2.resize(image, (640, 640))
    img_rgb = cv2.cvtColor(img_resized, cv2.COLOR_BGR2RGB)
    img_norm = img_rgb.astype(np.float32) / 255.0
    img_trans = np.transpose(img_norm, (2, 0, 1))
    input_tensor = np.expand_dims(img_trans, axis=0)
    
    t0 = time.time()
    outputs = session.run(None, {input_name: input_tensor})
    latency = (time.time() - t0) * 1000.0
    
    raw = outputs[0][0] # shape [5, 8400] or [6, 8400]
    if raw.shape[0] < raw.shape[1]:
        raw = raw.T # shape [8400, 5]
        
    boxes = []
    scores = []
    
    for row in raw:
        cx, cy, w, h = row[:4]
        # Class score(s)
        cls_scores = row[4:]
        max_score = float(np.max(cls_scores))
        if max_score >= conf_thresh:
            x1 = (cx - w / 2.0) / 640.0 * w_orig
            y1 = (cy - h / 2.0) / 640.0 * h_orig
            x2 = (cx + w / 2.0) / 640.0 * w_orig
            y2 = (cy + h / 2.0) / 640.0 * h_orig
            boxes.append([x1, y1, x2, y2])
            scores.append(max_score)
            
    boxes = np.array(boxes)
    scores = np.array(scores)
    
    if len(boxes) == 0:
        return [], latency
        
    keep_indices = nms(boxes, scores, iou_thresh)
    final_detections = []
    for idx in keep_indices:
        final_detections.append({
            "bbox": [float(b) for b in boxes[idx]],
            "confidence": float(scores[idx])
        })
    return final_detections, latency

def run_pytorch_inference(pt_path, img_path, conf_thresh=0.25, iou_thresh=0.45):
    try:
        model = YOLO(pt_path)
        t0 = time.time()
        results = model.predict(source=img_path, conf=conf_thresh, iou=iou_thresh, verbose=False, device='cpu')
        latency = (time.time() - t0) * 1000.0
        
        detections = []
        if len(results) > 0 and len(results[0].boxes) > 0:
            for box in results[0].boxes:
                b = box.xyxy[0].cpu().numpy().tolist()
                c = float(box.conf[0].cpu().numpy())
                detections.append({
                    "bbox": b,
                    "confidence": c
                })
        return detections, latency
    except Exception as e:
        print(f"  [Warning] PyTorch inference failed for {pt_path}: {e}")
        return [], 0.0

def evaluate_models(custom_model_path="ai-service/models/experimental/custom_yolov8s_pothole.onnx"):
    peterhdd_onnx = "ai-service/models/pothole_yolov8.onnx"
    pyresearch_pt = "ai-service/models/experimental/pyresearch_best.pt"
    
    print("=== STARTING BENCHMARK EVALUATION ===")
    
    # 1. Target Evaluation on istockphoto-502561495-612x612.jpg
    target_img = "test-data/evaluation/potholes/istockphoto-502561495-612x612.jpg"
    print(f"\n1. Target Benchmark: {target_img} (Ground Truth = 4 potholes)")
    
    peterhdd_dets, p_lat = run_onnx_inference(peterhdd_onnx, target_img)
    pyresearch_dets, pyr_lat = run_pytorch_inference(pyresearch_pt, target_img)
    
    if custom_model_path.endswith(".onnx"):
        custom_dets, c_lat = run_onnx_inference(custom_model_path, target_img)
    else:
        custom_dets, c_lat = run_pytorch_inference(custom_model_path, target_img)
        
    print(f"  peterhdd YOLOv8:  {len(peterhdd_dets)} detections ({p_lat:.1f} ms) -> {[round(d['confidence'], 3) for d in peterhdd_dets]}")
    print(f"  PyResearch best:  {len(pyresearch_dets)} detections ({pyr_lat:.1f} ms) -> {[round(d['confidence'], 3) for d in pyresearch_dets]}")
    print(f"  Custom YOLOv8s:   {len(custom_dets)} detections ({c_lat:.1f} ms) -> {[round(d['confidence'], 3) for d in custom_dets]}")
    
    # Render custom model annotated image
    img = cv2.imread(target_img)
    if img is not None:
        for det in custom_dets:
            x1, y1, x2, y2 = [int(v) for v in det['bbox']]
            conf = det['confidence']
            cv2.rectangle(img, (x1, y1), (x2, y2), (0, 255, 0), 2)
            cv2.putText(img, f"pothole {conf:.2f}", (x1, max(15, y1 - 5)),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)
        os.makedirs("docs/diagnostics", exist_ok=True)
        out_diag = "docs/diagnostics/istockphoto_custom_yolov8s.jpg"
        cv2.imwrite(out_diag, img)
        print(f"  Annotated image written to {out_diag}")
        
    # 2. Multi-Pothole Test Set Evaluation
    pothole_images = glob.glob("test-data/evaluation/potholes/*")
    print(f"\n2. Multi-Pothole Benchmark ({len(pothole_images)} images)")
    
    p_total_dets = 0
    pyr_total_dets = 0
    c_total_dets = 0
    
    for p_img in pothole_images:
        p_d, _ = run_onnx_inference(peterhdd_onnx, p_img)
        pyr_d, _ = run_pytorch_inference(pyresearch_pt, p_img)
        if custom_model_path.endswith(".onnx"):
            c_d, _ = run_onnx_inference(custom_model_path, p_img)
        else:
            c_d, _ = run_pytorch_inference(custom_model_path, p_img)
            
        p_total_dets += len(p_d)
        pyr_total_dets += len(pyr_d)
        c_total_dets += len(c_d)
        
    print(f"  peterhdd total detections across {len(pothole_images)} images: {p_total_dets}")
    print(f"  PyResearch total detections across {len(pothole_images)} images: {pyr_total_dets}")
    print(f"  Custom YOLOv8s total detections across {len(pothole_images)} images: {c_total_dets}")

    # 3. Clean Road False Positive Evaluation
    clean_images = glob.glob("test-data/evaluation/clean_roads/*")
    print(f"\n3. Clean Road Benchmark ({len(clean_images)} images)")
    
    p_fps = 0
    pyr_fps = 0
    c_fps = 0
    
    for c_img in clean_images:
        p_d, _ = run_onnx_inference(peterhdd_onnx, c_img)
        pyr_d, _ = run_pytorch_inference(pyresearch_pt, c_img)
        if custom_model_path.endswith(".onnx"):
            c_d, _ = run_onnx_inference(custom_model_path, c_img)
        else:
            c_d, _ = run_pytorch_inference(custom_model_path, c_img)
            
        p_fps += len(p_d)
        pyr_fps += len(pyr_d)
        c_fps += len(c_d)
        
    print(f"  peterhdd clean-road FP count: {p_fps}")
    print(f"  PyResearch clean-road FP count: {pyr_fps}")
    print(f"  Custom YOLOv8s clean-road FP count: {c_fps}")
    
    return {
        "target_image": {
            "peterhdd_count": len(peterhdd_dets),
            "pyresearch_count": len(pyresearch_dets),
            "custom_count": len(custom_dets),
            "custom_confidences": [round(d['confidence'], 3) for d in custom_dets]
        },
        "potholes_total_detections": {
            "peterhdd": p_total_dets,
            "pyresearch": pyr_total_dets,
            "custom": c_total_dets
        },
        "clean_road_fps": {
            "peterhdd": p_fps,
            "pyresearch": pyr_fps,
            "custom": c_fps
        }
    }

if __name__ == "__main__":
    model_path = sys.argv[1] if len(sys.argv) > 1 else "ai-service/models/experimental/custom_yolov8s_pothole.onnx"
    evaluate_models(model_path)
