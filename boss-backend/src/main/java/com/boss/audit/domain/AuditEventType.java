package com.boss.audit.domain;

/**
 * Enumerates the audit-worthy events across the platform (section 21 of the spec).
 * New event types are appended here as later phases introduce new capabilities.
 */
public enum AuditEventType {
    LOGIN,
    LOGIN_FAILED,
    LOGOUT,
    DATA_UPLOAD,
    DATA_DELETE,
    REPORT_GENERATED,
    AI_QUERY,
    SCENARIO_RUN,
    RECOMMENDATION_CREATED,
    ACTION_CREATED,
    ACTION_APPROVED,
    ACTION_REJECTED,
    ACTION_EXECUTED,
    ACTION_COMPLETED,
    ACTION_CANCELLED,
    OUTCOME_RECORDED,
    SETTINGS_CHANGED,
    TENANT_CREATED,
    COMPANY_CREATED,
    USER_CREATED,
    USER_ROLE_CHANGED,
    SNAPSHOT_REVISED,
    RECORD_UPDATED,
    RECORD_VOIDED
}
