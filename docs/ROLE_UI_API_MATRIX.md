# Role UI & API Authorization Matrix

| UI Component / View | Action | Allowed Role(s) | Required Status | Backend Endpoint | HTTP Success | HTTP Denied |
|---|---|---|---|---|---|---|
| Dashboard (`/`) | View Citizen Overview | ROLE_USER | ACTIVE | GET `/api/v1/dashboard/stats` | 200 OK | 401 Unauthorized |
| Dashboard (`/`) | View Officer Workspace | ROLE_OFFICER | VERIFIED | GET `/api/v1/officer/profile` | 200 OK | 403 Forbidden |
| Sidebar Navigation | Report Pothole Link | ROLE_USER | ACTIVE | N/A (Frontend UX) | Visible | Hidden |
| Sidebar Navigation | Review Queue & Dashboard | ROLE_OFFICER | ANY | N/A (Frontend UX) | Visible | Hidden |
| Header Navigation | Report Pothole Button | ROLE_USER | ACTIVE | N/A (Frontend UX) | Visible | Hidden |
| Report Page (`/upload`) | Upload Defect Image/Video | ROLE_USER | ACTIVE | POST `/api/v1/potholes/upload` | 201 Created | 403 Forbidden |
| Report Page (`/upload`) | Upload Attempt by Officer | ROLE_OFFICER | ANY | POST `/api/v1/potholes/upload` | N/A (Denied) | 403 Forbidden |
| Pothole Detail (`/potholes/:id`) | View Pothole Evidence | ROLE_USER, ROLE_OFFICER | ANY | GET `/api/v1/potholes/:id` | 200 OK | 401 Unauthorized |
| Pothole Detail (`/potholes/:id`) | Update Status Button | ROLE_OFFICER | VERIFIED | PATCH `/api/v1/officer/potholes/:id/status` | 200 OK | 403 Forbidden |
| Officer Dashboard | Accept Report | ROLE_OFFICER | VERIFIED | POST `/api/v1/officer/potholes/:id/accept` | 200 OK | 403 Forbidden |
| Officer Dashboard | Reject Report | ROLE_OFFICER | VERIFIED | POST `/api/v1/officer/potholes/:id/reject` | 200 OK | 403 Forbidden |
| Officer Dashboard | Self-Verify Demo Button | ROLE_OFFICER | PENDING_VERIFICATION | PATCH `/api/v1/officer/admin/verify/:id` | 200 OK | 403 Forbidden |
