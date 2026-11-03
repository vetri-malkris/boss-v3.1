package com.boss.common.audit;

import com.boss.common.logging.LogFields;
import com.boss.common.security.SecurityUtils;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Captures the who/where of "right now" (tenant, user, correlation id) so the
 * {@code audit} module's {@code AuditService} never has to re-derive request context itself.
 */
public record AuditContextSnapshot(UUID tenantId, UUID userId, String correlationId) {

    public static AuditContextSnapshot capture() {
        return SecurityUtils.currentUser()
                .map(user -> new AuditContextSnapshot(user.getTenantId(), user.getUserId(), MDC.get(LogFields.CORRELATION_ID)))
                .orElseGet(() -> new AuditContextSnapshot(null, null, MDC.get(LogFields.CORRELATION_ID)));
    }
}
