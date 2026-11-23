# Deploying ADP Automate to Render (staging)

Repo: `JonahKayizzi/ADP-Automate` · deploy from branch **`dev`**.

## Option A — Blueprint (fastest)

1. Open [Render Dashboard](https://dashboard.render.com) → **New** → **Blueprint**.
2. Connect GitHub and select **ADP-Automate**.
3. Use `render.yaml` from `dev`.
4. After services appear, set env vars (see below), then redeploy.

## Option B — Manual (three resources)

### 1. PostgreSQL

1. **New** → **PostgreSQL** (Free is fine for staging).
2. Name: `adp-automate-db`.
3. Copy **External Database URL** (starts with `postgres://…`).

The API accepts that URL as-is via `DATABASE_URL` (converted to JDBC + `sslmode=require`).

### 2. Backend web service

1. **New** → **Web Service** → this repo → branch **`dev`**.
2. Runtime: **Docker**.
3. Dockerfile path: `backend/Dockerfile` · Docker context: `backend`.
4. Health check path: `/actuator/health`.

**Environment**

| Key | Value |
| --- | --- |
| `DATABASE_URL` | Paste Render Postgres **External Database URL** |
| `CORS_ORIGINS` | Your frontend URL, e.g. `https://adp-automate-web.onrender.com` |
| `UPLOAD_DIR` | `/var/data/uploads` (if you attach a Disk) |

`PORT` is set by Render automatically. Session cookies become `Secure` + `SameSite=None` when Render’s `RENDER` env is present.

Optional: attach a **persistent disk** at `/var/data` for uploads later.

### 3. Frontend static site

1. **New** → **Static Site** → same repo → branch **`dev`**.
2. Build command: `cd frontend && npm ci && npm run build`
3. Publish directory: `frontend/dist`
4. Add a rewrite: `/*` → `/index.html` (SPA).

**Build env**

| Key | Value |
| --- | --- |
| `VITE_API_BASE_URL` | `https://<your-api-service>.onrender.com/api` |

Redeploy the frontend after the API URL is known.

## Verify

1. `https://<api>.onrender.com/actuator/health` → `UP`
2. `https://<api>.onrender.com/api/ping` → `status: ok`
3. Open the static site → create admin / sign in (first deploy with empty DB shows setup).

Free Render services spin down when idle; first request can take ~30–60s.

## After it works

Paste both public URLs here so we can tighten CORS and note them in the README.
