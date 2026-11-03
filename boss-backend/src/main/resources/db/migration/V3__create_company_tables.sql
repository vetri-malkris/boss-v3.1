CREATE TABLE companies (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID         NOT NULL REFERENCES tenants (id),
    name                    VARCHAR(255) NOT NULL,
    currency                VARCHAR(3)   NOT NULL DEFAULT 'INR',
    fiscal_year_start_month INTEGER      NOT NULL DEFAULT 4,
    active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version                 BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_companies_tenant_id ON companies (tenant_id);
