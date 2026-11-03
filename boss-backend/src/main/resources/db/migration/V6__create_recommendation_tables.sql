CREATE TABLE recommendations (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID          NOT NULL REFERENCES tenants (id),
    period                VARCHAR(7)    NOT NULL,
    source_risk           VARCHAR(255)  NULL,
    issue                 VARCHAR(255)  NOT NULL,
    root_cause            VARCHAR(1000) NOT NULL,
    impact                VARCHAR(1000) NOT NULL,
    recommended_action    VARCHAR(1000) NOT NULL,
    expected_outcome      VARCHAR(1000) NOT NULL,
    confidence            NUMERIC(3,2)  NOT NULL,
    time_horizon          VARCHAR(100)  NOT NULL,
    follow_up_measurement VARCHAR(500)  NOT NULL,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version               BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_recommendations_tenant ON recommendations (tenant_id, created_at DESC);

CREATE TABLE recommendation_evidence (
    recommendation_id UUID          NOT NULL REFERENCES recommendations (id) ON DELETE CASCADE,
    label              VARCHAR(255) NOT NULL,
    value              VARCHAR(500) NOT NULL,
    position           INTEGER      NOT NULL
);
CREATE INDEX idx_recommendation_evidence ON recommendation_evidence (recommendation_id);

CREATE TABLE recommendation_alternatives (
    recommendation_id UUID          NOT NULL REFERENCES recommendations (id) ON DELETE CASCADE,
    alternative        VARCHAR(500) NOT NULL,
    position           INTEGER      NOT NULL
);
CREATE INDEX idx_recommendation_alternatives ON recommendation_alternatives (recommendation_id);

CREATE TABLE recommendation_risks (
    recommendation_id UUID          NOT NULL REFERENCES recommendations (id) ON DELETE CASCADE,
    risk               VARCHAR(500) NOT NULL,
    position           INTEGER      NOT NULL
);
CREATE INDEX idx_recommendation_risks ON recommendation_risks (recommendation_id);

CREATE TABLE recommendation_assumptions (
    recommendation_id UUID          NOT NULL REFERENCES recommendations (id) ON DELETE CASCADE,
    assumption         VARCHAR(500) NOT NULL,
    position           INTEGER      NOT NULL
);
CREATE INDEX idx_recommendation_assumptions ON recommendation_assumptions (recommendation_id);
