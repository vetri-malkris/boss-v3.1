package com.boss.common.security;

import com.boss.common.logging.LogFields;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Resolves the Bearer token on every request into an {@link AuthenticatedUser}, populates the
 * Spring Security context and {@link TenantContext}, and tears both down once the request
 * completes. This is the single enforcement point tying every downstream service call back
 * to a specific tenant.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (token != null) {
                try {
                    AuthenticatedUser user = jwtService.parseAndValidate(token);

                    var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    TenantContext.set(user.getTenantId(), user.getUserId());
                    // SUPER_ADMIN accounts have no tenant (see User.tenantId javadoc) - leave
                    // the MDC key unset rather than NPE, same as the pre-authentication case.
                    if (user.getTenantId() != null) {
                        MDC.put(LogFields.TENANT_ID, user.getTenantId().toString());
                    }
                    MDC.put(LogFields.USER_ID, user.getUserId().toString());
                } catch (JwtService.InvalidTokenException e) {
                    // Leave the request unauthenticated; Spring Security's entry point
                    // will reject it with 401 if the target endpoint requires authentication.
                    SecurityContextHolder.clearContext();
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove(LogFields.TENANT_ID);
            MDC.remove(LogFields.USER_ID);
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
