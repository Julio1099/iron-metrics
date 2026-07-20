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
class ExerciseApiIntegrationTest extends PostgresIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry);
    }

    @Test
    void shouldCreateExerciseUnderVersionedRoute() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                uri("/api/v1/exercises"),
                createExercise("Bench Press"),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getHeaders().getLocation().getPath()).startsWith("/api/v1/exercises/");
        assertThat(response.getBody())
                .containsEntry("name", "Bench Press")
                .containsEntry("primaryMuscleGroup", "CHEST")
                .containsEntry("movementPattern", "PUSH")
                .containsEntry("mechanicsType", "COMPOUND");
        assertThat(response.getBody().get("id")).isNotNull();
    }

    @Test
    void shouldListExercisesSortedByName() {
        postExercise("Romanian Deadlift", "HAMSTRINGS", "HINGE", "COMPOUND");
        postExercise("Cable Fly", "CHEST", "PUSH", "ISOLATION");

        ResponseEntity<List> response = restTemplate.getForEntity(uri("/api/v1/exercises"), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> names = response.getBody().stream()
                .map(item -> (String) ((Map<?, ?>) item).get("name"))
                .toList();
        assertThat(names).contains("Cable Fly", "Romanian Deadlift");
        assertThat(names.indexOf("Cable Fly")).isLessThan(names.indexOf("Romanian Deadlift"));
    }

    @Test
    void shouldGetExerciseById() {
        UUID id = postExercise("Lat Pulldown", "BACK", "PULL", "COMPOUND");

        ResponseEntity<Map> response = restTemplate.getForEntity(uri("/api/v1/exercises/" + id), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("id", id.toString())
                .containsEntry("name", "Lat Pulldown");
    }

    @Test
    void shouldUpdateExercise() {
        UUID id = postExercise("Leg Press", "QUADRICEPS", "SQUAT", "COMPOUND");

        ResponseEntity<Map> response = restTemplate.exchange(
                uri("/api/v1/exercises/" + id),
                HttpMethod.PUT,
                new HttpEntity<>(Map.of(
                        "name", "Hack Squat",
                        "primaryMuscleGroup", "QUADRICEPS",
                        "movementPattern", "SQUAT",
                        "mechanicsType", "COMPOUND"
                )),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("id", id.toString())
                .containsEntry("name", "Hack Squat")
                .containsEntry("primaryMuscleGroup", "QUADRICEPS")
                .containsEntry("movementPattern", "SQUAT");
    }

    @Test
    void shouldDeleteExercise() {
        UUID id = postExercise("Seated Calf Raise", "CALVES", "ISOLATION", "ISOLATION");

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                uri("/api/v1/exercises/" + id),
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> getResponse = restTemplate.getForEntity(
                uri("/api/v1/exercises/" + id),
                Map.class
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(getResponse.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(getResponse.getBody())
                .containsEntry("type", "about:blank")
                .containsEntry("title", "Not Found")
                .containsEntry("status", 404)
                .containsEntry("detail", "Exercise not found.");
    }

    @Test
    void shouldRejectDuplicateExerciseNames() {
        postExercise("Incline Dumbbell Press", "CHEST", "PUSH", "COMPOUND");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                uri("/api/v1/exercises"),
                createExercise("incline dumbbell press"),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody())
                .containsEntry("title", "Conflict")
                .containsEntry("status", 409)
                .containsEntry("detail", "Exercise name already exists.");
    }

    @Test
    void shouldReturnProblemDetailsForInvalidPayload() {
        Map<String, Object> payload = Map.of(
                "name", "",
                "primaryMuscleGroup", "CHEST",
                "movementPattern", "PUSH",
                "mechanicsType", "COMPOUND"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                uri("/api/v1/exercises"),
                payload,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody())
                .containsEntry("type", "about:blank")
                .containsEntry("title", "Bad Request")
                .containsEntry("status", 400);
        assertThat(((Map<?, ?>) response.getBody().get("fieldErrors")).containsKey("name"))
                .isTrue();
    }

    private UUID postExercise(
            String name,
            String primaryMuscleGroup,
            String movementPattern,
            String mechanicsType
    ) {
        Map<String, Object> payload = Map.of(
                "name", name,
                "primaryMuscleGroup", primaryMuscleGroup,
                "movementPattern", movementPattern,
                "mechanicsType", mechanicsType
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(uri("/api/v1/exercises"), payload, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        return UUID.fromString((String) response.getBody().get("id"));
    }

    private Map<String, Object> createExercise(String name) {
        return Map.of(
                "name", name,
                "primaryMuscleGroup", "CHEST",
                "movementPattern", "PUSH",
                "mechanicsType", "COMPOUND"
        );
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
