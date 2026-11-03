package com.boss.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised whenever a request attempts to read or mutate a resource that belongs
 * to a different tenant than the one resolved from the authenticated principal.
 */
public class TenantAccessDeniedException extends BossException {

    public TenantAccessDeniedException(String message) {
        super(ErrorCode.TENANT_MISMATCH, HttpStatus.FORBIDDEN, message);
    }

    public static TenantAccessDeniedException forResource(String resourceType, Object id) {
        return new TenantAccessDeniedException(
                "Access denied to " + resourceType + " " + id + ": resource belongs to a different tenant");
    }
}
