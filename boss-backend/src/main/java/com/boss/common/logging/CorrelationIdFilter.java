package com.boss.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns (or propagates) a correlation ID for every inbound request and makes it available
 * via MDC for the lifetime of the request, and via a response header for client-side tracing.
 */
@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = firstNonBlank(request.getHeader(LogFields.CORRELATION_ID_HEADER), UUID.randomUUID().toString());
        String requestId = UUID.randomUUID().toString();

        MDC.put(LogFields.CORRELATION_ID, correlationId);
        MDC.put(LogFields.REQUEST_ID, requestId);
        response.setHeader(LogFields.CORRELATION_ID_HEADER, correlationId);
        response.setHeader(LogFields.REQUEST_ID_HEADER, requestId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(LogFields.CORRELATION_ID);
            MDC.remove(LogFields.REQUEST_ID);
        }
    }

    private static String firstNonBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
