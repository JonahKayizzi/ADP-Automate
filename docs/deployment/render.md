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

Paste that URL into the API service as `DATABASE_URL`. The app converts `postgres://…` to JDBC (`jdbc:postgresql://…?sslmode=require`) at startup.

If a deploy fails with `URL must start with 'jdbc'`, ensure the latest `dev` commit is deployed and use **Clear build cache & deploy** once so Docker does not reuse an old jar.

### 2. Backend web service

1. **New** → **Web Service** → this repo → branch **`dev`**.
2. Runtime: **Docker**.
3. Dockerfile path: `./backend/Dockerfile`
4. Docker build context directory: `.` (repository root — required so `backend/pom.xml` is visible)
5. Health check path: `/actuator/health`.

If the build says `"/pom.xml": not found` or `"/src": not found`, the context is wrong — set it to `.`, not `backend`.

**Environment**

| Key | Value |
| --- | --- |
| `DATABASE_URL` | Paste Render Postgres **External Database URL** |
| `CORS_ORIGINS` | Your frontend URL, e.g. `https://adp-automate-1.onrender.com` |
| `SESSION_COOKIE_SAME_SITE` | `none` (required: frontend and API are different hosts) |
| `SESSION_COOKIE_SECURE` | `true` |
| `UPLOAD_DIR` | `/var/data/uploads` (Docker image creates this; attach a Disk at `/var/data` for persistence) |

`PORT` is set by Render automatically. Session cookies become `Secure` + `SameSite=None` when Render’s `RENDER` env is present.

If a deploy fails with `Cannot create upload directory` / `AccessDeniedException`, the running user cannot write the upload path — redeploy a build that pre-creates `/var/data/uploads`, or set `UPLOAD_DIR` to a writable path.

Optional: attach a **persistent disk** at `/var/data` so uploads survive restarts.

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
