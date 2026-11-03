package com.boss.common.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationFailedException extends BossException {

    public AuthenticationFailedException(String message) {
        super(ErrorCode.AUTHENTICATION_FAILED, HttpStatus.UNAUTHORIZED, message);
    }
}
