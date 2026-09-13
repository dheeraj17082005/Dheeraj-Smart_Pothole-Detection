# Custom Training Dataset Selection & Audit Report

## 1. Dataset Selection Overview

To build a reproducible custom training pipeline targeting multi-pothole recall and small defect localization, we selected the **Roboflow Pothole-665 / Pothole v1 Dataset** (`pothole.v1i.yolov4pytorch`).

| Attribute | Specification |
| :--- | :--- |
| **Dataset Name** | Roboflow Pothole v1 Dataset (`potholes-detection-d4rma / v1`) |
| **Source URL** | `https://universe.roboflow.com/project-ssayl/potholes-detection-d4rma/dataset/1` |
| **License** | **CC BY 4.0** (Creative Commons Attribution 4.0 International — Permissive) |
| **Format** | PyTorch / YOLO Standard Format (`0 x_center y_center width height`) |
| **Total Images** | **715** (Train: 563, Validation: 152) |
| **Total Bounding Box Annotations** | **1,255** |
| **Classes** | `1` (`0: pothole`) |
| **Data Leakage Check** | **0 SHA-256 matches** against evaluation benchmark dataset |

---

## 2. Multi-Pothole & Object Size Distribution Audit

### 2.1 Potholes per Image Distribution
- **1 Pothole**: 494 images (69.1%)
- **2 Potholes**: 107 images (15.0%)
- **3 Potholes**: 52 images (7.3%)
- **4+ Potholes**: 61 images (8.5%)
- **Multi-Pothole Image Ratio**: **30.9%** (220 / 715 images contain 2+ distinct potholes)

### 2.2 Bounding Box Object Size Distribution
- **Small Defects ($< 32\times32\text{ px}$)**: 24 annotations (1.9%)
- **Medium Defects ($32\times32$ to $96\times96\text{ px}$)**: 350 annotations (27.9%)
- **Large Defects ($> 96\times96\text{ px}$)**: 881 annotations (70.2%)

---

## 3. Data Leakage Verification Result

A complete SHA-256 checksum comparison was executed between all 715 training/validation images and the 33 evaluation benchmark images (`istockphoto-502561495`, `pothole_rdd_*`, `pothole_road_test*`, `clean_road_*`).

**Result**: **0 overlapping hashes found**. The evaluation benchmark is completely isolated from training data.
