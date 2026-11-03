package com.boss.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BossException {

    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, HttpStatus.CONFLICT, message);
    }
}
