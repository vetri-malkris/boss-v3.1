package com.boss.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BossException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }

    public static ResourceNotFoundException of(String resourceType, Object id) {
        return new ResourceNotFoundException(resourceType + " not found: " + id);
    }
}
