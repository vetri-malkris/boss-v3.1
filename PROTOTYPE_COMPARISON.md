# BOSS — Python Prototype vs. Claude-built Version

Comparison as of 2026-09-22. Prototype location: `V:\donttouch\Malkris\boss python`
(`founder_dashboardv1_app.py`, Streamlit). Main project: `V:\donttouch\Malkris\boss-claude`
(Java/Spring Boot + Angular).

| Dimension | Python Prototype (`boss python`) | BOSS — Claude-built (this project) |
|---|---|---|
| **Tech stack** | Single Python file, Streamlit | Java/Spring Boot backend + Angular frontend, separate services |
| **Database** | None — one flat `financial_data.json` file, overwritten every change | PostgreSQL with versioned Flyway migrations |
| **Login / users** | None — anyone who opens it sees/edits everything | JWT login, password hashing, per-user accounts |
| **Multi-tenancy** | No — one shared global state | Yes — isolated per company/workspace |
| **Roles / permissions** | None | TENANT_ADMIN, CEO, CFO, Manager, Employee, Viewer — each sees only what they're granted |
| **Concurrent users** | Breaks — two people/tabs overwrite each other's data | Designed for many simultaneous users per company |
| **Data input** | Manually typed: 6 numbers total (employees, avg CTC, clients, price, contract, cash) | Real CSV/Excel transaction upload **+** 27 structured domain-specific forms with their own bulk upload |
| **Historical data / trends** | None — only "right now"; every save overwrites the last | Real multi-month history accumulates from every upload; trend chart, month-over-month comparison |
| **Financial calculations** | Inline arithmetic in the script (payroll, burn, revenue, profit, runway) | Dedicated tested calculators (Revenue/Expense/Profit/Margin/Burn/Runway), unit-tested |
| **Forecasting** | Flat 5-month projection, hardcoded month labels (Jan–May regardless of real date) | Real linear-regression (OLS) forecast over actual history, 3/6/12/24/36-month horizons |
| **Scenario simulation** | Recomputes the *same* live numbers as you move sliders — no saved base to compare against | Real base-vs-scenario comparison reusing the same forecast engine; revenue%, expense%, hiring inputs |
| **Risk detection** | 5 hardcoded `if` thresholds | Rule-based `RiskEngine` over real snapshot history, evidence-labeled |
| **Recommendations** | Not separate from the risk list — just a warning line | Persisted, evidence-based recommendations (root cause, alternatives, confidence, follow-up) |
| **Action tracking / outcomes** | None | Full lifecycle: propose → approve → complete, with actual-vs-expected variance recorded |
| **AI layer** | Misnamed "AI CFO" — literally 5 `if` statements, no chat | Orchestrated chat grounded in real computed data (mock LLM by default, swappable) |
| **Other business domains** | Finance only | HR, Accounting, Auditing, Compliance, Risk register — 27 subcategories with real forms |
| **Bulk upload** | None | Yes, for every one of the 27 domain forms, validated row-by-row |
| **Reporting / export** | One button → 3-line PDF | None yet (a genuine gap) |
| **UI** | Basic Streamlit default theme | Custom neumorphic light-mode design, collapsible sidebar |
| **Data safety on error** | Corrupted file → code silently deletes it (`except: os.remove(...)`) | Validation errors are rejected with a clear message; nothing is silently destroyed |
| **Tests** | None | 123+ backend tests, Playwright e2e suite |
| **Deployment** | `streamlit run` on one machine | Docker Compose (Postgres, Redis, backend, frontend) |
| **Setup time** | Seconds | Minutes (Docker images, migrations) |
| **Best for** | One person, one sitting, quick "what-if" guessing | A team running a real business, over months, with accountability |

## Where the Python version actually wins

Only one place, honestly: **PDF export**. It's a bare 3-line PDF, but it exists — BOSS has
no export/reporting feature at all yet. Beyond that, and pure setup speed, there's no
dimension where the prototype comes out ahead.

## Verdict

The prototype has no row in this table where it's usable by more than one person or
survives past a single sitting. BOSS is the real product; the prototype's only remaining
value is as a five-minute scratchpad for testing a new formula before it gets built
properly into BOSS.
