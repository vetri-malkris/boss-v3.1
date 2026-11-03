CREATE TABLE subcategory_data_records (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants (id),
    subcategory VARCHAR(32) NOT NULL,
    fields      JSONB       NOT NULL,
    created_by  UUID        NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    version     BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_subcategory_data_records_tenant_subcategory
    ON subcategory_data_records (tenant_id, subcategory, created_at DESC);
