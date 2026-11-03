package com.boss.common.security;

/**
 * Platform-wide role vocabulary (section 4 of the spec). Roles are assigned per user
 * and, other than SUPER_ADMIN, are always scoped to the user's tenant.
 */
public enum RoleName {
    SUPER_ADMIN,
    TENANT_ADMIN,
    CEO,
    CFO,
    FINANCE_MANAGER,
    MANAGER,
    EMPLOYEE,
    VIEWER
}
