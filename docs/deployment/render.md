# Deploying to Render

## Services

1. **PostgreSQL** — Render Postgres. Copy the Internal Database URL.
2. **Backend** — Web Service from `backend/Dockerfile` (or Maven native build).
3. **Frontend** — Static site from `frontend` (`npm run build`, publish `dist`) **or** Docker image with Nginx.

## Backend env

| Variable | Notes |
| --- | --- |
| `DATABASE_URL` | Must be JDBC: `jdbc:postgresql://HOST:5432/DB` |
| `DATABASE_USER` | From Render |
| `DATABASE_PASSWORD` | From Render |
| `CORS_ORIGINS` | Frontend origin, e.g. `https://adp-automate.onrender.com` |
| `UPLOAD_DIR` | Persistent disk mount path if using Render Disk |
| `PORT` | Render sets this automatically |

Render’s `postgres://user:pass@host/db` URL must be converted to JDBC form.

## Frontend env (build-time)

| Variable | Notes |
| --- | --- |
| `VITE_API_BASE_URL` | Public backend URL + `/api`, or `/api` if same origin via proxy |

## Suggested next step

Create the three Render resources and paste the service URLs / DB credentials back into this chat so CI deploy workflows can be wired.
