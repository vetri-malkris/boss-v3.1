package com.boss.audit.domain;

import com.boss.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Immutable record of a security- or business-significant event. Rows are only ever
 * inserted, never updated or deleted, by application code - see {@code AuditService}.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "tenant_id")
    private UUID tenantId;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private AuditEventType eventType;

    @Column(name = "entity_type", length = 64)
    private String entityType;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    protected AuditLog() {
    }

    public AuditLog(UUID tenantId, UUID userId, AuditEventType eventType, String entityType, String entityId,
                     String description, String correlationId) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
        this.correlationId = correlationId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getDescription() {
        return description;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
