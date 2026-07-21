package com.ironmetrics.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.ironmetrics.analytics.application.AnalyticsRefreshService;
import java.math.BigDecimal;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AnalyticsApiIntegrationTest extends PostgresIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AnalyticsRefreshService analyticsRefreshService;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry);
    }

    @Test
    void shouldExposeAuthenticatedReadOnlyAnalyticsFromAnalyticsSchema() {
        String currentUserToken = registerUser();
        String exerciseName = "Sprint 5 Bench Press " + UUID.randomUUID();
        UUID exerciseId = createExercise(exerciseName, currentUserToken);
        UUID sessionId = createWorkoutSession("Analytics Push Day", "2026-07-21", "82.40", currentUserToken);
        addWorkoutSet(sessionId, exerciseId, 1, "100.00", 5, "9.0", currentUserToken);

        String otherUserToken = registerUser();
        UUID otherSessionId = createWorkoutSession("Other Analytics Day", "2026-07-21", "95.00", otherUserToken);
        addWorkoutSet(otherSessionId, exerciseId, 1, "200.00", 5, "9.0", otherUserToken);

        analyticsRefreshService.refresh();

        ResponseEntity<List> volumeResponse = getAnalytics(
                "/api/v1/analytics/training-volume/daily?from=2026-07-21&to=2026-07-21",
                currentUserToken
        );
        ResponseEntity<List> progressionResponse = getAnalytics(
                "/api/v1/analytics/exercise-progressions/daily?from=2026-07-21&to=2026-07-21",
                currentUserToken
        );
        ResponseEntity<List> bodyWeightResponse = getAnalytics(
                "/api/v1/analytics/body-weight/daily?from=2026-07-21&to=2026-07-21",
                currentUserToken
        );

        assertThat(volumeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(progressionResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bodyWeightResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<?, ?> volume = onlyItem(volumeResponse.getBody());
        assertThat(volume.get("trainingDate")).isEqualTo("2026-07-21");
        assertThat(volume.get("totalSets")).isEqualTo(1);
        assertThat(volume.get("totalRepetitions")).isEqualTo(5);
        assertThat(decimal(volume.get("totalVolumeKg"))).isEqualByComparingTo("500.00");

        Map<?, ?> progression = onlyItem(progressionResponse.getBody());
        assertThat(progression.get("trainingDate")).isEqualTo("2026-07-21");
        assertThat(progression.get("exerciseId")).isEqualTo(exerciseId.toString());
        assertThat(progression.get("exerciseName")).isEqualTo(exerciseName);
        assertThat(decimal(progression.get("topLoadKg"))).isEqualByComparingTo("100.00");

        Map<?, ?> bodyWeight = onlyItem(bodyWeightResponse.getBody());
        assertThat(bodyWeight.get("measurementDate")).isEqualTo("2026-07-21");
        assertThat(decimal(bodyWeight.get("bodyWeightKg"))).isEqualByComparingTo("82.40");
    }

    @Test
    void shouldRequireAuthenticationForAnalyticsEndpoints() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                uri("/api/v1/analytics/training-volume/daily"),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("detail", "Authentication is required.");
    }

    private ResponseEntity<List> getAnalytics(String path, String token) {
        return restTemplate.exchange(
                uri(path),
                HttpMethod.GET,
                new HttpEntity<>(authorizationHeaders(token)),
                List.class
        );
    }

    private String registerUser() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                uri("/api/v1/auth/register"),
                Map.of(
                        "email", "analytics-api-" + UUID.randomUUID() + "@ironmetrics.test",
                        "displayName", "Analytics API Tester",
                        "password", "Password123!"
                ),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) response.getBody().get("accessToken");
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

    private UUID createWorkoutSession(String title, String sessionDate, String bodyWeightKg, String token) {
        ResponseEntity<Map> response = restTemplate.exchange(
                uri("/api/v1/workout-sessions"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "title", title,
                        "sessionDate", sessionDate,
                        "bodyWeightKg", bodyWeightKg
                ), authorizationHeaders(token)),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString((String) response.getBody().get("id"));
    }

    private void addWorkoutSet(
            UUID sessionId,
            UUID exerciseId,
            int setOrder,
            String loadKg,
            int repetitions,
            String rpe,
            String token
    ) {
        ResponseEntity<Map> response = restTemplate.exchange(
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
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private Map<?, ?> onlyItem(List<?> values) {
        assertThat(values).hasSize(1);
        return (Map<?, ?>) values.getFirst();
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
