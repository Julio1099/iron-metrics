package com.ironmetrics.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class WorkoutSessionApiIntegrationTest extends PostgresIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String accessToken;
    private UUID exerciseId;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry);
    }

    @BeforeEach
    void setUp() {
        accessToken = registerUser();
        exerciseId = createExercise("Sprint 2 Bench Press " + UUID.randomUUID(), accessToken);
    }

    @Test
    void shouldCreateWorkoutSessionForAuthenticatedUser() {
        ResponseEntity<Map> response = createWorkoutSession("Upper A", accessToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation().getPath()).startsWith("/api/v1/workout-sessions/");
        assertThat(response.getBody())
                .containsEntry("title", "Upper A")
                .containsEntry("sessionDate", "2026-07-20");
        assertThat(decimal(response.getBody().get("bodyWeightKg"))).isEqualByComparingTo("82.40");
        assertThat((List<?>) response.getBody().get("sets")).isEmpty();
    }

    @Test
    void shouldAddWorkoutSetAndCalculateEstimatedOneRepMax() {
        UUID sessionId = UUID.fromString((String) createWorkoutSession("Push Strength", accessToken).getBody().get("id"));

        ResponseEntity<Map> response = addWorkoutSet(
                sessionId,
                exerciseId,
                1,
                "100.00",
                5,
                "9.0",
                accessToken
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody())
                .containsEntry("setOrder", 1)
                .containsEntry("repetitions", 5);
        assertThat(decimal(response.getBody().get("loadKg"))).isEqualByComparingTo("100.00");
        assertThat(decimal(response.getBody().get("rpe"))).isEqualByComparingTo("9.0");
        assertThat(decimal(response.getBody().get("estimatedOneRepMaxKg"))).isEqualByComparingTo("120.00");
    }

    @Test
    void shouldIgnoreEstimatedOneRepMaxWhenGuardClauseApplies() {
        UUID sessionId = UUID.fromString((String) createWorkoutSession("Warmup Work", accessToken).getBody().get("id"));

        ResponseEntity<Map> response = addWorkoutSet(
                sessionId,
                exerciseId,
                1,
                "70.00",
                8,
                "6.5",
                accessToken
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("estimatedOneRepMaxKg")).isNull();
    }

    @Test
    void shouldListOnlySessionsOwnedByAuthenticatedUser() {
        UUID currentUserSessionId = UUID.fromString((String) createWorkoutSession("Owned Session", accessToken).getBody().get("id"));
        String otherUserToken = registerUser();
        createWorkoutSession("Other User Session", otherUserToken);

        ResponseEntity<List> response = restTemplate.exchange(
                uri("/api/v1/workout-sessions"),
                HttpMethod.GET,
                new HttpEntity<>(authorizationHeaders(accessToken)),
                List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> sessionIds = response.getBody().stream()
                .map(item -> (String) ((Map<?, ?>) item).get("id"))
                .toList();
        assertThat(sessionIds).contains(currentUserSessionId.toString());
        assertThat(sessionIds).hasSize(1);
    }

    @Test
    void shouldReturnNotFoundWhenAddingSetToAnotherUsersSession() {
        String otherUserToken = registerUser();
        UUID otherUserSessionId = UUID.fromString((String) createWorkoutSession("Private Session", otherUserToken).getBody().get("id"));

        ResponseEntity<Map> response = addWorkoutSet(
                otherUserSessionId,
                exerciseId,
                1,
                "100.00",
                5,
                "9.0",
                accessToken
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).containsEntry("detail", "Workout session not found.");
    }

    private ResponseEntity<Map> createWorkoutSession(String title, String token) {
        return restTemplate.exchange(
                uri("/api/v1/workout-sessions"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "title", title,
                        "sessionDate", "2026-07-20",
                        "bodyWeightKg", "82.40"
                ), authorizationHeaders(token)),
                Map.class
        );
    }

    private ResponseEntity<Map> addWorkoutSet(
            UUID sessionId,
            UUID exerciseId,
            int setOrder,
            String loadKg,
            int repetitions,
            String rpe,
            String token
    ) {
        return restTemplate.exchange(
                uri("/api/v1/workout-sessions/" + sessionId + "/sets"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "exerciseId", exerciseId.toString(),
                        "setOrder", setOrder,
                        "loadKg", loadKg,
                        "repetitions", repetitions,
                        "rpe", rpe
                ), authorizationHeaders(token)),
                Map.class
        );
    }

    private UUID createExercise(String name, String token) {
        ResponseEntity<Map> response = restTemplate.exchange(
                uri("/api/v1/exercises"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "name", name,
                        "primaryMuscleGroup", "CHEST",
                        "movementPattern", "PUSH",
                        "mechanicsType", "COMPOUND"
                ), authorizationHeaders(token)),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString((String) response.getBody().get("id"));
    }

    private String registerUser() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                uri("/api/v1/auth/register"),
                Map.of(
                        "email", "workout-" + UUID.randomUUID() + "@ironmetrics.test",
                        "displayName", "Workout Tester",
                        "password", "Password123!"
                ),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) response.getBody().get("accessToken");
    }

    private HttpHeaders authorizationHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private BigDecimal decimal(Object value) {
        return new BigDecimal(value.toString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
