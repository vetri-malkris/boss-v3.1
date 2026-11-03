CREATE TABLE audit_logs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID         NULL,
    user_id        UUID         NULL,
    event_type     VARCHAR(64)  NOT NULL,
    entity_type    VARCHAR(64)  NULL,
    entity_id      VARCHAR(64)  NULL,
    description    VARCHAR(1000) NULL,
    correlation_id VARCHAR(64)  NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version        BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_audit_logs_tenant_id ON audit_logs (tenant_id, created_at DESC);
CREATE INDEX idx_audit_logs_event_type ON audit_logs (event_type);

-- Audit logs are append-only from the application's perspective: no UPDATE/DELETE
-- code path exists in AuditService. A future hardening pass can additionally revoke
-- UPDATE/DELETE grants on this table for the application's database role.
