# Local development

1. Start Docker Desktop.
2. `docker compose up -d db`
3. Backend: `cd backend && mvn spring-boot:run`
4. Frontend: `cd frontend && npm run dev`

Without Docker, install PostgreSQL 16 locally and set `DATABASE_*` in `.env`.
