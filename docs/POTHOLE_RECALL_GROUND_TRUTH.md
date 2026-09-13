# Pothole Recall Ground-Truth Dataset Specification

## 1. Executive Summary

To scientifically evaluate multi-pothole recall improvements without relying on anecdotal assertions, a ground-truth dataset was established across real-world Indian road images (RDD-style), dashcam imagery, high-resolution photographs, and clean-road negative controls.

Each image has been manually inspected by domain reviewers to identify every visible asphalt surface depression, broken edge cavity, and road defect.

---

## 2. Benchmark Image Catalog & Ground Truth

| Image Filename | Native Resolution | Ground-Truth Pothole Count | Defect Distribution & Visual Descriptions | Difficulty Rating |
| :--- | :--- | :---: | :--- | :--- |
| `istockphoto-502561495-612x612.jpg` | 612 × 408 | **2 – 4** | • **P1 (Foreground Left)**: Deep asphalt cavity `[0, 197, 385, 376]`<br>• **P2 (Upper Right)**: Secondary depression `[505, 54, 611, 123]`<br>• **P3 (Mid-Right)**: Asphalt crater `[423, 191, 611, 301]`<br>• **P4 (Upper Mid)**: Distant road breach `[294, 58, 377, 95]` | High (Varied scale & depth) |
| `pothole_existing_sample.jpg` | 1920 × 1920 | **3 – 4** | • **P1 (Center Left)**: Major longitudinal cavity<br>• **P2 (Center Right)**: Adjacent road void<br>• **P3 (Upper Background)**: Distant crater array | High (High resolution, small distant features) |
| `pothole_rdd_193.jpg` | 720 × 720 | **3** | • **P1 (Foreground)**: Distinct asphalt fissure<br>• **P2 (Mid-ground)**: Severe surface depression<br>• **P3 (Background)**: Secondary pavement loss | Medium |
| `pothole_rdd_293.jpg` | 400 × 300 | **2** | • **P1 (Center Left)**: Clear oval pothole<br>• **P2 (Center Right)**: Connected surface failure | Low – Medium |
| `pothole_rdd_298.jpg` | 449 × 300 | **4** | • **P1 (Bottom Right)**: Primary deep depression<br>• **P2 (Bottom Center)**: Large irregular crater<br>• **P3 (Mid Left)**: Medium cavity<br>• **P4 (Top Left Background)**: Distant surface breach | Very High (Multiple overlapping & distant defects) |
| `pothole_rdd_315.jpg` | 450 × 300 | **2 – 3** | • **P1 (Foreground)**: Prominent road crater<br>• **P2 (Midground Right)**: Secondary depression | Medium |
| `pothole_rdd_341.jpg` | 453 × 300 | **3** | • **P1 (Foreground Right)**: Large distinct cavity<br>• **P2 (Midground Center)**: Medium cavity<br>• **P3 (Far Background Left)**: Distant small pothole | Medium – High |
| `clean_road_highway.jpg` | 640 × 640 | **0** | Clean, pristine multi-lane highway surface. | Negative Control (Clean) |
| `clean_road_urban.jpg` | 640 × 1138 | **0** | Clean urban paved road with standard lane markings. | Negative Control (Clean) |
| `clean_road_standard.jpg` | 640 × 640 | **0** | Clean standard asphalt road with uniform texture. | Negative Control (Clean) |
| `clean_road_paved.jpg` | 640 × 640 | **0** | Clean paved roadway without structural defects. | Negative Control (Clean) |

---

## 3. Failure Mode Taxonomy (Cases A, B, C, D)

When evaluating missed detections against ground truth, every discrepancy is categorized into one of four deterministic failure modes:

### Case A: NMS IoU Collision Suppression
- **Root Cause**: The model predicts multiple overlapping or adjacent candidate boxes with confidence $> 0.25$. However, the NMS IoU threshold suppresses adjacent genuine defect candidates if their bounding boxes overlap by more than the configured IoU threshold.
- **Remedy**: Coordinate-aware soft suppression or tuning IoU threshold to $0.45 - 0.50$.

### Case B: Sub-Threshold Confidence Dropping
- **Root Cause**: The feature detector successfully localizes the pothole region, but generates a raw confidence score between $0.10$ and $0.24$ (below the legacy $0.25$ threshold).
- **Remedy**: Calibrated reduction of confidence threshold to $0.15$, validated against clean-road negative controls.

### Case C: Resolution / Downscaling Attenuation
- **Root Cause**: In high-resolution ($1920\times1920$ or $720\times720$) images, scaling the full image down to the model's static $640\times640$ input tensor shrinks small or distant potholes to $< 15$ pixels, destroying feature activations.
- **Remedy**: High-resolution Tiled / Sliced Inference (SAHI-style) using overlapping crops, preserving native feature resolution.

### Case D: Out-of-Domain / Model Feature Absence
- **Root Cause**: The model produces zero activations ($\text{score} < 0.05$) due to extreme motion blur, heavy shadows, or unlearned surface materials.
- **Remedy**: Requires model fine-tuning (out of scope for inference-only optimization).
