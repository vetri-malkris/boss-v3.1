package com.boss.identity.service;

import com.boss.audit.service.AuditService;
import com.boss.common.exception.AuthenticationFailedException;
import com.boss.common.security.JwtProperties;
import com.boss.common.security.JwtService;
import com.boss.common.security.RoleName;
import com.boss.identity.domain.User;
import com.boss.identity.dto.LoginRequest;
import com.boss.identity.dto.LoginResponse;
import com.boss.identity.repository.UserRepository;
import com.boss.tenant.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuditService auditService;
    @Mock
    private TenantService tenantService;
    @Mock
    private UserService userService;

    private AuthService authService;

    private final JwtProperties jwtProperties = new JwtProperties("secret", "boss-test", 60, 14);

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, jwtProperties, auditService,
                tenantService, userService);
    }

    @Test
    void loginSucceedsForActiveUserWithMatchingPassword() {
        UUID tenantId = UUID.randomUUID();
        User user = new User(tenantId, "cfo@acme.test", "hashed", "CFO Person", Set.of(RoleName.CFO));
        user.setId(UUID.randomUUID());
        when(userRepository.findByEmailIgnoreCase("cfo@acme.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(any(), any(), anyString(), any())).thenReturn("signed-jwt");

        LoginResponse response = authService.login(new LoginRequest("cfo@acme.test", "secret123"));

        assertThat(response.accessToken()).isEqualTo("signed-jwt");
        assertThat(response.user().email()).isEqualTo("cfo@acme.test");
        verify(auditService).record(eq(tenantId), any(), any(), eq("User"), anyString(), anyString());
    }

    @Test
    void loginFailsForWrongPasswordAndRecordsFailedAttempt() {
        User user = new User(UUID.randomUUID(), "cfo@acme.test", "hashed", "CFO Person", Set.of(RoleName.CFO));
        when(userRepository.findByEmailIgnoreCase("cfo@acme.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("cfo@acme.test", "wrong")))
                .isInstanceOf(AuthenticationFailedException.class);

        verify(auditService).record(isNull(), isNull(), any(), eq("User"), eq("cfo@acme.test"), anyString());
    }

    @Test
    void loginFailsWhenNoSuchUserExists() {
        when(userRepository.findByEmailIgnoreCase("ghost@acme.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@acme.test", "whatever")))
                .isInstanceOf(AuthenticationFailedException.class);
    }
}
