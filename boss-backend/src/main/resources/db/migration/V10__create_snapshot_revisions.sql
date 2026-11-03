CREATE TABLE monthly_snapshot_revisions (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id              UUID        NOT NULL REFERENCES tenants (id),
    snapshot_id            UUID        NOT NULL REFERENCES monthly_financial_snapshots (id),
    period                 VARCHAR(7)  NOT NULL,
    previous_revenue       NUMERIC     NOT NULL,
    previous_expenses      NUMERIC     NOT NULL,
    previous_profit        NUMERIC     NOT NULL,
    previous_margin        NUMERIC     NOT NULL,
    previous_cash_balance  NUMERIC     NULL,
    previous_runway_months NUMERIC     NULL,
    revised_by             UUID        NULL REFERENCES users (id),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_monthly_snapshot_revisions_tenant_period
    ON monthly_snapshot_revisions (tenant_id, period, created_at DESC);
