# API Authorization Matrix — PotholeX

This document audits every REST endpoint in **PotholeX**, identifying its HTTP method, URL path, required role, spatial jurisdiction rule, state transition rule, and ownership policy.

| Method | Path | Required Role | Ownership Rule | Jurisdiction Rule | State Transition Rule |
|---|---|---|---|---|---|
| `POST` | `/api/v1/auth/register` | `PERMIT_ALL` | Creates citizen user | N/A | N/A |
| `POST` | `/api/v1/auth/officer/register` | `PERMIT_ALL` | Creates `PENDING_VERIFICATION` officer | N/A | N/A |
| `POST` | `/api/v1/auth/login` | `PERMIT_ALL` | Issues JWT token | N/A | N/A |
| `GET` | `/api/v1/auth/me` | `AUTHENTICATED` | Authenticated principal | N/A | N/A |
| `POST` | `/api/v1/potholes/detect-image` | `ROLE_USER` / `AUTHENTICATED` | Binds `created_by_user_id` from security context | Resolves PostGIS authority | Sets status `SUBMITTED` / `PENDING_OFFICER_REVIEW` |
| `POST` | `/api/v1/potholes/detect-video` | `ROLE_USER` / `AUTHENTICATED` | Binds `created_by_user_id` from security context | Resolves PostGIS authority | Sets status `SUBMITTED` / `PENDING_OFFICER_REVIEW` |
| `GET` | `/api/v1/potholes/map` | `PERMIT_ALL` | Public viewport query | Viewport bounding box | Read-only |
| `GET` | `/api/v1/potholes` | `AUTHENTICATED` | Filtered list | Public / Authority filter | Read-only |
| `GET` | `/api/v1/potholes/{id}` | `AUTHENTICATED` | Detail query | Public / Officer detail | Read-only |
| `GET` | `/api/v1/potholes/{id}/history` | `AUTHENTICATED` | Audit trail | Public / Officer detail | Read-only |
| `PATCH` | `/api/v1/potholes/{id}/status` | `ROLE_OFFICER` | Verified officer | Must match officer spatial jurisdiction | State machine enforced |
| `GET` | `/api/v1/officer/profile` | `ROLE_OFFICER` | Authenticated officer principal | Assigned jurisdiction | Read-only |
| `GET` | `/api/v1/officer/reports` | `ROLE_OFFICER` | Verified officer | Strict spatial containment (`ST_DWithin`) | Read-only |
| `POST` | `/api/v1/officer/reports/{id}/accept` | `ROLE_OFFICER` | Verified officer | Must match officer spatial jurisdiction | `SUBMITTED` → `ACCEPTED` |
| `POST` | `/api/v1/officer/reports/{id}/reject` | `ROLE_OFFICER` | Verified officer | Must match officer spatial jurisdiction | `SUBMITTED` → `REJECTED` |
| `PATCH` | `/api/v1/officer/reports/{id}/status` | `ROLE_OFFICER` | Verified officer | Must match officer spatial jurisdiction | `ACCEPTED` → `IN_PROGRESS` → `RESOLVED` |
| `GET` | `/api/v1/notifications/unread` | `AUTHENTICATED` | Authenticated principal | N/A | Read-only |
| `PATCH` | `/api/v1/notifications/{id}/read` | `AUTHENTICATED` | Recipient user | N/A | Marks `read_status = true` |
