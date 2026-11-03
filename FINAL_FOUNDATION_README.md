# BOSS — Definitive Foundation

This package is the consolidated engineering foundation built from the existing Version 3 codebase. It is **not labelled as Version 4**.

## Stack
- Backend: Java 21 / Spring Boot 3.3 / Spring Data JPA / PostgreSQL / Flyway
- Frontend: Angular 22 / Angular Material / ECharts
- Security: JWT + RBAC + tenant isolation
- Deployment: Docker Compose, separate backend/frontend containers

## Added definitive-foundation capabilities
- Subscription periods and token entitlement ledger
- Configurable business-model layer
- Configurable industry-profile layer
- Accounts receivable domain
- Accounts payable domain
- GST period and net-liability calculation
- Employee master data and tenure calculation
- Attrition event tracking and summary
- Unified business-health endpoint connecting AR, AP, GST, workforce and financial snapshots
- PostgreSQL constraints and tenant indexes for the new domains

## Architectural rule
Source data → validated business truth → deterministic calculations → analysis → intelligence → recommendation → human decision → action → outcome.

The AI layer must never become the system of record.

## Existing V3 functionality retained
Financial snapshots/revisions, forecasting, scenario analysis, risks, recommendations, actions/outcomes, ingestion, audit, authentication, tenant isolation and the existing Angular executive dashboard remain in place.

## Important verification note
The source environment used to prepare this package did not have Maven Central/network access and did not have a system Maven installation. Therefore a full Maven compile/test run could not be independently executed here. The original V3 test suite and source are retained; the new code should be compiled and tested in an environment with the declared dependencies available.

## Run
1. Configure PostgreSQL/Redis and environment variables using `.env.example`.
2. Start with `docker compose up --build` from the repository root.
3. Flyway applies the new `V12__definitive_foundation_domains.sql` migration automatically.
4. Open the Angular application through the web container.

## Key API additions
- `GET/POST /api/v1/subscription`
- `GET/POST /api/v1/business-models`
- `GET/POST /api/v1/industries`
- `GET/POST /api/v1/receivables`
- `GET/POST /api/v1/payables`
- `GET/POST /api/v1/gst`
- `GET/POST /api/v1/workforce/employees`
- `GET/POST /api/v1/workforce/attrition`
- `GET /api/v1/workforce/summary`
- `GET /api/v1/business-health`
