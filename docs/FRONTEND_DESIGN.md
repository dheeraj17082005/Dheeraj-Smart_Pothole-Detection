# RoadGuard AI — Frontend Design & Visual Architecture

## 1. Executive Summary & Design Direction

The **RoadGuard AI** frontend redesign transforms the Smart Pothole Detection & Reporting System from a prototype into an operational **civic telematics & road-safety intelligence platform**.

Inspired by the visual clarity, restraint, and utility of modern Indian mobility platforms (such as **okDriver**), the interface blends:
- **Authority & Utilitarian Precision**: Information-dense dashboard and defect registry tailored for municipal public works departments (e.g., NDMC, Delhi PWD, NHAI) and fleet safety operators.
- **Visual Evidence First**: Prominent annotated bounding-box visual proof, interactive full-screen Lightbox zoom inspection, and raw media access.
- **Geographic Command Intelligence**: Full-viewport PostGIS spatial querying with synchronized severity and lifecycle pins.
- **Asynchronous Dashcam Telematics**: Dedicated batch video progress tracker clearly labeled as *"Asynchronous Dashcam Video Processing"* with Type A track aggregation metrics.

---

## 2. Brand Identity & okDriver-Inspired Rationale

| Dimension | Previous Prototype | Redesigned RoadGuard Platform |
| :--- | :--- | :--- |
| **Identity** | Generic Civic Monitor | **RoadGuard AI // Road Intelligence** |
| **Visual Tone** | Plain white boxed prototype | **Dark-rail telematics + Crisp content surfaces** |
| **Color Palette** | Unrestrained browser blues | **Restrained Cobalt (`#1d4ed8`), Slate (`#0b1120`), and Semantic Severity Accents** |
| **Inspection Result** | Small inline image thumbnail | **High-impact 2-column layout (Large Annotated Evidence Left, 4-Metric Grid Right)** |
| **Navigation** | Horizontal text links | **Persistent Left Navigation Rail (Desktop) + Responsive Mobile Drawer** |
| **Video Workflow** | Generic upload | **Dashcam Inspection Telematics with Live 1s Polling & Frame Sampling Metrics** |

---

## 3. Color System & Semantic Tokens

```
Surface Base:          #f8fafc (Subtle Slate-50)
Surface Cards:         #ffffff (Pure White)
Navigation Rail:       #0b1120 (Deep Telematics Slate)
Navigation Surface:    #131b2e (Sub-card Slate)
Primary Brand:         #1d4ed8 (Road Safety Cobalt Blue)
Primary Hover:         #1e40af (Deep Blue)
Accent:                #0ea5e9 (Cyan Telematics Glow)

Severity Low:          #059669 (Emerald) | Bg: #ecfdf5 | Border: #a7f3d0
Severity Medium:       #d97706 (Amber)   | Bg: #fffbeb | Border: #fde68a
Severity High:         #dc2626 (Crimson) | Bg: #fef2f2 | Border: #fecaca

Status Reported:       #fee2e2 (Crimson Alert)
Status Acknowledged:   #fef3c7 (Amber Investigation)
Status In Progress:    #e0e7ff (Indigo Repair Crews)
Status Resolved:       #dcfce7 (Green Remediated)
```

---

## 4. Typography & Information Hierarchy

- **Primary Font**: `Plus Jakarta Sans`, `-apple-system`, `Segoe UI`, `Roboto`, `sans-serif`
- **Telemetry & Coordinate Font**: `JetBrains Mono`, `monospace`

```
Headings (H1):         1.85rem / 29.6px (800 weight, -0.025em letter-spacing)
Card Titles (H2/H3):   1.1rem - 1.25rem (700-800 weight, -0.01em)
Section Badges:        0.68rem - 0.75rem (700 weight, uppercase, +0.06em)
Numerical KPIs:        1.75rem - 1.85rem (800 weight, high contrast)
Metadata / Timestamps: 0.75rem - 0.8125rem (500-600 weight, Slate-500)
```

---

## 5. Component Structure & Workflows

### 5.1 App Shell (`src/components/layout/`)
- **`Sidebar.tsx`**: Left navigation rail containing brand mark, Operations group (`Dashboard`, `Inspect Road`, `Potholes`, `Live Map`), AI Telematics badge (`YOLOv8s-RDD ONNX`), and live system beacon.
- **`Header.tsx`**: Top contextual bar displaying the active route name, "Inspect Road" quick-action button, live connection badge (`System Active`), and operator badge (`Inspector Rajesh`).

### 5.2 Operational Dashboard (`src/pages/DashboardPage.tsx`)
- **Hero Title**: *"Road Intelligence — Monitor, detect and resolve road hazards."*
- **KPI Grid (`StatCards.tsx`)**: 6 cards for Total Potholes, Reported (New), Acknowledged, In Progress, Resolved, and High Severity.
- **Priority Action Banner**: High-contrast dark banner highlighting urgent high-severity defects with instant filter shortcuts.
- **Interactive Viewport Map**: Embedded live map preview updating via PostGIS bounding box queries.
- **Recent Inspections**: Chronological defect log with direct navigation.

### 5.3 Inspect Road Condition (`src/pages/UploadPage.tsx`)
- **Mode Switch**: Toggle between `Image Inspection` and `Video Stream (Dashcam)`.
- **Drag & Dropzone (`FileDropzone.tsx`)**: Pre-upload thumbnail preview, file size/format verification, and clear actions.
- **Location Picker (`LocationPicker.tsx`)**: Geolocation auto-detect (`📍 Use My Location`), manual coordinate validation, and street address text.
- **Domain Guidance Callout**: Advises users on road-facing perspective imagery for optimal YOLOv8s-RDD inference.
- **Detection Result (`ImageDetectionResult.tsx`)**:
  - **Left**: Click-to-zoom annotated visual evidence + bounding box coordinates table `[xmin, ymin, xmax, ymax]`.
  - **Right**: 4-KPI metric grid (Potholes count, Highest confidence, Visual severity, Duplicate status), assigned civic authority, coordinates, and "View in Registry" action.
  - **Reassurance State**: Clean road feedback when zero potholes are detected.

### 5.4 Defect Detail & Lifecycle Audit (`src/pages/PotholeDetailPage.tsx`)
- **Defect Header**: `#XXXXXXXX` with real-time lifecycle status badge and status transition trigger.
- **Lifecycle Progress Indicator (`StatusTimeline.tsx`)**: Visual 4-step progress track (`REPORTED` → `ACKNOWLEDGED` → `IN_PROGRESS` → `RESOLVED`) and chronological history log.
- **Evidence Inspection**: Lightbox full-resolution modal + raw media download.
- **Civic Authority & Simulated Dispatch**: External ticket reference, idempotency key, retry logs.
- **Status Update Modal (`StatusUpdateModal.tsx`)**: Enforces state machine rules and records operator audit notes.

### 5.5 Geographic Operations Map (`src/pages/MapPage.tsx` & `PotholeMap.tsx`)
- Full-height interactive Leaflet viewport map querying PostGIS spatial bounds.
- Severity pins with accessible color codes (`H` High, `M` Medium, `L` Low) and opacity indicating resolved status.
- Viewport scanning radar indicator and map legend.

---

## 6. Responsive Strategy

- **1440px+ (Desktop Monitor)**: Fixed 260px left navigation rail, 2-column evidence & telemetry grid, full-height command map.
- **1024px (Tablet Landscape)**: Collapsed responsive sidebar drawer, adaptive 2-column layouts.
- **768px (Tablet Portrait)**: Single-column stacked cards, full-width data tables with horizontal scroll containers.
- **390px (Mobile)**: Hamburger menu toggle, full-width touch-friendly buttons, condensed badge pills. Zero horizontal viewport overflow.

---

## 7. Accessibility (a11y) & Usability Standards

1. **Non-Color Exclusivity**: Badges combine shape icons (`▲`, `■`, `●`), text labels, and color indicators so colorblind operators can instantly distinguish severity.
2. **Keyboard Trapping & Escape**: Modals and lightboxes trap focus and close on `Escape` key.
3. **Form Error States**: Coordinates out-of-range display specific inline warning text.
4. **Live Scanning Feedback**: Map and video components indicate background polling rates and execution statuses without blocking UI interactions.
