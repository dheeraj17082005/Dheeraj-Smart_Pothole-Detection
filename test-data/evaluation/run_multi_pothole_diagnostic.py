import os
import sys
import json
import cv2
import numpy as np
import onnxruntime as ort

# Add app to path
sys.path.insert(0, '/app')
from app.image_processor import letterbox_image, preprocess_for_onnx

MODEL_PATH = '/app/models/pothole_yolov8.onnx'

def load_session():
    session_options = ort.SessionOptions()
    session_options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
    session = ort.InferenceSession(MODEL_PATH, sess_options=session_options, providers=["CPUExecutionProvider"])
    return session

def run_raw_inference(session, image_path):
    img = cv2.imread(image_path)
    if img is None:
        raise ValueError(f"Could not load image {image_path}")
    orig_h, orig_w = img.shape[:2]
    
    tensor, orig_w, orig_h, scale, (pad_x, pad_y) = preprocess_for_onnx(img, (640, 640))
    outputs = session.run(["output0"], {"images": tensor})
    output_tensor = outputs[0] # [1, 5, 8400]
    
    # Verify shape
    preds = output_tensor[0].T # [8400, 5]
    
    # Extract all candidates
    cx = preds[:, 0]
    cy = preds[:, 1]
    bw = preds[:, 2]
    bh = preds[:, 3]
    scores = preds[:, 4]
    
    # Calculate bounding boxes in original image space
    x1 = (cx - bw / 2.0 - pad_x) / scale
    y1 = (cy - bh / 2.0 - pad_y) / scale
    x2 = (cx + bw / 2.0 - pad_x) / scale
    y2 = (cy + bh / 2.0 - pad_y) / scale
    
    x1 = np.clip(x1, 0, orig_w)
    y1 = np.clip(y1, 0, orig_h)
    x2 = np.clip(x2, 0, orig_w)
    y2 = np.clip(y2, 0, orig_h)
    
    w_box = x2 - x1
    h_box = y2 - y1
    
    valid_idx = np.where((w_box >= 1.0) & (h_box >= 1.0))[0]
    
    return {
        "img": img,
        "orig_w": orig_w,
        "orig_h": orig_h,
        "scale": scale,
        "pad_x": pad_x,
        "pad_y": pad_y,
        "output_shape": list(output_tensor.shape),
        "scores": scores,
        "boxes_xywh": np.column_stack([x1, y1, w_box, h_box]),
        "boxes_xyxy": np.column_stack([x1, y1, x2, y2]),
        "valid_idx": valid_idx
    }

def evaluate_nms_and_conf(raw_data, conf_thresholds=[0.01, 0.10, 0.15, 0.20, 0.25, 0.30, 0.35], iou_thresholds=[0.30, 0.45, 0.50, 0.60, 0.70]):
    scores = raw_data["scores"]
    boxes_xywh = raw_data["boxes_xywh"]
    boxes_xyxy = raw_data["boxes_xyxy"]
    orig_w = raw_data["orig_w"]
    orig_h = raw_data["orig_h"]
    
    results = {}
    
    # 1. Raw candidate stats across thresholds (before NMS)
    results["raw_candidate_counts_before_nms"] = {}
    for c_th in [0.01, 0.05, 0.10, 0.15, 0.20, 0.25, 0.30, 0.35, 0.50]:
        mask = scores >= c_th
        results["raw_candidate_counts_before_nms"][str(c_th)] = int(np.sum(mask))
        
    # Top 20 raw candidate confidences
    top_indices = np.argsort(scores)[::-1][:20]
    results["top_20_raw_candidates"] = [
        {
            "index": int(idx),
            "score": float(round(scores[idx], 4)),
            "box_xyxy": [int(round(coord)) for coord in boxes_xyxy[idx]],
            "box_w_h": [int(round(boxes_xywh[idx, 2])), int(round(boxes_xywh[idx, 3]))]
        }
        for idx in top_indices if scores[idx] >= 0.01
    ]
    
    # 2. Grid of Conf Thresh vs NMS IoU Thresh
    results["grid_results"] = {}
    for c_th in conf_thresholds:
        results["grid_results"][str(c_th)] = {}
        # Filter before NMS
        mask = scores >= c_th
        cand_indices = np.where(mask)[0]
        if len(cand_indices) == 0:
            for iou_th in iou_thresholds:
                results["grid_results"][str(c_th)][str(iou_th)] = {"count": 0, "boxes": []}
            continue
            
        c_boxes_xywh = boxes_xywh[cand_indices]
        c_scores = scores[cand_indices]
        
        # Format for cv2.dnn.NMSBoxes: [int(x), int(y), int(w), int(h)]
        nms_input_boxes = [[int(round(b[0])), int(round(b[1])), int(round(b[2])), int(round(b[3]))] for b in c_boxes_xywh]
        nms_input_scores = [float(s) for s in c_scores]
        
        for iou_th in iou_thresholds:
            indices = cv2.dnn.NMSBoxes(nms_input_boxes, nms_input_scores, score_threshold=c_th, nms_threshold=iou_th)
            final_boxes = []
            if len(indices) > 0:
                flat_indices = indices.flatten() if hasattr(indices, 'flatten') else indices
                for fi in flat_indices:
                    orig_idx = cand_indices[fi]
                    final_boxes.append({
                        "confidence": float(round(scores[orig_idx], 4)),
                        "box_xyxy": [int(round(c)) for c in boxes_xyxy[orig_idx]],
                        "box_wh": [int(round(boxes_xywh[orig_idx, 2])), int(round(boxes_xywh[orig_idx, 3]))],
                        "area_ratio": float(round((boxes_xywh[orig_idx, 2] * boxes_xywh[orig_idx, 3]) / (orig_w * orig_h), 6))
                    })
            results["grid_results"][str(c_th)][str(iou_th)] = {
                "count": len(final_boxes),
                "boxes": sorted(final_boxes, key=lambda b: b["confidence"], reverse=True)
            }
            
    return results

def generate_diagnostic_visualizations(raw_data, image_name, output_dir):
    os.makedirs(output_dir, exist_ok=True)
    img = raw_data["img"]
    scores = raw_data["scores"]
    boxes_xyxy = raw_data["boxes_xyxy"]
    boxes_xywh = raw_data["boxes_xywh"]
    
    # 1. Image showing ALL raw candidates at conf >= 0.01 (Raw Model Predictions)
    raw_img = img.copy()
    cand_001_idx = np.where(scores >= 0.01)[0]
    # Sort ascending so highest scores are drawn on top
    sorted_cand = cand_001_idx[np.argsort(scores[cand_001_idx])]
    
    for idx in sorted_cand:
        conf = scores[idx]
        x1, y1, x2, y2 = [int(round(c)) for c in boxes_xyxy[idx]]
        # Color gradient: blue (low 0.01) to yellow (0.15) to red (0.5+)
        if conf < 0.10:
            color = (255, 120, 0) # Blueish/cyan in BGR
            thickness = 1
        elif conf < 0.25:
            color = (0, 200, 255) # Yellow/Orange
            thickness = 2
        else:
            color = (0, 0, 255) # Bright Red
            thickness = 2
            
        cv2.rectangle(raw_img, (x1, y1), (x2, y2), color, thickness)
        if conf >= 0.05: # Only label boxes >= 0.05 to avoid text flood
            label = f"{conf:.2f}"
            cv2.putText(raw_img, label, (x1 + 2, max(12, y1 - 2)), cv2.FONT_HERSHEY_SIMPLEX, 0.4, color, 1, cv2.LINE_AA)
            
    raw_out_path = os.path.join(output_dir, f"{image_name}_raw_candidates_0.01.jpg")
    cv2.imwrite(raw_out_path, raw_img)
    
    # 2. Image showing final boxes after NMS at default production settings (conf=0.25, iou=0.45)
    cand_025_idx = np.where(scores >= 0.25)[0]
    final_img_025 = img.copy()
    if len(cand_025_idx) > 0:
        nms_boxes = [[int(round(b[0])), int(round(b[1])), int(round(b[2])), int(round(b[3]))] for b in boxes_xywh[cand_025_idx]]
        nms_scores = [float(s) for s in scores[cand_025_idx]]
        indices = cv2.dnn.NMSBoxes(nms_boxes, nms_scores, score_threshold=0.25, nms_threshold=0.45)
        if len(indices) > 0:
            flat_indices = indices.flatten() if hasattr(indices, 'flatten') else indices
            for fi in flat_indices:
                orig_idx = cand_025_idx[fi]
                conf = scores[orig_idx]
                x1, y1, x2, y2 = [int(round(c)) for c in boxes_xyxy[orig_idx]]
                cv2.rectangle(final_img_025, (x1, y1), (x2, y2), (0, 0, 235), 3)
                label = f"Pothole: {conf:.2f}"
                cv2.putText(final_img_025, label, (x1 + 4, max(18, y1 - 6)), cv2.FONT_HERSHEY_SIMPLEX, 0.55, (0, 0, 235), 2, cv2.LINE_AA)
    final_out_path = os.path.join(output_dir, f"{image_name}_final_nms_0.25.jpg")
    cv2.imwrite(final_out_path, final_img_025)
    
    # 3. Image showing NMS at lower threshold (conf=0.10, iou=0.45) for diagnostic comparison
    cand_010_idx = np.where(scores >= 0.10)[0]
    final_img_010 = img.copy()
    if len(cand_010_idx) > 0:
        nms_boxes = [[int(round(b[0])), int(round(b[1])), int(round(b[2])), int(round(b[3]))] for b in boxes_xywh[cand_010_idx]]
        nms_scores = [float(s) for s in scores[cand_010_idx]]
        indices = cv2.dnn.NMSBoxes(nms_boxes, nms_scores, score_threshold=0.10, nms_threshold=0.45)
        if len(indices) > 0:
            flat_indices = indices.flatten() if hasattr(indices, 'flatten') else indices
            for fi in flat_indices:
                orig_idx = cand_010_idx[fi]
                conf = scores[orig_idx]
                x1, y1, x2, y2 = [int(round(c)) for c in boxes_xyxy[orig_idx]]
                cv2.rectangle(final_img_010, (x1, y1), (x2, y2), (0, 215, 255), 3) # Amber
                label = f"Pothole: {conf:.2f}"
                cv2.putText(final_img_010, label, (x1 + 4, max(18, y1 - 6)), cv2.FONT_HERSHEY_SIMPLEX, 0.55, (0, 215, 255), 2, cv2.LINE_AA)
    diag_out_path = os.path.join(output_dir, f"{image_name}_diagnostic_nms_0.10.jpg")
    cv2.imwrite(diag_out_path, final_img_010)

    return {
        "raw_candidates_img": raw_out_path,
        "final_025_img": final_out_path,
        "diag_010_img": diag_out_path
    }

def main():
    print("Initializing Multi-Pothole Recall Investigation...")
    session = load_session()
    
    test_images = [
        {"path": "/app/test-data/evaluation/potholes/pothole_rdd_341.jpg", "name": "pothole_rdd_341", "human_count": 3, "description": "RDD India scene with 3 visible potholes (foreground large, middle right, far background left)"},
        {"path": "/app/test-data/evaluation/potholes/pothole_rdd_315.jpg", "name": "pothole_rdd_315", "human_count": 2, "description": "RDD multi-defect scene with 2 distinct pothole clusters"},
        {"path": "/app/test-data/evaluation/potholes/pothole_rdd_298.jpg", "name": "pothole_rdd_298", "human_count": 4, "description": "RDD severe degradation scene with 4 visible road surface depressions"},
        {"path": "/app/test-data/evaluation/potholes/pothole_road_test.jpg", "name": "pothole_road_test", "human_count": 2, "description": "Two adjacent asphalt potholes with varied shadow contrast"},
        {"path": "/app/test-data/evaluation/potholes/pothole_road_test2.jpg", "name": "pothole_road_test2", "human_count": 3, "description": "Multi-cluster asphalt pothole array across roadway"},
        {"path": "/app/test-data/evaluation/potholes/istockphoto-502561495-612x612.jpg", "name": "istockphoto_multi", "human_count": 2, "description": "Stock asphalt road photograph with prominent foreground pothole and secondary background defect"},
        # Clean road controls
        {"path": "/app/test-data/evaluation/clean_roads/clean_road_highway.jpg", "name": "control_clean_highway", "human_count": 0, "description": "Clean highway control"},
        {"path": "/app/test-data/evaluation/clean_roads/clean_road_urban.jpg", "name": "control_clean_urban", "human_count": 0, "description": "Clean urban road control"}
    ]
    
    output_dir = "/app/test-data/evaluation/diagnostics"
    os.makedirs(output_dir, exist_ok=True)
    
    all_report = {}
    
    for item in test_images:
        path = item["path"]
        name = item["name"]
        print(f"\nProcessing {name} (Human count: {item['human_count']})...")
        if not os.path.exists(path):
            print(f"Warning: File {path} not found.")
            continue
            
        raw_data = run_raw_inference(session, path)
        eval_res = evaluate_nms_and_conf(raw_data)
        vis_res = generate_diagnostic_visualizations(raw_data, name, output_dir)
        
        all_report[name] = {
            "metadata": item,
            "image_dims": {"width": raw_data["orig_w"], "height": raw_data["orig_h"]},
            "preprocessing": {
                "letterbox_scale": raw_data["scale"],
                "pad_x": raw_data["pad_x"],
                "pad_y": raw_data["pad_y"],
                "output_shape": raw_data["output_shape"]
            },
            "evaluation": eval_res,
            "visualizations": vis_res
        }
        
        print(f"  Raw candidates >= 0.01: {eval_res['raw_candidate_counts_before_nms']['0.01']}")
        print(f"  Raw candidates >= 0.10: {eval_res['raw_candidate_counts_before_nms']['0.1']}")
        print(f"  Raw candidates >= 0.25: {eval_res['raw_candidate_counts_before_nms']['0.25']}")
        print(f"  Detections at Conf=0.25, IoU=0.45: {eval_res['grid_results']['0.25']['0.45']['count']}")
        print(f"  Detections at Conf=0.15, IoU=0.45: {eval_res['grid_results']['0.15']['0.45']['count']}")
        print(f"  Detections at Conf=0.10, IoU=0.45: {eval_res['grid_results']['0.1']['0.45']['count']}")
        
    out_json_path = os.path.join(output_dir, "multi_pothole_diagnostic_results.json")
    with open(out_json_path, "w") as f:
        json.dump(all_report, f, indent=2)
    print(f"\nSaved detailed analysis JSON to {out_json_path}")

if __name__ == "__main__":
    main()
