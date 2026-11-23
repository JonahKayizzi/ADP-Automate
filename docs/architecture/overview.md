# Architecture overview

Modular monolith:

- **frontend** — React SPA talking to REST APIs
- **backend** — Spring Boot services, JPA, Flyway, Spring Security
- **db** — PostgreSQL (staging on Render Postgres; same engine for local Compose)
- **files** — local disk volume initially; S3-compatible (MinIO/R2) later behind the same attachment API

Hash-style deep links from the prototype map to React Router paths (`/overview`, `/notams`, …). Live multi-user updates start with TanStack Query refetch/invalidation.
