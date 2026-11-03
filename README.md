# BOSS — AI Business Decision Intelligence Platform

BOSS is an AI-powered business decision intelligence / "AI CFO" platform. It is a
decision layer on top of business data, not an accounting system replacement.

**The UI is intentionally minimal: exactly two routes.**

```
/login       sign in, or toggle to create a new workspace (no separate route)
/dashboard   everything else - a dynamic sidebar, cards, charts, an upload dialog,
             and the AI CFO chat
```

There are no other routes. `/dashboard` has one collapsible sidebar (business
category -> subcategory, toggled icon-only/full-width by a single button) that
filters *which* panels are shown on that same screen - it never navigates anywhere.
Every capability (financials, trends, risks, forecasting, scenarios,
recommendations, actions, outcomes, AI chat) is still a panel, card, or dialog on
that single screen. On login (and via the sidebar's "Overview" entry) the dashboard
shows every panel the signed-in user has access to at once; picking a specific
subcategory narrows it down to just that.

The whole UI is styled as neumorphic "soft UI" in light mode: one flat off-white
background color throughout, with every card/button/input/tile reading as raised or
pressed purely from a paired light/dark shadow (`--neu-*` tokens and `.mat-mdc-*`
overrides in `src/styles.css`) - never from a border or a contrasting panel color.
Sidebar categories are click-to-expand (one open at a time; the sidebar itself also
collapses to an icon rail) and the sidebar scrolls independently of the main
content, so scrolling the menu never moves the dashboard.

## Repository layout

```
boss-backend/   Java 21 / Spring Boot 3 modular monolith (Maven)
boss-web/       Angular + Angular Material (dark theme) + Tailwind + ECharts frontend
docker-compose.yml   Local dev stack: Postgres, Redis, backend, frontend
```

## What's real vs. what's a placeholder

Every number on the dashboard comes from a deterministic calculation or real
persisted history - nothing is fabricated (including in the AI chat). Built so far:

- **Auth, tenancy, RBAC** - JWT auth, tenant isolation, audit log.
- **Ingestion** - CSV/XLSX adapters parse an uploaded transactions file in memory;
  nothing is written to disk. Malformed rows are skipped and reported, not fatal.
- **Financial engine** - `RevenueCalculator`/`ExpenseCalculator`/`ProfitCalculator`/
  `MarginCalculator`/`BurnRateCalculator`/`RunwayCalculator`/`MonthlyBreakdownCalculator`.
  Each upload upserts one `MonthlyFinancialSnapshot` per calendar month it covers -
  this is the only thing that survives past the request (no raw transactions are
  ever persisted).
- **Analytics** - `MonthComparisonService` (current vs. previous month) and
  `TrendDetectionEngine` (increasing/decreasing/stable/volatile over up to 6 months)
  run off snapshot history.
- **Risk engine** - `RiskEngine` runs deterministic rules (operating at a loss, low
  margin, short runway, revenue decline, margin compression, rapid expense growth)
  over the latest snapshot + trends. Every risk's evidence is labeled FACT; any
  possible cause is explicitly marked as unconfirmed.
- **Forecasting** - `ForecastService` fits an ordinary-least-squares linear trend
  (`LinearRegressionForecaster`) over revenue/expense history and projects forward
  (3/6/12/24/36-month horizons, `GET /api/v1/forecasts`, also embedded in the
  dashboard payload at a 12-month horizon). Derives break-even (first future month
  projected revenue ≥ projected expenses) and a cash-runway projection (first month
  projected cash ≤ 0) from that same trend - both `null` rather than guessed when
  not reached within the horizon, or when no starting cash is known. Needs ≥3
  months of history, same as trend detection.
- **AI CFO chat** - a real `AiOrchestrator` → `AiClient` pipeline with tool access to
  both `DashboardService` and `ForecastService`. With no LLM provider configured
  (`AI_PROVIDER=mock`, the default), `MockAiClient` answers by selecting and phrasing
  real evidence gathered by the orchestrator - it never invents a number, including
  forecast numbers. Swapping in a real provider later requires no change to the
  orchestrator, controller, or frontend.
- **Scenario engine** - `ScenarioEngine` applies hypothetical revenue %/expense %/
  hiring changes to the *existing forecast trend* (reusing `ForecastService`'s
  projection, never mutating it) and recalculates cash/runway/break-even from the
  adjusted trajectory via the same `ForecastProjectionDeriver` the real forecast
  uses - base and scenario are directly comparable because they're computed the
  same way. `POST /api/v1/scenarios/simulate`.
- **Recommendation engine** - `RecommendationEngine` translates each risk from
  `RiskEngine` into one persisted, actionable `Recommendation` (evidence, root
  cause, impact, recommended action, alternatives, expected outcome, risks-of-
  acting, confidence, assumptions, time horizon, follow-up) via a template keyed
  by risk type - deterministic Java, not an LLM call, so nothing here can invent a
  claim the risk engine didn't already establish. Confidence is deliberately
  moderate (0.45-0.6): the diagnosis is a fact, the specific remedy's effectiveness
  isn't. `GET /api/v1/recommendations` (list), `POST /api/v1/recommendations/generate`
  (writes new rows - not run automatically on every dashboard load).
- **Action & outcome tracking** - `Action` (a persisted state machine: PENDING_APPROVAL
  → APPROVED/REJECTED → IN_PROGRESS → COMPLETED, or CANCELLED at any non-terminal
  point) is proposed from a `Recommendation`, requires an elevated-role approval
  (TENANT_ADMIN/CFO/CEO) before it can be started or completed - nothing executes
  itself. Completing an action means supplying an actual outcome value; `variance`
  (actual − expected) is the only number the system computes itself, everything
  else is a human input. `GET/POST /api/v1/actions`, `POST /api/v1/actions/{id}/
  {approve,reject,start,complete,cancel}`.
- **All 8 implementation phases are now built.** Still explicitly out of scope:
  anomaly detection (statistical outlier detection vs. this period's rule-based
  risk checks) and receivables/payables tracking (would need AR/AP data the
  ingestion format doesn't capture yet).
- **Menu access (sidebar categories/subcategories)** - the sidebar's full taxonomy is
  6 business categories x 33 subcategories (`common.access.Category`/`Subcategory`,
  `GET /api/v1/categories`). A subcategory renders one of three things: a real
  analytics panel (Finance/Risk, computed from uploaded transactions), a real
  data-entry form (see below), or an honest "this section isn't built yet" state -
  never fabricated data. See `CategoryContent`'s `PANELS_BY_SUBCATEGORY` map and
  `DataEntryService`'s schema lookup for where those two "is this real" checks live.
  A user's menu access is a set of granted subcategories
  (`User.enabledSubcategories`); a category only appears in *their* sidebar if at
  least one of its subcategories is granted - there's no separate category-level
  flag, so "category on with nothing under it" isn't a representable state. A
  TENANT_ADMIN always sees the full taxonomy and manages other users' access from
  the **Manage users** dialog (create a user + pick their access, or edit an
  existing user's access at any time) - `POST /api/v1/users`,
  `PATCH /api/v1/users/{id}/access`.
- **Subcategory data entry** - some subcategories get their own real, domain-specific
  data-entry form and matching CSV/Excel bulk upload, distinct per subcategory (a
  Hiring record has real hiring fields, a Tax/GST record would have real tax
  fields - never a generic label/value form). The field schema for each is defined
  once in `SubcategorySchemaRegistry` and drives both the backend's validation
  (`SchemaValidator`) and the frontend's form rendering (`DataEntryForm`,
  schema-driven so the renderer itself is shared without the domains being
  generic) - `GET /api/v1/data-entry/schemas`,
  `GET/POST /api/v1/data-entry/{subcategory}/records`,
  `POST /api/v1/data-entry/{subcategory}/bulk-upload`. Entered rows persist as
  `SubcategoryDataRecord` (tenant + subcategory + a jsonb `fields` column - the
  schema keeps that column's *shape* explicit and distinct per subcategory even
  though the storage mechanism is shared). **27 of the 33 subcategories now have
  one**: all of Human Resource, Accounting and Compliance; all of Auditing; Finance's
  Capital; and a manual risk register for Risk's Exposure, Financial, Operational,
  Compliance and Technology (shown *alongside*, not instead of, the real
  auto-detected risk panel where one already exists - see `CategoryContent`, which
  renders panels and a data-entry form as independent blocks). The only
  subcategories still without a schema are Finance's Position/Liquidity/
  Profitability/Forecast/Cost (already real, transaction-driven analytics) and
  Risk's Mitigation (already the real risk -> recommendation -> action -> outcome
  workflow) - both deliberately excluded because they already have other real
  functionality, not because they're unbuilt.

## Running locally

### Option A: Docker Compose (full stack)

```bash
cp .env.example .env        # then set JWT_SECRET (openssl rand -base64 48) and DB_PASSWORD
docker compose up --build
```

- Frontend: http://localhost:4200
- Backend API: http://localhost:8080/api/v1
- Swagger UI: http://localhost:8080/swagger-ui.html

Sign up on `/login`, then use the **Upload data** button to drop a sample file
(repo root) - any CSV/XLSX with `date,description,amount,type` columns (any order)
works:

- [`sample-transactions.csv`](./sample-transactions.csv) /
  [`sample-transactions.xlsx`](./sample-transactions.xlsx) - one month, 8 rows,
  healthy and profitable.
- [`sample-transactions-full.csv`](./sample-transactions-full.csv) /
  [`sample-transactions-full.xlsx`](./sample-transactions-full.xlsx) - six months
  (Aug 2025 - Jan 2026), 42 rows, revenue declining / expenses rising every month.
  Populates the trend chart, month-over-month comparison, and (with a starting cash
  balance around 15000) all five risk rules at once.

### Option B: Run each service directly (faster iteration)

Backend (needs a local Postgres; the `dev` profile defaults to
`postgres://boss:boss@localhost:5432/boss`):

```bash
cd boss-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Frontend:

```bash
cd boss-web
npm install
npm start   # http://localhost:4200
```

## Backend architecture

Modular monolith; module boundaries are designed so any module can later be
extracted into its own microservice:

```
identity, tenant, company, ingestion, financial, analytics, risk, forecasting,
scenario, recommendation, action, ai, dashboard, audit, common
```

`common.access` holds the sidebar taxonomy (`Category`, `Subcategory` enums, each
subcategory tagged with exactly one category) - the single source of truth for menu
access, exposed read-only via `GET /api/v1/categories` and consumed by
`User.enabledSubcategories` (an `@ElementCollection`, `user_subcategories` table).

(`outcome` is folded into `action` - a completed Action already carries expected/
actual/variance, so there's no separate persisted Outcome entity. `integration`,
`notification`, `scheduler` are reserved for future integration work, not part of
the phase plan this build followed.)

Each module follows `Controller -> Service -> Domain -> Repository`, plus
`calculator/`, `engine/`, `orchestration/`, `llm/`, `parser/`, `validator/` where
relevant. Controllers stay thin. Tenant isolation is enforced explicitly at the
service layer (every tenant-scoped lookup takes the tenant id from the authenticated
JWT via `SecurityUtils`, never from a client-supplied parameter).

Key endpoints:

- `POST /api/v1/auth/login`, `POST /api/v1/auth/register` (creates tenant + admin)
- `POST /api/v1/ingestion/upload` - multipart file (+ optional `cashBalance`)
- `GET /api/v1/dashboard` - the entire dashboard state in one round trip: metrics,
  month-over-month comparison, trends, chart series, risks, and a 12-month forecast
- `GET /api/v1/forecasts?horizonMonths=12` - revenue/expense/cash/runway/break-even
  forecast; `horizonMonths` is one of 3/6/12/24/36
- `POST /api/v1/scenarios/simulate?horizonMonths=12` - `{ revenueChangePercent,
  expenseChangePercent, additionalHires, avgMonthlyCostPerHire }` → base vs. scenario
- `GET /api/v1/recommendations`, `POST /api/v1/recommendations/generate`
- `GET/POST /api/v1/actions`, `POST /api/v1/actions/{id}/{approve,reject,start,
  complete,cancel}` - approve/reject/start/complete require TENANT_ADMIN/CFO/CEO
- `POST /api/v1/ai/chat` - `{ "message": "..." }`
- `GET /api/v1/categories` - the full sidebar taxonomy (category -> subcategories)
- `GET/POST /api/v1/users`, `POST /api/v1/users/{id}/deactivate`,
  `PATCH /api/v1/users/{id}/access` - TENANT_ADMIN only; create a user with an
  initial `enabledSubcategories` set, or change an existing user's anytime

## Frontend architecture

Angular (standalone components, functional interceptors/guards), Angular Material
in dark mode, Tailwind for utility styling, ECharts for the trend chart:

```
core/       auth (AuthService/session), guards, interceptors, services
            (dashboard, category, category-view, user-management), config
shared/     models (DTO-mirroring interfaces, incl. the category/subcategory
            taxonomy), chart wrapper, subcategory-picker (reusable access editor)
layout/     dashboard-layout (header + sidebar + content wrapper), sidebar
            (dynamic per-user category/subcategory nav)
features/
  login/       sign-in, toggles in place to a create-workspace form
  dashboard/   the single screen, composed of components/:
               dashboard-header, category-content (maps the sidebar's selected
               subcategory to the relevant panels, or an honest "coming soon"),
               metric-cards, financial-chart, monthly-comparison, risk-panel,
               forecast-panel, scenario-panel, recommendation-panel, action-panel,
               outcome-panel, ai-chat, upload-dialog, manage-users-dialog,
               propose-action-dialog, complete-action-dialog
```

`CategoryViewService` holds the sidebar's currently-selected subcategory as a
signal (`null` = "Overview"); `Sidebar` writes to it on click, defaulting to
Overview so login always lands on a fully-populated dashboard rather than a blank
one. `CategoryService.visibleCategories`/`visibleSubcategoryCodes` derive the
current user's actual menu access (full taxonomy for TENANT_ADMIN, else whatever
they were granted) and are shared by both `Sidebar` (what to list) and
`CategoryContent` (what "Overview" unions together, and what a specific
subcategory maps to via its `PANELS_BY_SUBCATEGORY` table).

The access token lives in `AuthService` (backed by `localStorage`) and is attached
by `authInterceptor`; `errorInterceptor` drops the session and redirects to `/login`
on a 401 from a non-auth endpoint.

## Testing

Backend:

```bash
cd boss-backend
./mvnw test     # unit tests (Mockito) + one Testcontainers integration test -
                 # requires Docker running
```

123 tests: financial calculators, CSV/Excel adapters (including malformed-cell
regressions), snapshot upsert behavior, trend detection, month comparison, risk
rules, linear-regression forecasting, scenario engine (including a full-math
integration-style test suite, not just mocks), recommendation generation, the
action state machine (every legal/illegal transition) and variance computation,
AI response validation and grounding (including forecast questions), JWT, auth
flows, tenant isolation, user menu-access assignment (bootstrap admin gets every
subcategory, invited users get exactly what's granted, access is editable after
creation, tenant-isolated), and subcategory data entry (schema validation per
field type, bulk upload skipping invalid rows without failing the whole file).

Frontend:

```bash
cd boss-web
npm test        # Vitest unit tests
npm run e2e     # Playwright - needs the app running (docker compose up, or
                 # `ng serve` + backend) at http://localhost:4200
```

The e2e suite covers the full golden path: sign up → land on `/dashboard` (a
bootstrap admin, so the full sidebar taxonomy is visible) → upload via the dialog
→ click into the relevant sidebar subcategory (e.g. Finance → Position, Finance →
Forecast, Risk → Mitigation) → see real computed metrics/chart/comparison/risks/
forecast/scenario/recommendations/actions/outcomes → ask the AI CFO questions and
see grounded answers → sign out → confirm `/dashboard` redirects to `/login` when
unauthenticated.

## Security & privacy notes

- No secret (DB password, JWT signing key, AI provider key) has a production
  fallback; `application-prod.yml` requires them via environment variables and
  fails fast if unset.
- The AI provider key never reaches the browser - all AI calls are server-side
  (`ai` module) only.
- Uploaded files and parsed transaction rows are never persisted - only the
  aggregated monthly numbers (`MonthlyFinancialSnapshot`) survive, and the AI
  chat's question text itself is never logged (only that a query happened and its
  detected intent).
- Every tenant-scoped read/write is checked against the tenant id resolved from
  the caller's JWT; there is no endpoint that accepts a tenant id from the client.
