# ADP Automate (AIS Doc Manager)

UCAA · AIM Entebbe — shared register for NOTAMs, AIP/AIC publications, processing forms (OF/82–83d), library, complaints, and tasks.

**Stack (Phase 1 foundation)**

| Layer | Choice |
| --- | --- |
| Frontend | React 18 + TypeScript + Vite + Tailwind |
| Backend | Java 17 + Spring Boot 3.3 |
| Database | PostgreSQL 16 |
| Files | Local disk volume (S3/MinIO-compatible later) |
| CI | GitHub Actions |
| Hosting | Docker Compose locally · Render for staging |

> Spec PDF listed Java 21 / MySQL / MinIO. This repo uses **Java 17** and **PostgreSQL** as agreed for the first build; object storage can be swapped behind the same upload API.

Repository: [JonahKayizzi/ADP-Automate](https://github.com/JonahKayizzi/ADP-Automate)

## Monorepo layout

```
adp-automate/
├── frontend/          React SPA
├── backend/           Spring Boot API
├── e2e/               Playwright (later)
├── infrastructure/    Nginx, scripts
├── docs/
├── compose.yaml
└── .github/workflows/
```

## Prerequisites

- Node.js 20+
- JDK 17
- Maven 3.9+
- Docker Desktop (Postgres + full stack)

## Local development

### 1. Database

```bash
docker compose up -d db
```

### 2. Backend

```bash
cd backend
mvn spring-boot:run
```

API: http://localhost:8080  
Health: http://localhost:8080/actuator/health  
Ping: http://localhost:8080/api/ping  
Swagger: http://localhost:8080/swagger-ui.html

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

App: http://localhost:5173

### Full stack via Compose

```bash
docker compose up --build
```

App via Nginx: http://localhost:8088

## Environment

Copy `.env.example` to `.env`. On Render, set:

- `DATABASE_URL` as JDBC (`jdbc:postgresql://…`) — convert from Render’s `postgres://` URL if needed
- `DATABASE_USER` / `DATABASE_PASSWORD`
- `CORS_ORIGINS` to your frontend URL
- `VITE_API_BASE_URL` at frontend build time

## Implementation phases

1. **Foundation** (current) — repo, Spring Boot, React, Postgres, Docker, CI stub  
2. Core registers — NOTAMs, publications, AIRAC, library, files  
3. Processing files — OF/82–83d, signatures, revisions  
4. Operational modules — complaints, tasks, users, search, dashboard  
5. Stabilization — E2E, security, Render/UAT, release  

## License

Proprietary — Uganda Civil Aviation Authority.
