package com.ironmetrics.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiIntegrationTest extends PostgresIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry);
    }

    @Test
    void shouldPublishVersionedOpenApiContractWithJwtSecurity() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                uri("/api/v1/v3/api-docs"),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = response.getBody();
        assertThat(((Map<?, ?>) body.get("info")).get("title")).isEqualTo("Iron Metrics API");
        assertThat(((Map<?, ?>) body.get("info")).get("version")).isEqualTo("v1");
        assertThat((List<?>) body.get("servers"))
                .anySatisfy(server -> assertThat(((Map<?, ?>) server).get("url")).isEqualTo("/api/v1"));

        Map<?, ?> paths = (Map<?, ?>) body.get("paths");
        List<String> pathNames = paths.keySet().stream()
                .map(Object::toString)
                .toList();
        assertThat(pathNames).contains(
                "/auth/register",
                "/auth/login",
                "/exercises",
                "/workout-sessions",
                "/workout-sessions/{id}/sets"
        );

        Map<?, ?> securitySchemes = (Map<?, ?>) ((Map<?, ?>) body.get("components")).get("securitySchemes");
        Map<?, ?> bearerAuth = (Map<?, ?>) securitySchemes.get("bearerAuth");
        assertThat(bearerAuth.get("type")).isEqualTo("http");
        assertThat(bearerAuth.get("scheme")).isEqualTo("bearer");
        assertThat(bearerAuth.get("bearerFormat")).isEqualTo("JWT");

        Map<?, ?> registerOperation = (Map<?, ?>) ((Map<?, ?>) paths.get("/auth/register")).get("post");
        assertThat(registerOperation.get("security")).isEqualTo(List.of());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
