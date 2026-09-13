# Severity Model & Visual Heuristics

This document describes the visual severity model used by the **Smart Pothole Detection and Reporting System** to quantify road defect urgency from computer vision inferences.

---

## 1. Metric Distinctions

The system strictly decouples model certainty from defect severity:

| Metric | Symbol | Range | Description |
|---|---|---|---|
| **Model Confidence** | $C$ | $[0.0, 1.0]$ | Statistical probability from the YOLOv8 object detection model indicating certainty that a detected feature is a pothole. |
| **Visual Area Ratio** | $R$ | $[0.0, 1.0]$ | Proportion of the 2D camera image frame occupied by the pothole bounding box. |
| **Severity Score** | $S$ | $[0.0, 100.0]$ | Continuous numeric score quantifying relative optical footprint weighted by detection confidence. |
| **Severity Class** | Enum | `LOW`, `MEDIUM`, `HIGH` | Discrete triage category for prioritization in municipal dispatch. |

---

## 2. Mathematical Formulation

### 2.1 Visual Area Ratio ($R$)
Calculated from the bounding box pixel dimensions relative to the original uncropped image resolution:

$$R = \frac{\text{Bounding Box Width} \times \text{Bounding Box Height}}{\text{Image Frame Width} \times \text{Image Frame Height}} = \frac{(x_{\max} - x_{\min}) \times (y_{\max} - y_{\min})}{W \times H}$$

### 2.2 Single Detection Severity Score ($S$)
Weighted product of visual footprint, scale multiplier ($1000$), and model confidence, capped at $100.0$:

$$S = \min\left(100.0, \; R \times 1000.0 \times C\right)$$

*Example*:
- Bounding box occupies $4.2\%$ of frame ($R = 0.042$) with confidence $C = 0.885$:
  $$S = \min(100.0, \; 0.042 \times 1000 \times 0.885) = 37.17$$

---

## 3. Classification Thresholds

Discrete thresholding maps the continuous severity score to business priority levels:

- **`LOW`**: $S < 20.0$
  - Small surface fissures or minor shallow potholes occupying $< 2\%$ of the image frame.
- **`MEDIUM`**: $20.0 \le S < 50.0$
  - Moderate road defects occupying $2\% - 5\%$ of the image frame requiring scheduled road maintenance.
- **`HIGH`**: $S \ge 50.0$
  - Severe road craters occupying $> 5\%$ of the image frame posing imminent traffic disruption.

---

## 4. Multiple-Pothole Aggregation Rule

When multiple potholes appear within a single image or video aggregation window:

1. **Individual Contributions**:
   Each detection retains its own bounding box, confidence $C_i$, visual area ratio $R_i$, and individual score $S_i = \min(100.0, R_i \times 1000 \times C_i)$.

2. **Aggregate Severity Score**:
   $$S_{\text{agg}} = \min\left(100.0, \; \sum_{i=1}^{k} S_i\right)$$

3. **Cluster Severity Escalation**:
   - If the number of potholes in the cluster is **$\ge 3$**, the overall classification is escalated to **`HIGH`**, regardless of individual sizes. A cluster of multiple potholes represents extensive structural pavement failure requiring rapid intervention.
   - If count $< 3$, the classification follows standard score thresholds on $S_{\text{agg}}$.

---

## 5. Explicit Limitations & Boundaries

> [!IMPORTANT]
> **2D Optical Heuristic Limitation**:
> - The severity score and classification are **2D camera perspective optical heuristics**.
> - They quantify the optical area of dark damaged asphalt within the camera's field of view.
> - They do **NOT** estimate:
>   - True 3D physical pothole depth or volume (mm / cm)
>   - Sub-surface structural pavement deterioration
>   - Vehicle suspension impact or tire damage risk
>   - Wet road reflections or depth of water inside flooded potholes.
