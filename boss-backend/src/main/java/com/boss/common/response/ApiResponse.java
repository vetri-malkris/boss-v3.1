package com.boss.common.response;

import java.time.Instant;

/**
 * Generic success envelope used by controllers so every endpoint returns a predictable shape.
 */
public record ApiResponse<T>(T data, Instant timestamp, String correlationId) {

    public static <T> ApiResponse<T> of(T data, String correlationId) {
        return new ApiResponse<>(data, Instant.now(), correlationId);
    }
}
