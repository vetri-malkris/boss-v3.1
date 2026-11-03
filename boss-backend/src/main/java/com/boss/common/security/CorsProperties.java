package com.boss.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "boss.security.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
