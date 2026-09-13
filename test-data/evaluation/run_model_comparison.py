import os
import io
import json
import cv2
import numpy as np
from PIL import Image
import onnxruntime as ort

THRESHOLDS = [0.10, 0.15, 0.20, 0.25, 0.30, 0.35, 0.40]

def letterbox_image(image_bgr, target_size=(640, 640), fill_color=(114, 114, 114)):
    orig_h, orig_w = image_bgr.shape[:2]
    target_w, target_h = target_size
    scale = min(target_w / orig_w, target_h / orig_h)
    new_w = int(round(orig_w * scale))
    new_h = int(round(orig_h * scale))
    resized = cv2.resize(image_bgr, (new_w, new_h), interpolation=cv2.INTER_LINEAR)
    
    pad_w = (target_w - new_w) / 2
    pad_h = (target_h - new_h) / 2
    top = int(round(pad_h - 0.1))
    bottom = int(round(pad_h + 0.1))
    left = int(round(pad_w - 0.1))
    right = int(round(pad_w + 0.1))
    
    padded = cv2.copyMakeBorder(resized, top, bottom, left, right, cv2.BORDER_CONSTANT, value=fill_color)
    return padded, scale, pad_w, pad_h

def postprocess_yolo_detections(output, scale, pad_w, pad_h, orig_w, orig_h, pothole_class_idx, conf_threshold, iou_threshold=0.45):
    # output is (1, channels, anchors) e.g. (1, 9, 8400) or (1, 5, 8400) or (1, 6, 2100)
    preds = output[0].T # (anchors, channels)
    
    boxes = []
    confidences = []
    
    for row in preds:
        cx, cy, w, h = row[0], row[1], row[2], row[3]
        conf = float(row[4 + pothole_class_idx])
        if conf >= conf_threshold:
            # un-pad and un-scale
            unpad_x = (cx - pad_w) / scale
            unpad_y = (cy - pad_h) / scale
            unpad_w = w / scale
            unpad_h = h / scale
            
            xmin = max(0, min(orig_w - 1, int(round(unpad_x - unpad_w / 2))))
            ymin = max(0, min(orig_h - 1, int(round(unpad_y - unpad_h / 2))))
            xmax = max(0, min(orig_w, int(round(unpad_x + unpad_w / 2))))
            ymax = max(0, min(orig_h, int(round(unpad_y + unpad_h / 2))))
            
            if xmax > xmin and ymax > ymin:
                boxes.append([xmin, ymin, xmax - xmin, ymax - ymin])
                confidences.append(conf)
                
    if not boxes:
        return []
        
    indices = cv2.dnn.NMSBoxes(boxes, confidences, conf_threshold, iou_threshold)
    results = []
    if len(indices) > 0:
        for idx in indices.flatten():
            bx, by, bw, bh = boxes[idx]
            results.append({
                'box': [bx, by, bx + bw, by + bh],
                'confidence': round(confidences[idx], 4),
                'area_ratio': (bw * bh) / (orig_w * orig_h)
            })
    return results

class ModelRunner:
    def __init__(self, name, model_path, input_size, pothole_class_idx):
        self.name = name
        self.session = ort.InferenceSession(model_path)
        self.input_name = self.session.get_inputs()[0].name
        self.input_size = input_size
        self.pothole_class_idx = pothole_class_idx
        
    def predict(self, image_path, conf_threshold):
        with open(image_path, 'rb') as f:
            file_bytes = f.read()
        nparr = np.frombuffer(file_bytes, np.uint8)
        img_bgr = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        orig_h, orig_w = img_bgr.shape[:2]
        
        img_padded, scale, pad_w, pad_h = letterbox_image(img_bgr, target_size=self.input_size)
        img_rgb = cv2.cvtColor(img_padded, cv2.COLOR_BGR2RGB)
        tensor = img_rgb.astype(np.float32) / 255.0
        tensor = np.transpose(tensor, (2, 0, 1))
        tensor = np.expand_dims(tensor, axis=0)
        
        outputs = self.session.run(None, {self.input_name: tensor})
        dets = postprocess_yolo_detections(
            outputs[0], scale, pad_w, pad_h, orig_w, orig_h,
            self.pothole_class_idx, conf_threshold
        )
        return dets, orig_w, orig_h

# Load candidate models
candidates = [
    ModelRunner('vinothvikas1987/pothole-detection-yolov8', 'models/candidates/vinothvikas.onnx', (640, 640), 3),
    ModelRunner('peterhdd/pothole-detection-yolov8', 'models/candidates/peterhdd.onnx', (640, 640), 0),
    ModelRunner('tahaUgan/pothole-yolo11n', 'models/candidates/tahaUgan.onnx', (320, 320), 0),
    ModelRunner('subhodeepmoitra/pothole-detection-yolov8', 'models/candidates/subhodeepmoitra.onnx', (640, 640), 0)
]

# Define test datasets
in_domain_potholes = [
    'pothole_rdd_341.jpg',
    'pothole_rdd_315.jpg',
    'pothole_rdd_298.jpg',
    'pothole_rdd_293.jpg',
    'pothole_rdd_193.jpg',
    'pothole_existing_sample.jpg'
]

ood_user_potholes = [
    'istockphoto-502561495-612x612.jpg',
    'pothole_road_test.jpg',
    'pothole_road_test2.jpg'
]

clean_roads = [
    'clean_road_standard.jpg',
    'clean_road_highway.jpg',
    'clean_road_paved.jpg',
    'clean_road_concrete.jpg',
    'clean_road_urban.jpg'
]

all_potholes = in_domain_potholes + ood_user_potholes

print("=== STARTING MODEL BENCHMARK SWEEP ===\n")
benchmark_results = {}

for model in candidates:
    print(f"Evaluating {model.name}...")
    model_data = {
        'threshold_metrics': {},
        'image_details': {},
        'user_problem_images': {}
    }
    
    # 1. Sweep thresholds across all test images
    for t in THRESHOLDS:
        tp_in = 0
        tp_ood = 0
        fn_in = 0
        fn_ood = 0
        fp_clean = 0
        conf_sum = 0.0
        det_total = 0
        
        # Test In-domain potholes
        for pf in in_domain_potholes:
            path = os.path.join('test-data/evaluation/potholes', pf)
            dets, _, _ = model.predict(path, t)
            if len(dets) > 0:
                tp_in += 1
                conf_sum += max(d['confidence'] for d in dets)
                det_total += 1
            else:
                fn_in += 1
                
        # Test OOD / User-problem potholes
        for pf in ood_user_potholes:
            path = os.path.join('test-data/evaluation/potholes', pf)
            dets, _, _ = model.predict(path, t)
            if len(dets) > 0:
                tp_ood += 1
                conf_sum += max(d['confidence'] for d in dets)
                det_total += 1
            else:
                fn_ood += 1
                
        # Test Clean roads (negatives)
        for cf in clean_roads:
            path = os.path.join('test-data/evaluation/clean_roads', cf)
            dets, _, _ = model.predict(path, t)
            if len(dets) > 0:
                fp_clean += len(dets)
                
        total_pothole_ground_truth = len(all_potholes) # 9
        total_tp = tp_in + tp_ood
        total_fn = fn_in + fn_ood
        recall = (total_tp / total_pothole_ground_truth) * 100.0
        precision = (total_tp / (total_tp + fp_clean) * 100.0) if (total_tp + fp_clean) > 0 else 0.0
        avg_conf = (conf_sum / det_total) if det_total > 0 else 0.0
        
        model_data['threshold_metrics'][str(t)] = {
            'tp_in_domain': f"{tp_in}/{len(in_domain_potholes)}",
            'tp_ood': f"{tp_ood}/{len(ood_user_potholes)}",
            'total_tp': total_tp,
            'total_fn': total_fn,
            'fp_clean': fp_clean,
            'recall_pct': round(recall, 1),
            'precision_pct': round(precision, 1),
            'avg_conf': round(avg_conf, 4)
        }
        
    # 2. Detailed record for user problem images at t=0.20 and t=0.25
    for img_name in ood_user_potholes:
        path = os.path.join('test-data/evaluation/potholes', img_name)
        dets_020, _, _ = model.predict(path, 0.20)
        dets_025, _, _ = model.predict(path, 0.25)
        dets_raw, _, _ = model.predict(path, 0.01)
        model_data['user_problem_images'][img_name] = {
            'raw_detections': len(dets_raw),
            'raw_max_conf': max([d['confidence'] for d in dets_raw], default=0.0),
            'detected_at_020': len(dets_020) > 0,
            'detected_at_025': len(dets_025) > 0,
            'detections_025': dets_025
        }
        
    benchmark_results[model.name] = model_data

with open('test-data/evaluation/model_comparison_results.json', 'w') as f:
    json.dump(benchmark_results, f, indent=2)

print("\n=== BENCHMARK COMPLETE ===")
print(json.dumps(benchmark_results, indent=2))
