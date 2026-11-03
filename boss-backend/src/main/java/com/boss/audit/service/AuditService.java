package com.boss.audit.service;

import com.boss.audit.domain.AuditEventType;
import com.boss.audit.domain.AuditLog;
import com.boss.audit.repository.AuditLogRepository;
import com.boss.common.audit.AuditContextSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Writes immutable audit entries. This is the only class permitted to insert into
 * {@code audit_logs}; there is deliberately no update/delete path (section 21).
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(AuditEventType eventType, String entityType, String entityId, String description) {
        AuditContextSnapshot context = AuditContextSnapshot.capture();
        record(context.tenantId(), context.userId(), eventType, entityType, entityId, description);
    }

    @Transactional
    public void record(UUID tenantId, UUID userId, AuditEventType eventType, String entityType, String entityId,
                        String description) {
        String correlationId = AuditContextSnapshot.capture().correlationId();
        auditLogRepository.save(new AuditLog(tenantId, userId, eventType, entityType, entityId, description, correlationId));
    }
}
