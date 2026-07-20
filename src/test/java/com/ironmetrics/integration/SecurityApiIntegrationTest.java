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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityApiIntegrationTest extends PostgresIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry);
    }

    @Test
    void shouldRegisterUserAndReturnJwt() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                uri("/api/v1/auth/register"),
                registrationPayload(uniqueEmail(), "Julio Tester"),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody())
                .containsEntry("tokenType", "Bearer")
                .containsKey("accessToken")
                .containsKey("expiresInSeconds");
        assertThat((String) response.getBody().get("accessToken")).isNotBlank();
        assertThat(((Map<?, ?>) response.getBody().get("user")).get("displayName"))
                .isEqualTo("Julio Tester");
    }

    @Test
    void shouldLoginUserAndReturnJwt() {
        String email = uniqueEmail();
        register(email, "Password123!");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                uri("/api/v1/auth/login"),
                Map.of("email", email, "password", "Password123!"),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String) response.getBody().get("accessToken")).isNotBlank();
        assertThat(response.getBody()).containsEntry("tokenType", "Bearer");
    }

    @Test
    void shouldRejectInvalidCredentialsAsProblemDetail() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                uri("/api/v1/auth/login"),
                Map.of("email", uniqueEmail(), "password", "wrong-password"),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody())
                .containsEntry("title", "Unauthorized")
                .containsEntry("status", 401)
                .containsEntry("detail", "Invalid email or password.");
    }

    @Test
    void shouldProtectBusinessRoutesWhenTokenIsMissing() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                uri("/api/v1/exercises"),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody())
                .containsEntry("title", "Unauthorized")
                .containsEntry("status", 401)
                .containsEntry("detail", "Authentication is required.");
    }

    @Test
    void shouldAllowBusinessRoutesWhenTokenIsValid() {
        String token = register(uniqueEmail(), "Password123!");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<List> response = restTemplate.exchange(
                uri("/api/v1/exercises"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String register(String email, String password) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                uri("/api/v1/auth/register"),
                registrationPayload(email, "Authenticated Tester", password),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) response.getBody().get("accessToken");
    }

    private Map<String, Object> registrationPayload(String email, String displayName) {
        return registrationPayload(email, displayName, "Password123!");
    }

    private Map<String, Object> registrationPayload(String email, String displayName, String password) {
        return Map.of(
                "email", email,
                "displayName", displayName,
                "password", password
        );
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@ironmetrics.test";
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
