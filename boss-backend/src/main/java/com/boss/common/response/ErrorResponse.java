package com.boss.common.response;

import com.boss.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Consistent error envelope returned by {@code GlobalExceptionHandler} for every failed request.
 * Never carries stack traces or internal exception details.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        ErrorCode code,
        String message,
        String correlationId,
        String path,
        List<FieldError> fieldErrors
) {

    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(int status, ErrorCode code, String message, String correlationId, String path) {
        return new ErrorResponse(Instant.now(), status, code, message, correlationId, path, null);
    }

    public static ErrorResponse ofFieldErrors(int status, ErrorCode code, String message, String correlationId,
                                               String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, code, message, correlationId, path, fieldErrors);
    }
}
