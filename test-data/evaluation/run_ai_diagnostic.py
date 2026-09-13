import os
import sys
import io
import mimetypes
import json
import urllib.request
from PIL import Image
import onnxruntime as ort

sys.path.insert(0, os.path.abspath('ai-service'))
from app.image_processor import run_image_inference

THRESHOLDS = [0.10, 0.15, 0.20, 0.25, 0.30, 0.35]
BASE_URL = 'http://localhost:8000'

session = ort.InferenceSession('ai-service/models/pothole_yolov8.onnx')
input_name = session.get_inputs()[0].name

pothole_files = sorted([f for f in os.listdir('test-data/evaluation/potholes') if not f.startswith('.')])
clean_files = sorted([f for f in os.listdir('test-data/evaluation/clean_roads') if not f.startswith('.')])

print(f"Loaded {len(pothole_files)} pothole images and {len(clean_files)} clean road images.", flush=True)

def send_to_fastapi(file_path, conf):
    boundary = '----WebKitFormBoundary7MA4YWxkTrZu0gW'
    filename = os.path.basename(file_path)
    mime = mimetypes.guess_type(file_path)[0] or 'application/octet-stream'
    
    with open(file_path, 'rb') as f:
        file_bytes = f.read()
        
    body = bytearray()
    body.extend(f'--{boundary}\r\n'.encode('utf-8'))
    body.extend(f'Content-Disposition: form-data; name="file"; filename="{filename}"\r\n'.encode('utf-8'))
    body.extend(f'Content-Type: {mime}\r\n\r\n'.encode('utf-8'))
    body.extend(file_bytes)
    body.extend(b'\r\n')
    body.extend(f'--{boundary}--\r\n'.encode('utf-8'))
    
    req = urllib.request.Request(f'{BASE_URL}/detect/image?confidence_threshold={conf}', data=body)
    req.add_header('Content-Type', f'multipart/form-data; boundary={boundary}')
    
    with urllib.request.urlopen(req, timeout=10) as resp:
        return json.loads(resp.read().decode('utf-8'))

# 1. Test Pothole Images
results_potholes = []
for pf in pothole_files:
    fpath = os.path.join('test-data/evaluation/potholes', pf)
    with Image.open(fpath) as im:
        w, h = im.size
        fmt = im.format
    
    with open(fpath, 'rb') as f:
        raw_res, _ = run_image_inference(f.read(), session, input_name=input_name, conf_threshold=0.01)
    
    t_counts = {}
    t_boxes = {}
    for t in THRESHOLDS:
        api_res = send_to_fastapi(fpath, t)
        t_counts[t] = api_res['pothole_count']
        t_boxes[t] = api_res['detections']
        
    results_potholes.append({
        'filename': pf,
        'format': fmt,
        'size': f'{w}x{h}',
        'raw_max_conf': round(raw_res.max_confidence, 4) if raw_res.pothole_count > 0 else 0.0,
        'raw_count_001': raw_res.pothole_count,
        'threshold_counts': t_counts,
        'detections_020': t_boxes[0.20]
    })
    print(f"Processed pothole image {pf}: raw_max_conf={results_potholes[-1]['raw_max_conf']}, 0.20_count={t_counts[0.20]}", flush=True)

# 2. Test Clean Road Controls
results_clean = []
for cf in clean_files:
    fpath = os.path.join('test-data/evaluation/clean_roads', cf)
    with Image.open(fpath) as im:
        w, h = im.size
        fmt = im.format
        
    with open(fpath, 'rb') as f:
        raw_res, _ = run_image_inference(f.read(), session, input_name=input_name, conf_threshold=0.01)
        
    t_counts = {}
    for t in THRESHOLDS:
        api_res = send_to_fastapi(fpath, t)
        t_counts[t] = api_res['pothole_count']
        
    results_clean.append({
        'filename': cf,
        'format': fmt,
        'size': f'{w}x{h}',
        'raw_max_conf': round(raw_res.max_confidence, 4) if raw_res.pothole_count > 0 else 0.0,
        'threshold_counts': t_counts
    })
    print(f"Processed clean road {cf}: raw_max_conf={results_clean[-1]['raw_max_conf']}, 0.20_count={t_counts[0.20]}", flush=True)

output_data = {'potholes': results_potholes, 'clean': results_clean}
with open('test-data/evaluation/evaluation_results.json', 'w') as f:
    json.dump(output_data, f, indent=2)

print("EVALUATION COMPLETE. Saved to test-data/evaluation/evaluation_results.json", flush=True)
