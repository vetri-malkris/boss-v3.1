package com.boss.common.logging;

/**
 * MDC key constants used across the request lifecycle for structured logging.
 * See section 20 of the platform spec: every log line must be traceable via correlationId.
 */
public final class LogFields {

    public static final String CORRELATION_ID = "correlationId";
    public static final String REQUEST_ID = "requestId";
    public static final String TENANT_ID = "tenantId";
    public static final String USER_ID = "userId";
    public static final String OPERATION = "operation";

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private LogFields() {
    }
}
