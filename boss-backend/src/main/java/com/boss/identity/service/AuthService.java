package com.boss.identity.service;

import com.boss.audit.domain.AuditEventType;
import com.boss.audit.service.AuditService;
import com.boss.common.exception.AuthenticationFailedException;
import com.boss.common.security.JwtProperties;
import com.boss.common.security.JwtService;
import com.boss.common.security.RoleName;
import com.boss.identity.domain.User;
import com.boss.identity.dto.LoginRequest;
import com.boss.identity.dto.LoginResponse;
import com.boss.identity.dto.RegisterTenantRequest;
import com.boss.identity.repository.UserRepository;
import com.boss.tenant.domain.Tenant;
import com.boss.tenant.dto.CreateTenantRequest;
import com.boss.tenant.service.TenantService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Authenticates users and orchestrates tenant onboarding (SUPER_ADMIN-only - see
 * {@code AuthController#register}). The financial engine, AI orchestrator and every
 * other module trust the {@code AuthenticatedUser} this produces without re-checking
 * credentials.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuditService auditService;
    private final TenantService tenantService;
    private final UserService userService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                        JwtProperties jwtProperties, AuditService auditService, TenantService tenantService,
                        UserService userService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.auditService = auditService;
        this.tenantService = tenantService;
        this.userService = userService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .filter(User::isActive)
                .orElseGet(() -> {
                    auditService.record(null, null, AuditEventType.LOGIN_FAILED, "User", request.email(),
                            "Failed login attempt");
                    throw new AuthenticationFailedException("Invalid email or password");
                });

        auditService.record(user.getTenantId(), user.getId(), AuditEventType.LOGIN, "User", user.getId().toString(),
                "User logged in");
        return issueTokenFor(user);
    }

    @Transactional
    public LoginResponse registerTenant(RegisterTenantRequest request) {
        Tenant tenant = tenantService.createTenant(new CreateTenantRequest(request.tenantName(), request.tenantSlug()));
        // Every client's first user is their CEO (section: CEO is the senior position in
        // a tenant) - CEO carries the same create/manage-user permissions TENANT_ADMIN
        // has, so this one account can run the company's account from day one.
        User admin = userService.createUser(tenant.getId(), request.adminEmail(), request.adminPassword(),
                request.adminFullName(), Set.of(RoleName.CEO));
        auditService.record(tenant.getId(), admin.getId(), AuditEventType.LOGIN, "User", admin.getId().toString(),
                "Tenant CEO account created during onboarding");
        return issueTokenFor(admin);
    }

    private LoginResponse issueTokenFor(User user) {
        String token = jwtService.generateAccessToken(user.getId(), user.getTenantId(), user.getEmail(), user.getRoles());
        long expiresInSeconds = jwtProperties.accessTokenTtlMinutes() * 60;
        return LoginResponse.bearer(token, expiresInSeconds, UserService.toDto(user));
    }
}
