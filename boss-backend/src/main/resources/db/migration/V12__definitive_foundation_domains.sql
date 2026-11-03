CREATE TABLE subscriptions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    plan_type VARCHAR(20) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    allocated_tokens INTEGER NOT NULL CHECK (allocated_tokens >= 0),
    consumed_tokens INTEGER NOT NULL DEFAULT 0 CHECK (consumed_tokens >= 0 AND consumed_tokens <= allocated_tokens),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_subscriptions_tenant_active ON subscriptions(tenant_id, active, expires_at);

CREATE TABLE token_transactions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    amount INTEGER NOT NULL CHECK (amount > 0),
    action VARCHAR(100) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_token_transactions_tenant_created ON token_transactions(tenant_id, created_at DESC);

CREATE TABLE business_models (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    configuration_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, code)
);

CREATE TABLE industry_profiles (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    configuration_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, code)
);

CREATE TABLE receivables (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    party_id UUID NOT NULL,
    amount NUMERIC(19,2) NOT NULL CHECK (amount >= 0),
    settled_amount NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (settled_amount >= 0 AND settled_amount <= amount),
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_receivables_tenant_due ON receivables(tenant_id, due_date, status);

CREATE TABLE payables (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    party_id UUID NOT NULL,
    amount NUMERIC(19,2) NOT NULL CHECK (amount >= 0),
    settled_amount NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (settled_amount >= 0 AND settled_amount <= amount),
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_payables_tenant_due ON payables(tenant_id, due_date, status);

CREATE TABLE gst_periods (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    period VARCHAR(20) NOT NULL,
    output_tax NUMERIC(19,2) NOT NULL DEFAULT 0,
    input_tax_credit NUMERIC(19,2) NOT NULL DEFAULT 0,
    paid NUMERIC(19,2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, period)
);

CREATE TABLE employee_profiles (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(200) NOT NULL,
    date_of_birth DATE NOT NULL,
    date_of_joining DATE NOT NULL,
    address VARCHAR(1000),
    emergency_contact VARCHAR(100),
    salary NUMERIC(19,2) NOT NULL CHECK (salary >= 0),
    bonus NUMERIC(19,2) DEFAULT 0,
    pf NUMERIC(19,2) DEFAULT 0,
    esi NUMERIC(19,2) DEFAULT 0,
    hike NUMERIC(19,2) DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_employee_profiles_tenant_status ON employee_profiles(tenant_id, status);

CREATE TABLE attrition_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    separation_date DATE NOT NULL,
    type VARCHAR(30) NOT NULL,
    reason VARCHAR(500),
    tenure_months BIGINT NOT NULL CHECK (tenure_months >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_attrition_events_tenant_date ON attrition_events(tenant_id, separation_date);
