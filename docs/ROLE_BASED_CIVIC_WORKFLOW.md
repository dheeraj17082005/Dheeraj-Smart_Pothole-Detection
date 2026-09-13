# Role-Based Civic Workflow Architecture: Citizen & Verified Officer Application

## 1. Executive Summary & Persona Transformation

PotholeX has been transformed from a single-user demo concept into a production-grade, dual-role civic reporting and municipal remediation platform:
1. **Citizen (`ROLE_USER`)**: Public users who register, log in, submit road defect reports (with image/video and GPS location), track personal submissions, view public defect maps, and receive real-time notifications on status changes.
2. **Municipal Officer (`ROLE_OFFICER`)**: Government officials assigned to specific municipal departments. Officers undergo strict identity verification (uploading official ID cards stored securely in MinIO) and are constrained by PostGIS spatial jurisdiction parameters.

All legacy "Inspector Rajest" demo identity references have been completely purged from the frontend and backend.

---

## 2. Authentication & Role-Based Access Control (RBAC)

### Security Infrastructure
- **Security Engine**: Spring Security 6 with `BCryptPasswordEncoder` (10 rounds).
- **Session Management**: Stateless JWT authentication via `JwtAuthenticationFilter`.
- **JWT Token Standard**: Signed using HMAC-SHA256 containing `userId`, `email`, `role`, and `fullName`.

### Endpoint Access Rules
| Endpoint Pattern | Http Method | Required Role | Description |
|---|---|---|---|
| `/api/v1/auth/register` | POST | Public | Citizen registration |
| `/api/v1/auth/officer/register` | POST | Public | Officer registration + ID card upload |
| `/api/v1/auth/login` | POST | Public | Login & JWT issuance |
| `/api/v1/auth/me` | GET | Authenticated | Retrieve current user profile |
| `/api/v1/potholes/detect-image` | POST | Public / Auth | Submit image for detection (links user if auth) |
| `/api/v1/potholes/map` | GET | Public | Public map viewport markers |
| `/api/v1/officer/profile` | GET | `ROLE_OFFICER` | Officer profile & jurisdiction details |
| `/api/v1/officer/reports` | GET | `ROLE_OFFICER` (Verified) | Reports inside officer spatial jurisdiction |
| `/api/v1/officer/reports/{id}/status` | PATCH | `ROLE_OFFICER` (Verified) | Status update with 403 jurisdiction check |
| `/api/v1/notifications` | GET | Authenticated | User in-app notifications list |

---

## 3. Officer Verification & Secure Document Storage

### Secure ID Document Bucket
- Officer registration requires attaching a government-issued ID card document.
- Uploaded ID documents are saved in a dedicated, private MinIO bucket: `pothole-officer-docs`.
- **Privacy Enforcement**: Bucket access policies prohibit public `s3:GetObject`. ID documents are strictly inaccessible to citizens or unauthenticated requests.

### Verification Lifecycle State Machine
1. `PENDING_VERIFICATION`: Initial state upon registration. Officer can log in but cannot access spatial reports or modify report statuses.
2. `VERIFIED`: Approved officer account. Unlocks spatial jurisdiction reports and remediation controls.
3. `REJECTED`: Application denied. Access blocked.
4. `SUSPENDED`: Temporarily deactivated account.

---

## 4. PostGIS Spatial Jurisdiction Routing & 403 Enforcement

### Spatial Jurisdiction Model
Every officer profile is linked to an active `OfficerJurisdiction` record containing:
- `office_location`: `geometry(Point, 4326)` (Office coordinates).
- `radius_km`: Metric radius around office location (default 5.0 to 10.0 km).
- `boundary_polygon`: `geometry(Polygon, 4326)` (Optional administrative boundary).

### PostGIS Query Logic
Reports inside an officer's jurisdiction are queried dynamically using PostGIS geography measurement:

```sql
SELECT * FROM potholes p
WHERE ST_DWithin(
        CAST(p.location AS geography),
        CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
        :radiusMeters
      )
ORDER BY p.first_detected_at DESC;
```

### 403 Forbidden Authorization Checks
When an officer attempts to access or update report `PTH-1024`:
1. Verify officer status is `VERIFIED`. (If `PENDING_VERIFICATION`, return `HTTP 403 Forbidden: Officer account pending verification`).
2. Verify `PTH-1024` location is within the officer's spatial jurisdiction using `isReportWithinJurisdiction`. (If outside jurisdiction, return `HTTP 403 Forbidden: Report is outside officer spatial jurisdiction`).

---

## 5. Canonical Report Lifecycle & In-App Notifications

### Status Transition Flow
`REPORTED` $\to$ `ACKNOWLEDGED` $\to$ `IN_PROGRESS` $\to$ `RESOLVED`

- Each transition is audited in `pothole_status_history` with timestamp, officer name, department, and audit notes.

### In-App Notification Triggers
Whenever an officer updates a report status:
1. System checks if report is associated with a registered citizen `user_id`.
2. Creates a `Notification` entity (`STATUS_CHANGE`) for the citizen.
3. Citizen's topbar bell displays an unread badge counter.
4. Citizen clicks notification to open report details.

---

## 6. Leaflet Map Usability Refinements

1. **Unrestricted Panning**: Refactored Leaflet `MapContainer` with `MapViewController` using `moveend` and `zoomend` event listeners. Map panning never snaps back or forces center resets on re-render.
2. **Exact Coordinate Jump**: Coordinate jump toolbar allows entering exact `minLat`, `maxLat`, `minLng`, `maxLng` or target point (`lat`, `lng`) to immediately re-center.
3. **Marker Severity & Status Styling**:
   - `HIGH`: Red glow (`#EF4444`), symbol `H`.
   - `MEDIUM`: Amber glow (`#F59E0B`), symbol `M`.
   - `LOW`: Green glow (`#10B981`), symbol `L`.
   - `RESOLVED`: 60% opacity with green badge.

---

## 7. Automated Test Suite & Verification Results

### Backend Maven Test Suite
- `AuthServiceTest`: Passed (register, duplicate email check, officer registration, JWT login).
- `OfficerServiceTest`: Passed (profile query, pending verification 403 check, spatial jurisdiction query, out-of-jurisdiction 403 check, status transition).
- `NotificationServiceTest`: Passed (create notification, unread count, mark as read, forbidden ownership check).
- **Total Suite**: 107 tests passed cleanly (0 failures, 0 errors).

### Frontend Vitest Suite
- `useAuth`, `LoginPage`, `RegisterPage`, `OfficerDashboardPage`, `PotholeMap`: 28 tests passed cleanly.
- `npm run build`: Production bundle built successfully (`dist/index.html`, `dist/assets/index-B0DywtOy.js`).
