package com.boss.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidFileException extends BossException {

    public InvalidFileException(String message) {
        super(ErrorCode.INVALID_FILE, HttpStatus.BAD_REQUEST, message);
    }
}
