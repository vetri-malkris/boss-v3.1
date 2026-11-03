package com.boss.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "boss.security.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long accessTokenTtlMinutes,
        long refreshTokenTtlDays
) {
}
