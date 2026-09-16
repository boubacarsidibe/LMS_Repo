# Repository Guidelines

## Project Structure

This LMS has two applications and a local PostgreSQL stack.

- `backend/`: Spring Boot 4, Java 17, JWT, PostgreSQL, and Flyway.
  Java code is under `backend/src/main/java/inpg/sn/lms/`; tests mirror it in
  `backend/src/test/java/`. Database migrations live in
  `backend/src/main/resources/db/migration/`.
- `frontend/`: Angular 21 and Tailwind CSS. Put domain screens in
  `frontend/src/app/features/`, shared UI in `shared/`, and API/auth code in
  `core/`.
- `backend/db/`: replayable seed scripts; `docs/` contains audits and handover notes.

## Build, Test, and Development

```bash
docker compose up -d --build          # start frontend, API, and PostgreSQL
cd frontend && npm ci && npm run build # production Angular build
cd frontend && npm test -- --watch=false # run Angular/Vitest tests once
cd backend && ./mvnw test             # run Java tests (Linux, macOS, or CI)
```

Use Docker for an integrated local run. Do not run Spring integration tests against
a shared database: Flyway migrations may be applied. The Windows Maven wrapper is
currently unreliable; CI uses `./mvnw test`.

## Coding Style

Follow the existing style: four spaces in Java, two spaces in TypeScript/HTML, and
French names/messages where the surrounding domain uses French. Name Java classes
in `PascalCase`, methods/fields in `camelCase`, Angular components as
`*.component.ts`, and tests as `*Test.java` or `*.spec.ts`.

Use Tailwind design tokens and shared components; do not add arbitrary colors. A
page heading needs both a font family and scale, for example
`font-display text-display`. Keep frontend files' existing line endings intact.

## Testing and Database Changes

Add focused tests for bug fixes and business rules. Prefer Mockito unit tests for
backend services and Vitest component/service tests for frontend behavior.

Never edit an applied Flyway migration. Add a new timestamped migration such as
`V202609161030__ajout_statut.sql`; avoid sequential version collisions.

## Commits and Pull Requests

Recent history uses concise imperative messages, commonly `feat(scope): ...`,
`fix: ...`, or `refine: ...`. Keep each commit scoped to one change. PRs should
state the user-visible result, affected roles/routes, migration requirements, and
validation commands. Include screenshots for visual Angular changes and link the
relevant issue or audit item when available.

## Security and Configuration

Never commit `backend/.env`, passwords, JWT keys, or SMTP credentials. Configure
secrets through environment variables and keep demo data disabled outside Compose.
Enforce authorization and ownership in backend endpoints; Angular guards are not a
security boundary.
