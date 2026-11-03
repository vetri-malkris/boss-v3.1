package com.boss.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessValidationException extends BossException {

    public BusinessValidationException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, HttpStatus.BAD_REQUEST, message);
    }
}
