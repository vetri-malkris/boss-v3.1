package com.boss.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Issues and validates access tokens. This is the only place in the application that
 * touches JWT internals; everything else works against {@link AuthenticatedUser}.
 *
 * <p>The architecture is OIDC-ready: swapping this out for an external identity provider
 * (Keycloak/Auth0/Azure AD) only requires replacing this service and
 * {@link JwtAuthenticationFilter}'s token source - callers of {@code TenantContext} and
 * {@code AuthenticatedUser} are unaffected.
 */
@Service
@EnableConfigurationProperties(JwtProperties.class)
public class JwtService {

    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_EMAIL = "email";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, UUID tenantId, String email, Set<RoleName> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.accessTokenTtlMinutes() * 60);
        List<String> roleNames = roles.stream().map(Enum::name).collect(Collectors.toList());

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim(CLAIM_TENANT_ID, tenantId == null ? null : tenantId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLES, roleNames)
                .signWith(signingKey)
                .compact();
    }

    public AuthenticatedUser parseAndValidate(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid or expired access token");
        }

        UUID userId = UUID.fromString(claims.getSubject());
        String tenantIdClaim = claims.get(CLAIM_TENANT_ID, String.class);
        UUID tenantId = tenantIdClaim == null ? null : UUID.fromString(tenantIdClaim);
        String email = claims.get(CLAIM_EMAIL, String.class);

        @SuppressWarnings("unchecked")
        List<String> roleClaims = claims.get(CLAIM_ROLES, List.class);
        Set<RoleName> roles = roleClaims == null
                ? Set.of()
                : roleClaims.stream().map(RoleName::valueOf).collect(Collectors.toSet());

        return new AuthenticatedUser(userId, tenantId, email, roles, true);
    }

    public static final class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}
