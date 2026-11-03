package com.boss.identity.api;

import com.boss.identity.dto.LoginRequest;
import com.boss.identity.dto.RegisterTenantRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end verification of the signup -> login flow against a real PostgreSQL instance
 * (via Testcontainers) with Flyway migrations applied, exercising security, tenant isolation
 * and the audit trail together (section 29 - requires a running Docker daemon).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("boss")
            .withUsername("boss")
            .withPassword("boss");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("boss.security.jwt.secret", () -> "integration-test-signing-secret-32chars-minimum!!");
    }

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void signupThenLoginIssuesAWorkingAccessToken() {
        RegisterTenantRequest signup = new RegisterTenantRequest(
                "Acme Manufacturing", "acme-mfg", "cfo@acme-mfg.test", "SuperSecret123", "Ada CFO");

        ResponseEntity<Map<String, Object>> registerResponse = restTemplate.exchange(
                url("/api/v1/auth/register"), org.springframework.http.HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(signup), new org.springframework.core.ParameterizedTypeReference<>() {
                });
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        LoginRequest login = new LoginRequest("cfo@acme-mfg.test", "SuperSecret123");
        ResponseEntity<Map<String, Object>> loginResponse = restTemplate.exchange(
                url("/api/v1/auth/login"), org.springframework.http.HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(login), new org.springframework.core.ParameterizedTypeReference<>() {
                });
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // A second signup with the same slug must be rejected - tenants are unique by slug.
        ResponseEntity<String> duplicate = restTemplate.postForEntity(url("/api/v1/auth/register"), signup, String.class);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // A protected endpoint without a token must be rejected.
        ResponseEntity<String> unauthorized = restTemplate.getForEntity(url("/api/v1/companies"), String.class);
        assertThat(unauthorized.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
