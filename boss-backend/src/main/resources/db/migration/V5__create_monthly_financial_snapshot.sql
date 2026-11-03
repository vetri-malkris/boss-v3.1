CREATE TABLE monthly_financial_snapshots (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID          NOT NULL REFERENCES tenants (id),
    period            VARCHAR(7)    NOT NULL, -- 'YYYY-MM'
    revenue           NUMERIC(18,2) NOT NULL,
    expenses          NUMERIC(18,2) NOT NULL,
    profit            NUMERIC(18,2) NOT NULL,
    margin            NUMERIC(9,2)  NOT NULL,
    cash_balance      NUMERIC(18,2) NULL,
    runway_months     NUMERIC(9,1)  NULL,
    receivables       NUMERIC(18,2) NULL,
    payables          NUMERIC(18,2) NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version           BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_snapshot_tenant_period UNIQUE (tenant_id, period)
);

CREATE INDEX idx_snapshots_tenant_period ON monthly_financial_snapshots (tenant_id, period);
