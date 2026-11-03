package com.boss.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Base for every entity that is owned by a tenant. Every query against a subclass
 * must be scoped by {@code tenantId} at the repository or service layer -
 * see {@code common.security.TenantContext} and the module-level services.
 */
@MappedSuperclass
public abstract class TenantScopedEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }
}
