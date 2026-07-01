# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Internal expense management tool. Employees submit expenses with an optional receipt; each expense
goes through a two-step approval (Team Lead, then Finance Manager) before reimbursement. Two-part
repo: a Spring Boot 3 / Java 21 backend (`backend/`) and a React 18 / TypeScript / Vite frontend
(`frontend/`).

## Commands

### Backend (`backend/`)

```
./mvnw.cmd spring-boot:run                        # run the API on :8080 (dev profile active by default)
./mvnw.cmd test                                    # run all tests
./mvnw.cmd test -Dtest=ApprovalServiceTest          # run a single test class
./mvnw.cmd test -Dtest=ApprovalServiceTest#approve_asTeamLead_movesToPendingFinance   # single test method
./mvnw.cmd compile                                  # compile only
```

There is no system-wide `mvn` — always invoke via `./mvnw.cmd` (Windows) from `backend/`. The
wrapper downloads the pinned Maven distribution (3.9.9) on first use, which requires network
access; if it's unavailable, the wrapper JAR/zip may need to be fetched manually.

The service and unit tests (`ApprovalServiceTest`, `DashboardServiceTest`) are plain Mockito tests
with `@InjectMocks`/`@Mock` — no Spring context or database is spun up for them.

The app requires a running PostgreSQL instance to actually start (Flyway runs migrations on boot;
`ddl-auto: validate`, not `update`). Point it at Postgres via env vars or run one locally, e.g.:

```
docker run -d --name expense-pg -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=expense_tracker -p 5432:5432 postgres:16
```

Connection is configured via `DB_URL` / `DB_USER` / `DB_PASSWORD` (see `application.yml`), defaulting
to `jdbc:postgresql://localhost:5432/expense_tracker` / `postgres` / `postgres`.

### Frontend (`frontend/`)

```
npm install
npm run dev        # Vite dev server on :5173, proxies /api/* to localhost:8080
npm run build       # tsc typecheck + production build
npm run preview
```

There is no configured lint or test script in `frontend/package.json` — `npm run build` (which runs
`tsc`) is the main correctness check available.

## Architecture

### Domain / approval state machine

The core entity is `Expense` (`backend/src/main/java/com/company/expense/expense/Expense.java`),
which moves through `ExpenseStatus`:

```
PENDING_TEAM_LEAD --TL approve--> PENDING_FINANCE --FM approve--> APPROVED
        |                                 |
    TL reject                         FM reject
        v                                 v
    REJECTED                          REJECTED
```

`ApprovalService` (`backend/.../approval/ApprovalService.java`) owns this state machine plus the
routing/authorization rules. Every transition writes an immutable `ApprovalEvent` row (audit trail),
and a rejection at either stage is terminal — the expense never resumes progressing. Each
`Department` has exactly one assigned `team_lead_id` and one `finance_manager_id`
(`department` table); an approver can only act on expenses in a department they're assigned to as
approver for that stage — this is enforced in `ApprovalService.loadForDecision`, not just by role.

### Roles and authorization

Three roles (`user/Role.java`): `EMPLOYEE`, `TEAM_LEAD`, `FINANCE_MANAGER`. A user has exactly one
role and belongs to at most one department (`department_id` on `users`, nullable — employees without
a department cannot submit expenses). Authorization is layered:
- Coarse-grained: `@PreAuthorize("hasRole(...)")` / `hasAnyRole(...)` on controllers.
- Fine-grained, data-dependent: hand-written checks in services — e.g. `ExpenseService.authorizeView`
  (owner, or the department's assigned Team Lead/Finance Manager may view an expense/receipt) and
  `ApprovalService.loadForDecision` (must be the *assigned* approver for that department and stage,
  not just hold the role).

### Auth

Stateless JWT auth (`auth/JwtService.java`, `config/JwtAuthFilter.java`): login returns a bearer
token (`app.jwt.secret` / `app.jwt.expiration-minutes`, default 8h) carrying `uid` and `role`
claims; `SecurityConfig` disables CSRF/sessions and permits only `/api/v1/auth/login` and the health
endpoints unauthenticated. `AppUserPrincipal` is the authenticated-user object threaded through
services via `SecurityUtils.currentUser()`.

### Module layout (backend)

Package-by-feature under `com.company.expense`: `auth`, `user`, `department`, `expense`, `approval`,
`receipt`, `dashboard`, `notification`, `common` (shared `ApiException` /
`GlobalExceptionHandler`, which maps exceptions to a uniform JSON error body), `config` (security,
JWT filter, `DataSeeder`). Each feature package generally has an entity, a Spring Data
`*Repository`, a `*Service` holding business logic, a `*Controller`, and a `dto/` subpackage for
request/response records — follow this shape when adding a new feature.

Receipts are stored as `BYTEA` directly in Postgres (`receipt/Receipt.java`), not on disk/blob
storage; max 5MB, only PNG/JPEG/PDF (`ExpenseService.ALLOWED_CONTENT_TYPES`). `DashboardService`
aggregates pending totals by department via a native/derived query
(`ExpenseRepository.aggregatePendingByDepartment`) for the Finance Manager dashboard.

Email notifications (`notification/EmailNotificationService.java`) fire on submit and on each
approval/rejection decision. In the `dev` profile, if no SMTP server is reachable, mail is logged to
the console instead of sent (`app.mail.log-only`, see `application-dev.yml`) — point `MAIL_HOST`
/`MAIL_PORT` at a local Mailhog instance to capture them in a UI instead.

### Data seeding

`config/DataSeeder.java` runs only under the `dev` profile with `app.seed.enabled=true`, and only if
the `users` table is empty (idempotent). It creates two departments (Engineering, Marketing) with
their Team Lead/Finance Manager, a few employees, and sample expenses across every status so the
approval queues and dashboard have realistic data. All seeded users share the password
`password123` (e.g. `alice@company.com`, `bob.lead@company.com`, `fiona.finance@company.com`).

### Frontend

Role-based routing lives in `App.tsx`: after login, `RoleHome` redirects each role to its landing
page (`EMPLOYEE` → `/submit`, `TEAM_LEAD` → `/approvals`, `FINANCE_MANAGER` → `/dashboard`), and
`ProtectedRoute` (`auth/ProtectedRoute.tsx`) gates routes both on auth and on an optional `roles`
allow-list. `AuthContext` holds the current user and bootstraps it from a stored JWT on load.

`api/client.ts` is a single Axios instance: it attaches the bearer token from `localStorage` on every
request and, on any `401` response, clears the token and redirects to `/login`. Per-feature API
modules (`api/auth.ts`, `api/expenses.ts`, `api/approvals.ts`, `api/dashboard.ts`) wrap this client;
add new endpoints there rather than calling `axios`/`api` directly from components. Shared
request/response shapes are mirrored by hand in `types/index.ts` and must be kept in sync with the
backend DTOs (`expense/dto/*`, `approval/dto/*`, `auth/dto/*`, `dashboard/DashboardResponse.java`) —
there is no codegen between them.

In dev, Vite proxies `/api/*` to `localhost:8080` (`vite.config.ts`), so the frontend always calls
relative `/api/v1/...` paths.
