# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Repository layout

The actual project lives in `edu-saas-platform/` (EduSphere, a multi-tenant education SaaS platform). The root-level `pom.xml` and `src/` are a leftover IntelliJ scratch project (`org.example` demo code, e.g. pay-strategy examples) unrelated to the platform — don't touch them when working on the platform.

```
edu-saas-platform/
├─ backend/    Maven multi-module Spring Boot 3.3.5 backend (Java 21)
├─ frontend/   Vue 3 + TypeScript + Vite admin UI (Ant Design Vue)
├─ deploy/     Dockerfiles, docker-compose, nginx, prometheus configs
└─ docs/       Architecture and design docs (Chinese)
```

Documentation, commit messages, and most code comments are in Chinese.

## Project-local skills

- For UI/UX, frontend visual design, layout, accessibility, and interaction polish work in this repository, first read the project-local skill at `.codex/skills/ui-ux-pro-max/SKILL.md`.
- Do not rely on a global `ui-ux-pro-max` skill for this project; this repository carries its own local copy so other projects remain unaffected.
- When running the skill search helper from this repository root, use `python3 .codex/skills/ui-ux-pro-max/scripts/search.py ...`.

## Commands

### Backend (run from `edu-saas-platform/backend/`)

```bash
mvn clean install -DskipTests   # build all modules (required once before running)
cd edu-api && mvn spring-boot:run   # start API on http://localhost:8080
```

- Swagger UI: `http://localhost:8080/swagger-ui.html`; actuator at `/actuator/health|metrics|prometheus`.
- Requires MySQL 8 (`edu_saas` database) and Redis running locally. See `docs/local-startup-guide.md` for Docker one-liners.
- Config via env vars: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `JWT_SECRET`, `PAYMENT_CALLBACK_SECRET`.
- There are currently no automated tests in the backend.

### Frontend (run from `edu-saas-platform/frontend/`)

```bash
npm install
npm run dev -- --host 127.0.0.1   # dev server on http://localhost:5173
npm run build                      # vue-tsc -b && vite build (type-checks too)
```

Demo login: organization code `demo`, username `admin`, password `demo123456`.

## Backend architecture

Maven multi-module monolith under `backend/` (parent POM `com.edusphere:edu-saas-platform`):

- **edu-api** — the only Spring Boot application module. Contains ALL controllers, services, configuration, jobs, and Flyway migrations (`edu-api/src/main/resources/db/migration/`). Entry point: `EduApiApplication`.
- **edu-common** — `ApiResult`/`PageResult` response wrappers, `BizException`, `BaseEntity` (audit fields), Redis cache helpers (`ReliableCacheHelper`, `RedisSupportService`), request tracing.
- **edu-security** — JWT auth (`JwtService`, `JwtAuthenticationFilter`), `TenantContext`/`SecurityContext` ThreadLocals, RBAC via `@RequirePermission` + `PermissionAspect`, data-scope support (`DataScope`, `DataScopeSupport`).
- **edu-tenant / edu-system / edu-course / edu-order** — domain modules holding only MyBatis Plus entities (`domain/`) and mappers (`mapper/`). Business logic for these domains lives in `edu-api`'s service layer, not in these modules.

Key architectural facts that span multiple files:

- **Tenant isolation is manual.** Every business table has `tenant_id`. `JwtAuthenticationFilter` populates `TenantContext` (ThreadLocal) from the JWT, but there is NO MyBatis Plus tenant-line interceptor — `MybatisPlusConfig` only registers pagination (max page size 100). Services must filter queries by `TenantContext.get()` explicitly. When adding queries, always include the tenant filter.
- **Auth flow**: `POST /api/auth/login` (organizationCode + username + password) → access/refresh JWT pair. Token revocation uses a Redis blacklist. Security chain is configured in `edu-api`'s `DevSecurityConfig` (stateless, CSRF disabled).
- **Order/payment safety**: payment callbacks are idempotent (DB unique constraint + Redis), order numbers use `SecureRandom`, class capacity uses pessimistic locking, all `BigDecimal` money math uses a unified scale. Preserve these patterns when touching order/payment code.
- **Caching**: Redis-backed, consistency-first via `ReliableCacheHelper` in edu-common — use it rather than ad-hoc cache code.

## Schema/domain pitfalls (history of commit 418a707)

The five once-disabled controllers (Attendance, Contract, Notification, Report, Scheduling) have been restored and Flyway re-enabled (`baseline-on-migrate: true`). The bugs that caused the disablement were column/field mismatches — watch for these patterns when writing new code:

- `attendance_record` columns are `status` and `checked_at` (NOT `attendance_status`/`check_in_at`); this applies to raw SQL in `JdbcTemplate` queries too, which the compiler can't check.
- `class_enrollment` has no `total_sessions` column — total sessions come from `course_product.total_lessons` via `class_group.course_product_id`.
- `ApiResult` is a record: accessor is `.data()`, not `.getData()`.
- The `tenant` table has no `tenant_id` column; the `Tenant` entity shadows the inherited `BaseEntity.tenantId` with `@TableField(exist = false)`.
- MySQL 8 does not support `ALTER TABLE ... ADD COLUMN/INDEX IF NOT EXISTS` (MariaDB-only) — don't use it in Flyway migrations.
- Demo permissions: `db/seed.sql` must contain a `menu_permission` row + `role_permission` grant for every `@RequirePermission` code, or endpoints return 403 for the demo admin.
- `db/seed.sql` must be imported with utf8mb4 (`mysql --default-character-set=utf8mb4 < seed.sql`); the script now starts with `SET NAMES utf8mb4` as a guard, but older imports stored mojibake Chinese.
- `a-statistic` icons go in the `#prefix` slot, not `:prefix="() => h(Icon)"` — function props render as literal text.
- Don't put `@Cacheable` on controller methods returning `ApiResult` — records serialize without type info under the Redis cache's `NON_FINAL` default typing and fail to deserialize; SpEL `#param` keys only see method parameters (not locals like `tenantId`), which previously caused a cross-tenant cache-key bug.

## Frontend architecture

- Vue 3 `<script setup>` + TypeScript, Pinia, Vue Router, **Ant Design Vue 4** (the README still mentions Element Plus; Ant Design Vue is what's actually used since the UI upgrade).
- HTTP layer: `src/api/http.ts` (axios). Views in `src/views/`, layout in `AdminLayout.vue` (note: uses `h()` render calls, not JSX — JSX is not configured in this Vite setup). Tenant theming lives under `src/theme/`.
