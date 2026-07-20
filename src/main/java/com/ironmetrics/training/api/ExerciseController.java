package com.ironmetrics.training.api;

import com.ironmetrics.training.application.ExerciseService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @PostMapping
    public ResponseEntity<ExerciseResponse> create(@Valid @RequestBody CreateExerciseRequest request) {
        ExerciseResponse response = ExerciseResponse.from(exerciseService.create(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<ExerciseResponse> findAll() {
        return exerciseService.findAll().stream()
                .map(ExerciseResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ExerciseResponse findById(@PathVariable UUID id) {
        return ExerciseResponse.from(exerciseService.findById(id));
    }

    @PutMapping("/{id}")
    public ExerciseResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExerciseRequest request
    ) {
        return ExerciseResponse.from(exerciseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        exerciseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
