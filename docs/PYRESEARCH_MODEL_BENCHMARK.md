# Offline Model Benchmark: PyResearch `best.pt` vs Production `peterhdd`

## 1. Executive Summary

This document presents the empirical benchmark comparing the experimental `best.pt` checkpoint from the [PyResearch Pothole Computer Vision Project](https://github.com/pyresearch/Pothole-Computer-Vision-Project) against our current production detector (`peterhdd/pothole-detection-yolov8`).

**Final Recommendation**: **Decision A — Keep Current Production Model (`peterhdd/pothole-detection-yolov8`)**.

PyResearch `best.pt` exhibits lower accuracy, severe duplicate bounding box hallucinations covering entire road frames, a high-confidence false positive on clean highway pavement (56.3%), and compatibility issues due to an unreleased YOLOv12 draft architecture.

---

## 2. Model Metadata & Compatibility Inspection

### PyResearch `best.pt` Details:
- **Repository Source**: `https://github.com/pyresearch/Pothole-Computer-Vision-Project`
- **Experimental File Location**: `ai-service/models/experimental/pyresearch_best.pt`
- **File Size**: `18,632,867 bytes (17.77 MB)`
- **SHA-256 Checksum**: `4a4aa7e6a3f2f9d51873b933d48fe86e269e97323f14efdf15452ce559e58806`
- **Model Format**: PyTorch Checkpoint (`.pt`)
- **YOLO Generation / Architecture**: `YOLOv12s` (`yolov12s.yaml` draft with `A2C2f` and `AAttn` attention blocks)
- **Class Count**: `1`
- **Class Mapping**: `{0: 'Pothole'}`
- **Discovered Training Source**: Roboflow Pothole Dataset (`/content/Pothole-1/data.yaml`)
- **Discovered Training Parameters**: 30 epochs, batch size 16, imgsz 640, mosaic 1.0, erasing 0.4.
- **Compatibility Issues**: Standard Ultralytics engine throws `AttributeError: 'AAttn' object has no attribute 'qkv'` because the checkpoint was trained on a custom YOLOv12 fork where projection layers (`qk` and `v`) were separated. Standard ONNX export fails without custom tensor projection monkey-patching.

---

## 3. Head-to-Head Benchmark Evaluation

Evaluation performed across the standardized test dataset at `CONFIDENCE_THRESHOLD = 0.15` and `NMS_IOU_THRESHOLD = 0.45`:

| Image Identifier | Native Resolution | Ground Truth | Current Model (`peterhdd`) Count | PyResearch (`best.pt`) Count | Current Model Max Conf | PyResearch Max Conf | Clean-Road False Positives |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| `istockphoto-502561495` | 612 × 408 | 2 – 4 | **2** (clean boxes) | **4** (hallucinated overlap) | **0.8559** | 0.6788 | 0 |
| `pothole_rdd_298.jpg` | 449 × 300 | 4 | **3** | **6** (fragmented) | **0.8164** | 0.3220 | 0 |
| `pothole_rdd_315.jpg` | 450 × 300 | 2 | **3** | **2** | **0.9174** | 0.8021 | 0 |
| `pothole_rdd_341.jpg` | 453 × 300 | 3 | **3** | **4** | **0.6711** | 0.5361 | 0 |
| `pothole_road_test.jpg` | 480 × 480 | 2 | 0 | 0 | < 0.04 | < 0.04 | 0 |
| `pothole_road_test2.jpg` | 480 × 480 | 3 | 0 | 0 | < 0.04 | < 0.04 | 0 |
| `clean_road_highway.jpg` | 640 × 640 | 0 | **0** | **1 (FALSE POSITIVE)** | 0.0000 | **0.5630** | **PyResearch FP** |
| `clean_road_urban.jpg` | 640 × 1138 | 0 | **0** | **0** | 0.0000 | 0.0000 | 0 |

---

## 4. Confidence & NMS Sweeps for PyResearch `best.pt`

### 4.1 Confidence Threshold Sweep (IoU = 0.45)
| Image | Conf = 0.10 | Conf = 0.15 | Conf = 0.20 | Conf = 0.25 | Conf = 0.30 | Conf = 0.35 |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| `istockphoto-502561495` | 6 | 4 | 3 | 3 | 3 | 2 |
| `pothole_rdd_298.jpg` | 9 | 6 | 6 | 3 | 1 | 0 |
| `pothole_rdd_315.jpg` | 5 | 2 | 2 | 2 | 2 | 2 |
| `pothole_rdd_341.jpg` | 5 | 4 | 3 | 3 | 3 | 2 |
| `clean_road_highway.jpg` | **1** | **1** | **1** | **1** | **1** | **1** |
| `clean_road_urban.jpg` | 0 | 0 | 0 | 0 | 0 | 0 |

*Critical Observation*: On `clean_road_highway.jpg`, PyResearch `best.pt` predicts a false positive pothole at **56.3% confidence**. Even at `conf = 0.35`, the false positive persists.

---

## 5. Visual Artifact Comparison on `istockphoto-502561495-612x612.jpg`

Side-by-side diagnostic overlays generated at `docs/diagnostics/`:

- **Current Production Model (`peterhdd`)**:
  - Box 1: `[0, 198, 386, 376]` (Confidence: `0.8559`) — Tightly bounds the foreground left pothole.
  - Box 2: `[505, 55, 612, 124]` (Confidence: `0.4926`) — Tightly bounds the top right background pothole.
- **PyResearch Model (`best.pt`)**:
  - Box 1: `[0, 160, 414, 408]` (Confidence: `0.6788`) — Foreground left pothole.
  - Box 2: `[69, 80, 599, 404]` (Confidence: `0.6470`) — **Giant duplicate box** encompassing almost the entire road surface, engulfing Box 1.
  - Box 3: `[149, 37, 398, 164]` (Confidence: `0.3473`) — Overlapping asphalt shadow patch.
  - Box 4: `[352, 119, 499, 217]` (Confidence: `0.1992`) — Overlapping road texture artifact.

---

## 6. CPU Inference Latency Comparison

- **Current Production Model (`peterhdd`) ONNX Engine**: **$190 - 230\text{ ms}$** average per frame.
- **PyResearch Model (`best.pt`) PyTorch Engine**: **$270 - 1,100\text{ ms}$** average per frame.

---

## 7. Strategic Decision & Conclusion

**Decision A: Keep Current Production Model (`peterhdd/pothole-detection-yolov8`)**.

1. **Precision & Safety**: Current model maintains **0 false positives** on clean asphalt controls, whereas PyResearch `best.pt` triggers high-confidence false detections (56.3%) on clean highways.
2. **Bounding Box Quality**: PyResearch `best.pt` produces giant bounding boxes wrapping entire roadways rather than isolating discrete potholes.
3. **Production Stability**: Current ONNX model executes deterministically across CPU environments without custom Python runtime patches or unreleased YOLOv12 architecture dependencies.
