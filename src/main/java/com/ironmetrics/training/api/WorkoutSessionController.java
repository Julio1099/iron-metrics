package com.ironmetrics.training.api;

import com.ironmetrics.training.application.WorkoutSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/workout-sessions")
@Tag(name = "Workout Sessions")
public class WorkoutSessionController {

    private final WorkoutSessionService workoutSessionService;

    public WorkoutSessionController(WorkoutSessionService workoutSessionService) {
        this.workoutSessionService = workoutSessionService;
    }

    @PostMapping
    @Operation(summary = "Create a workout session")
    public ResponseEntity<WorkoutSessionResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateWorkoutSessionRequest request
    ) {
        WorkoutSessionResponse response = workoutSessionService.create(currentUserId(jwt), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "List workout sessions owned by the authenticated user")
    public List<WorkoutSessionResponse> findAll(@AuthenticationPrincipal Jwt jwt) {
        return workoutSessionService.findAll(currentUserId(jwt));
    }

    @PostMapping("/{id}/sets")
    @Operation(summary = "Add a workout set and calculate estimated 1RM when eligible")
    public ResponseEntity<WorkoutSetResponse> addSet(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody CreateWorkoutSetRequest request
    ) {
        WorkoutSetResponse response = workoutSessionService.addSet(currentUserId(jwt), id, request);

        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{setId}")
                .buildAndExpand(response.id())
                .toUri()).body(response);
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
