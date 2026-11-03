package com.boss.common.exception;

/**
 * Stable machine-readable error codes returned to API clients.
 * Codes must never change once released; they are part of the API contract.
 */
public enum ErrorCode {
    RESOURCE_NOT_FOUND,
    DUPLICATE_RESOURCE,
    VALIDATION_ERROR,
    BUSINESS_RULE_VIOLATION,
    ACCESS_DENIED,
    TENANT_MISMATCH,
    AUTHENTICATION_FAILED,
    INVALID_FILE,
    UNSUPPORTED_OPERATION,
    INTERNAL_ERROR
}
