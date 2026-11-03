package com.boss.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for all domain/application exceptions in BOSS.
 * Carries an {@link ErrorCode} and the HTTP status the GlobalExceptionHandler should map it to.
 */
public class BossException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public BossException(ErrorCode errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
