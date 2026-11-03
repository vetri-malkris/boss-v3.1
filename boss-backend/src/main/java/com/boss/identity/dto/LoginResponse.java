package com.boss.identity.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserSummaryDto user
) {
    public static LoginResponse bearer(String accessToken, long expiresInSeconds, UserSummaryDto user) {
        return new LoginResponse(accessToken, "Bearer", expiresInSeconds, user);
    }
}
