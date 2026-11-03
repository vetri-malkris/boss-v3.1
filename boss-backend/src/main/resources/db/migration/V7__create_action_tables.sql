CREATE TABLE actions (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                UUID          NOT NULL REFERENCES tenants (id),
    recommendation_id        UUID          NOT NULL REFERENCES recommendations (id),
    description              VARCHAR(1000) NOT NULL,
    status                   VARCHAR(32)   NOT NULL,
    proposed_by              UUID          NOT NULL REFERENCES users (id),
    approved_by              UUID          NULL REFERENCES users (id),
    approved_at              TIMESTAMPTZ   NULL,
    executed_at              TIMESTAMPTZ   NULL,
    completed_at             TIMESTAMPTZ   NULL,
    expected_outcome_metric  VARCHAR(255)  NOT NULL,
    expected_outcome_value   NUMERIC(18,2) NOT NULL,
    actual_outcome_value     NUMERIC(18,2) NULL,
    variance                 NUMERIC(18,2) NULL,
    outcome_reason           VARCHAR(1000) NULL,
    created_at               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version                  BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_actions_tenant ON actions (tenant_id, created_at DESC);
CREATE INDEX idx_actions_recommendation ON actions (recommendation_id);
