package com.boss.common.security;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtProperties properties = new JwtProperties(
            "unit-test-signing-secret-must-be-at-least-32-bytes-long", "boss-test", 60, 14);
    private final JwtService jwtService = new JwtService(properties);

    @Test
    void generatesTokenThatParsesBackToTheSameIdentity() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Set<RoleName> roles = Set.of(RoleName.TENANT_ADMIN, RoleName.CFO);

        String token = jwtService.generateAccessToken(userId, tenantId, "cfo@acme.test", roles);
        AuthenticatedUser resolved = jwtService.parseAndValidate(token);

        assertThat(resolved.getUserId()).isEqualTo(userId);
        assertThat(resolved.getTenantId()).isEqualTo(tenantId);
        assertThat(resolved.getUsername()).isEqualTo("cfo@acme.test");
        assertThat(resolved.getRoles()).containsExactlyInAnyOrder(RoleName.TENANT_ADMIN, RoleName.CFO);
    }

    @Test
    void supportsNullTenantForPlatformLevelUsers() {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), null, "root@boss.test", Set.of(RoleName.SUPER_ADMIN));

        AuthenticatedUser resolved = jwtService.parseAndValidate(token);

        assertThat(resolved.getTenantId()).isNull();
        assertThat(resolved.getRoles()).containsExactly(RoleName.SUPER_ADMIN);
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), UUID.randomUUID(), "a@b.test", Set.of(RoleName.VIEWER));
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.parseAndValidate(tampered))
                .isInstanceOf(JwtService.InvalidTokenException.class);
    }

    @Test
    void rejectsGarbageToken() {
        assertThatThrownBy(() -> jwtService.parseAndValidate("not-a-jwt"))
                .isInstanceOf(JwtService.InvalidTokenException.class);
    }
}
