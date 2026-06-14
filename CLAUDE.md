# CLAUDE.md

This file provides guidance to Claude Code when working in this repository.

## Repository overview

This repository contains software engineering course-design materials and the NekoCafe implementation.

- Course documents and templates live at the repository root, including `选题指南/`, `产出模板/`, and `管理表单/`.
- The main application lives in `NekoCafe/`.
- `NekoCafe/backend/` is a Spring Boot backend.
- `NekoCafe/frontend/` is a Vue 3 + Vite frontend.

## Application architecture

### Backend

Location: `NekoCafe/backend/`

Tech stack:

- Java 17
- Spring Boot 3.3.5
- Spring Web, Validation, Security, Actuator
- MyBatis-Plus
- MySQL
- Flyway database migrations
- JWT authentication via `io.jsonwebtoken`
- Springdoc OpenAPI / Swagger UI

Important backend paths:

- Entry point: `NekoCafe/backend/src/main/java/com/nekocafe/NekoCafeApplication.java`
- Configuration: `NekoCafe/backend/src/main/resources/application.yml`
- Dev profile: `NekoCafe/backend/src/main/resources/application-dev.yml`
- Flyway migrations: `NekoCafe/backend/src/main/resources/db/migration/`
- Security config: `NekoCafe/backend/src/main/java/com/nekocafe/security/SecurityConfig.java`
- Common API wrapper: `NekoCafe/backend/src/main/java/com/nekocafe/common/result/ApiResult.java`

Backend modules are organized by domain under `com.nekocafe`, including auth, security, user, store, menu, reservation, order, payment, staff, manager, dashboard, admin, and cat management.

### Frontend

Location: `NekoCafe/frontend/`

Tech stack:

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Element Plus
- Axios
- Day.js
- ECharts

Important frontend paths:

- Entry point: `NekoCafe/frontend/src/main.ts`
- Router: `NekoCafe/frontend/src/router/index.ts`
- Route guards: `NekoCafe/frontend/src/router/guards.ts`
- Permission helpers: `NekoCafe/frontend/src/router/permissions.ts`
- Auth store: `NekoCafe/frontend/src/stores/auth.ts`
- Axios client: `NekoCafe/frontend/src/api/http.ts`
- Layouts: `NekoCafe/frontend/src/layouts/`
- Views: `NekoCafe/frontend/src/views/`

Vite runs on port `5173` and proxies `/api` to `http://localhost:8080` in `NekoCafe/frontend/vite.config.ts`.

## Common commands

Run commands from the listed directories.

### Backend

```bash
cd NekoCafe/backend
mvn spring-boot:run
```

Build backend:

```bash
cd NekoCafe/backend
mvn -B -DskipTests package
```

Run backend tests if tests are present:

```bash
cd NekoCafe/backend
mvn test
```

### Frontend

Install dependencies:

```bash
cd NekoCafe/frontend
npm install
```

Start dev server:

```bash
cd NekoCafe/frontend
npm run dev
```

Build frontend:

```bash
cd NekoCafe/frontend
npm run build
```

Type-check / lint command:

```bash
cd NekoCafe/frontend
npm run typecheck
npm run lint
```

Preview production build:

```bash
cd NekoCafe/frontend
npm run preview
```

## Runtime configuration

Backend defaults:

- Server port: `8080`
- Active profile: `${NEKO_PROFILE:dev}`
- Health endpoint: `/actuator/health`
- Swagger UI: `/swagger-ui.html`
- OpenAPI docs: `/api-docs`

Development database settings are read from environment variables in `application-dev.yml`:

- `MYSQL_HOST`, default `localhost`
- `MYSQL_PORT`, default `3306`
- `MYSQL_DATABASE`, default `nekocafe`
- `MYSQL_USERNAME`, default `nekocafe_app`
- `MYSQL_PASSWORD`, default `change_me`
- `FLYWAY_ENABLED`, default `true`

Security-sensitive environment variables:

- `JWT_SECRET` should be set to a strong value in real deployments.
- Do not commit real secrets or tokens.
- Do not print secrets or tokens in comments, logs, test output, or documentation.

## API and security conventions

- Backend API responses use `ApiResult<T>` with `code`, `message`, `data`, `traceId`, and `timestamp`.
- Successful responses use `code = 0`.
- Frontend Axios error handling expects non-zero `code` values to represent application-level failures.
- JWT tokens are attached by the frontend as `Authorization: Bearer <token>`.
- Public backend endpoints are configured in `SecurityConfig`; all other endpoints require authentication.
- Before changing authentication, authorization, JWT, password handling, or role checks, call out the security impact explicitly.

## Development notes

- Match the existing domain-oriented backend package structure.
- Keep frontend API wrappers under `NekoCafe/frontend/src/api/` and view components under `NekoCafe/frontend/src/views/`.
- Prefer typed DTOs and TypeScript interfaces when adding frontend API calls.
- Keep route authorization metadata in router definitions and related permission helpers.
- Use Flyway migrations for database schema or seed-data changes instead of ad-hoc SQL.
- Avoid committing generated build output such as `target/`, `dist/`, and `node_modules/`.

## CI

There is a CI workflow at `NekoCafe/.github/workflows/ci.yml` that builds the backend and frontend using Java 17 and Node 20. Its build steps are:

```bash
cd NekoCafe/backend && mvn -B -DskipTests package
cd NekoCafe/frontend && npm install
cd NekoCafe/frontend && npm run build
```

If the workflow is intended to run on GitHub, ensure the workflow file is located where GitHub Actions can detect it for this repository.

## Document handling

This repository contains many Word/docx and Office documents. When modifying rich-text documents:

- Back up the original file first.
- Prefer safe document tooling such as `python-docx`, LibreOffice, Office automation, or dedicated document libraries.
- Avoid directly rewriting docx internal XML unless doing a minimal targeted replacement.
- After modification, verify the document can still be opened or converted, and remind the user to inspect pagination, images, screenshots, and PDF/exported outputs manually.
