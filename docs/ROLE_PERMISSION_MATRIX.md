# Central Role Permission Matrix — PotholeX

This document establishes the canonical definition of capabilities, permissions, and security rules for **PotholeX**.

## Actors & Roles

1. **USER (Citizen)**: Ordinary citizen user who registers, reports road defects/potholes, views personal report history, tracks status, and receives notifications.
2. **OFFICER (Municipal Officer)**: Verified government/department officer responsible for an assigned spatial jurisdiction. Controllable report lifecycle authority.

---

## Permission Matrix

| Capability | USER | OFFICER | Owning Role | Frontend Action | REST Endpoint | Backend Security Rule |
|---|:---:|:---:|---|---|---|---|
| Register Account | YES | YES | PUBLIC | `/register`, `/officer/register` | `POST /api/v1/auth/register`, `POST /api/v1/auth/officer/register` | Unauthenticated |
| Sign In / Sign Out | YES | YES | PUBLIC | `/login` | `POST /api/v1/auth/login` | Unauthenticated |
| View Own Profile | YES | YES | USER / OFFICER | Profile Header | `GET /api/v1/auth/me` | Authenticated principal |
| Upload Pothole Evidence | YES | NO | ROLE_USER | `/upload` Form Submit | `POST /api/v1/potholes/detect-image` | Authenticated ROLE_USER ONLY |
| Upload Dashcam Survey | YES | NO | ROLE_USER | `/upload` Video | `POST /api/v1/potholes/detect-video` | Authenticated ROLE_USER ONLY |
| View Own Reports | YES | YES | USER | `/potholes` Table | `GET /api/v1/potholes` | Filtered by `created_by_user_id` |
| View Jurisdiction Inbox | NO | YES | OFFICER | `/officer/dashboard` | `GET /api/v1/officer/reports` | `hasAuthority('ROLE_OFFICER')` & `VERIFIED` & Spatial containment |
| View Interactive Live Map | YES | YES | PUBLIC / ALL | `/map` Viewport | `GET /api/v1/potholes/map` | Bounding box spatial query |
| View Responsible Officer | YES | YES | PUBLIC / ALL | `/potholes/:id` | `GET /api/v1/potholes/:id` | Authenticated / Public detail |
| Receive Notifications | YES | YES | USER / OFFICER | Header Bell | `GET /api/v1/notifications/unread` | Authenticated `user_id` |
| Accept Incoming Report | NO | YES | OFFICER | `[Accept Report]` | `POST /api/v1/officer/reports/:id/accept` | `hasAuthority('ROLE_OFFICER')` & `VERIFIED` & Spatial jurisdiction |
| Reject Incoming Report | NO | YES | OFFICER | `[Reject Report]` | `POST /api/v1/officer/reports/:id/reject` | `hasAuthority('ROLE_OFFICER')` & `VERIFIED` & Spatial jurisdiction |
| Mark IN_PROGRESS | NO | YES | OFFICER | `[Start Work]` | `PATCH /api/v1/officer/reports/:id/status` | `hasAuthority('ROLE_OFFICER')` & `VERIFIED` & Spatial jurisdiction |
| Mark RESOLVED | NO | YES | OFFICER | `[Mark Resolved]` | `PATCH /api/v1/officer/reports/:id/status` | `hasAuthority('ROLE_OFFICER')` & `VERIFIED` & Spatial jurisdiction |
| Change Official Status | NO | YES | OFFICER | Officer Status Buttons | `PATCH /api/v1/officer/reports/:id/status` | `hasAuthority('ROLE_OFFICER')` & `VERIFIED` & State machine rules |
| Modify Original Evidence | NO | NO | NONE | N/A | N/A | Immutable original media & GPS |
| Modify AI Detections | NO | NO | NONE | N/A | N/A | Immutable model outputs |
| Approve Own Report | NO | NO | NONE | N/A | N/A | Officers cannot self-approve out-of-jurisdiction reports |

---

## State Transition Rules

```
       SUBMITTED / PENDING_OFFICER_REVIEW
                  ├───► REJECTED (Reason + Note)
                  └───► ACCEPTED
                           │
                           ▼
                      IN_PROGRESS
                           │
                           ▼
                       RESOLVED
```

- **Forbidden Transitions**:
  - `SUBMITTED` → `RESOLVED` (Direct jump blocked by state transition service).
  - Citizen attempts any status mutation → `403 Forbidden`.
  - Officer A attempts mutation on Officer B's jurisdiction → `403 Forbidden`.
  - Unverified Officer attempts inbox access or status mutation → `403 Forbidden`.
