package com.boss.identity.controller;

import com.boss.common.logging.LogFields;
import com.boss.common.response.ApiResponse;
import com.boss.identity.dto.LoginRequest;
import com.boss.identity.dto.LoginResponse;
import com.boss.identity.dto.RegisterTenantRequest;
import com.boss.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.of(authService.login(request), MDC.get(LogFields.CORRELATION_ID));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Onboard a new client: creates a tenant and its first CEO user (SUPER_ADMIN only)")
    public ResponseEntity<ApiResponse<LoginResponse>> register(@Valid @RequestBody RegisterTenantRequest request) {
        LoginResponse response = authService.registerTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response, MDC.get(LogFields.CORRELATION_ID)));
    }
}
