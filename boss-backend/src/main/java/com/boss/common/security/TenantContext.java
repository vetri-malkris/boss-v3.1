package com.boss.common.security;

import java.util.UUID;

/**
 * Request-scoped (thread-bound) holder for the tenant and user resolved from the
 * authenticated JWT. Populated by {@link JwtAuthenticationFilter} and cleared at the
 * end of every request. Services must read the tenant from here rather than trusting
 * a tenantId supplied by the client, which is the enforcement point for tenant isolation
 * (section 4 of the spec).
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<UUID> CURRENT_USER = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId, UUID userId) {
        CURRENT_TENANT.set(tenantId);
        CURRENT_USER.set(userId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static UUID getUserId() {
        return CURRENT_USER.get();
    }

    public static UUID requireTenantId() {
        UUID tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant resolved on the current request context");
        }
        return tenantId;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
        CURRENT_USER.remove();
    }
}
