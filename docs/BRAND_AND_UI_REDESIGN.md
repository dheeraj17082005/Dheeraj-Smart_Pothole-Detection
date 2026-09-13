# PotholeX — Brand & UI Redesign System

## 1. Product Identity & Brand Foundation

### Brand Overview
**PotholeX** (*AI Road Intelligence*) is a next-generation civic telematics and automated road-defect detection platform. Designed with inspiration from modern high-performance automotive telematics (e.g. okDriver), the application provides municipal road authorities and operators with real-time visual AI detection, spatial GIS defect management, and automated workflow resolution.

- **Product Name**: PotholeX
- **Subtitle / Tagline**: AI ROAD INTELLIGENCE
- **Application Role**: Road-Condition Telematics & Defect Governance

---

## 2. Logo Integration & Navigation

### Asset Specifications
- **Source**: `a_clean_high_resolution_icon_logo_style_graphic_o.png`
- **Location**: `frontend/public/logo.png`, `frontend/src/assets/logo.png`, and `frontend/public/a_clean_high_resolution_icon_logo_style_graphic_o.png`
- **Visual Description**: A dark shield crest featuring a stylized hexagonal roadway perspective grid crowned by an electric lime-green target bounding mark.

### Universal Home Navigation
The PotholeX logo is implemented across both desktop and mobile layouts as a universal, accessible home navigation button:
- **Desktop Sidebar**: High-contrast logo badge with `PotholeX` header and `AI ROAD INTELLIGENCE` subtitle, wrapped in a clickable link to `/` with `aria-label="PotholeX Home"`.
- **Mobile Top Header**: Compact logo lockup with `PotholeX` text and direct link to `/`.

---

## 3. Design Tokens & Color Palette

The interface is built upon a **Dark Automotive Telematics** foundation (`#0B0D0F`), preventing glare in operational dispatch centers while providing high contrast for visual AI bounding boxes and severity indicators.

### Core Tokens (`frontend/src/index.css`)

| Token | Hex Value | Semantic Usage |
| :--- | :--- | :--- |
| `--bg-app` | `#0B0D0F` | Global canvas background |
| `--surface` | `#111417` | Standard card and container surface |
| `--surface-elevated` | `#171B1F` | Elevated cards, modals, table headers, and dropzones |
| `--surface-hover` | `#1E2328` | Interactive row and button hover states |
| `--border` | `#292F34` | Crisp borders and component outlines |
| `--border-focus` | `#3D464E` | Form field focus and active containers |
| `--brand-accent` | `#D4F63D` | **Logo Lime** accent — Primary CTAs, active nav items, timeline progress |
| `--brand-accent-hover`| `#C2E232` | Primary button hover state |
| `--brand-accent-glow` | `rgba(212, 246, 61, 0.25)` | Focus rings and active indicator glows |
| `--text-main` | `#F1F5F9` | Primary typography and high-contrast labels |
| `--text-muted` | `#94A3B8` | Subtitles, secondary metadata, and table column headers |
| `--text-subtle` | `#64748B` | Timestamp details, microcopy, and footnotes |

### Severity Invariant Palette (Defects & Status)
Defect severity retains universal traffic-control semantic color conventions to maintain unambiguous operational clarity:

- **High Severity**: `#EF4444` (Crimson / Urgent Action)
- **Medium Severity**: `#F59E0B` (Amber / Warning)
- **Low Severity**: `#10B981` (Emerald / Normal Monitor)
- **Information / Telematics**: `#38BDF8` (Sky Blue)

---

## 4. Typography & Visual Hierarchy

- **Font Family**: Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif
- **Data / Coordinates / Confidence Font**: Monospace (`ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace`) for GPS latitudes/longitudes, IDs, and confidence percentages.
- **Section Eyebrows**: Uppercase tracking (`letterSpacing: '0.08em'`, `fontSize: '0.72rem'`, `fontWeight: 800`) in `var(--brand-accent)`.
- **Page Headings**: High-contrast bold headings (`fontSize: '1.75rem'`, `fontWeight: 800`, `letterSpacing: '-0.03em'`).

---

## 5. Domain Status Lifecycle

The system enforces the strict domain and API lifecycle:

$$\text{REPORTED} \longrightarrow \text{ACKNOWLEDGED} \longrightarrow \text{IN\_PROGRESS} \longrightarrow \text{RESOLVED}$$

- **REPORTED**: Defect newly detected by AI vision inference and registered in PostGIS.
- **ACKNOWLEDGED**: Defect reviewed and verified by public works operator.
- **IN PROGRESS**: Remediation crew dispatched to the defect GPS location.
- **RESOLVED**: Physical road repair completed, validated, and logged to audit trail.

---

## 6. Component Architecture & UI Patterns

### 1. Unified Sidebar (`frontend/src/components/layout/Sidebar.tsx`)
- Fixed dark sidebar with brand crest and PotholeX title.
- Navigation links with glowing left active indicator and lime text tinting.
- Bottom **AI Telematics Engine** badge displaying active `peterhdd/pothole-detection-yolov8` ONNX model status.

### 2. Header (`frontend/src/components/layout/Header.tsx`)
- Fixed top bar with dynamic route breadcrumbs.
- Live `System Operational` beacon indicator with animated pulse effect.
- Municipal Operator badge.

### 3. Executive Telematics Dashboard (`frontend/src/pages/DashboardPage.tsx`)
- **6 KPI Telematics Cards**: Total Inspected, Open / Reported, Acknowledged, In Progress, Resolved, High Severity (100% backed by `/api/v1/dashboard/stats`).
- **Urgent Action Banner**: High-priority alert highlighting pending critical defects requiring crew dispatch.
- **Live Defect Stream**: Dark-themed tabular log of recently detected road damage with direct inspection deep links.

### 4. Road Inspection Studio (`frontend/src/pages/UploadPage.tsx`)
- **Dual Mode Switch**: High-contrast toggle for `Single Road Image` and `Dashcam Video (Async)`.
- **Pre-Upload Interactive Preview**: Immediate visual rendering of selected imagery prior to AI submission.
- **GPS Coordinates Input & Geolocation**: Integrated "Detect GPS" button with browser geolocation fallback.
- **Visual Inspection Result Panel**:
  - Side-by-side zoomable visual evidence preview with detected bounding box overlays.
  - Interactive Defect Details table listing confidence score, severity badge, area in pixels, and spatial coordinates.
  - Modal Lightbox (`ImageLightboxModal`) for deep visual auditing.
- **Asynchronous Video Processing Tracker (`VideoJobTracker.tsx`)**:
  - Live polling progress bar (0–100%) with status badge (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`).
  - Explicit notification: *"ASYNC VIDEO PROCESSING: Dashcam video processing runs asynchronously to aggregate frame detections."*

### 5. Spatial GIS Defect Map (`frontend/src/components/map/PotholeMap.tsx`)
- Dark custom Leaflet canvas with inverted/contrasted map tiles.
- Custom severity-colored SVG markers with animated sonar ping rings for high-severity hazards.
- PostGIS bounding box queries on map pan/zoom.
- Rich dark popups with defect thumbnail, severity badge, assigned authority, and deep link to detail view.

### 6. Defect Registry Table (`frontend/src/pages/PotholesListPage.tsx`)
- Multi-parameter filter bar (Severity, Lifecycle Status: `REPORTED`, `ACKNOWLEDGED`, `IN_PROGRESS`, `RESOLVED`, Responsible Municipal Authority).
- Dark table with alternating row hover highlights, thumbnail previews, confidence meters, and action buttons.

### 7. Pothole Detail & Dispatch View (`frontend/src/pages/PotholeDetailPage.tsx`)
- **Lifecycle Timeline Track (`StatusTimeline.tsx`)**: 4-step progress tracker (`REPORTED` → `ACKNOWLEDGED` → `IN_PROGRESS` → `RESOLVED`) with lime progress connectors and timestamps.
- **Action Dispatcher**: Status transition button opening the dark update modal (`StatusUpdateModal.tsx`).
- **GIS Mini-Map**: Exact localized geolocation pin with latitude, longitude, and assigned municipal authority.

---

## 7. Accessibility & Responsive Strategy

- **Contrast Ratios**: Exceeds WCAG AA requirements (minimum 4.5:1 for body copy and 3:1 for large headings) with `#F1F5F9` on `#111417`.
- **Keyboard Navigation**: All interactive modals (`ImageLightboxModal`, `StatusUpdateModal`) support `Escape` key dismiss and focus containment.
- **Assistive Technology**: Universal `aria-label` tags on all logo links, action buttons, close triggers, and icon indicators.
- **Responsive Layout**: Seamlessly collapses on mobile viewports (<768px) with accessible hamburger toggle, sticky header, and stacked grid layouts.
