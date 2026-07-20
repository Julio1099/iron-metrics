package com.ironmetrics.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "iron-metrics.rate-limit.auth.capacity=2",
                "iron-metrics.rate-limit.auth.refill-tokens=2",
                "iron-metrics.rate-limit.auth.refill-period=PT1M",
                "iron-metrics.rate-limit.authenticated.capacity=2",
                "iron-metrics.rate-limit.authenticated.refill-tokens=2",
                "iron-metrics.rate-limit.authenticated.refill-period=PT1M"
        }
)
class RateLimitIntegrationTest extends PostgresIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry);
    }

    @Test
    void shouldRateLimitAuthenticationRoutesByClientIp() {
        Map<String, String> payload = Map.of(
                "email", "missing-" + UUID.randomUUID() + "@ironmetrics.test",
                "password", "wrong-password"
        );
        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, authHeaders("203.0.113.10"));

        restTemplate.exchange(uri("/api/v1/auth/login"), HttpMethod.POST, request, Map.class);
        restTemplate.exchange(uri("/api/v1/auth/login"), HttpMethod.POST, request, Map.class);

        ResponseEntity<Map> response = restTemplate.exchange(
                uri("/api/v1/auth/login"),
                HttpMethod.POST,
                request,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody())
                .containsEntry("title", "Too Many Requests")
                .containsEntry("status", 429)
                .containsEntry("detail", "Rate limit exceeded.");
    }

    @Test
    void shouldRateLimitAuthenticatedRoutesByUser() {
        String token = registerUser();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        restTemplate.exchange(uri("/api/v1/exercises"), HttpMethod.GET, new HttpEntity<>(headers), List.class);
        restTemplate.exchange(uri("/api/v1/exercises"), HttpMethod.GET, new HttpEntity<>(headers), List.class);

        ResponseEntity<Map> response = restTemplate.exchange(
                uri("/api/v1/exercises"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).containsEntry("detail", "Rate limit exceeded.");
    }

    private String registerUser() {
        ResponseEntity<Map> response = restTemplate.exchange(
                uri("/api/v1/auth/register"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "email", "rate-limit-" + UUID.randomUUID() + "@ironmetrics.test",
                        "displayName", "Rate Limit Tester",
                        "password", "Password123!"
                ), authHeaders("203.0.113.20")),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) response.getBody().get("accessToken");
    }

    private HttpHeaders authHeaders(String clientIp) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-For", clientIp);
        return headers;
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
