package com.ironmetrics.training.application;

import com.ironmetrics.training.api.CreateExerciseRequest;
import com.ironmetrics.training.api.UpdateExerciseRequest;
import com.ironmetrics.training.domain.Exercise;
import com.ironmetrics.training.infrastructure.ExerciseRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExerciseService {

    private static final Sort SORT_BY_NAME = Sort.by(Sort.Direction.ASC, "name");

    private final ExerciseRepository exerciseRepository;

    public ExerciseService(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    @Transactional
    public Exercise create(CreateExerciseRequest request) {
        ensureNameIsAvailable(request.name(), null);

        Exercise exercise = new Exercise(
                request.name(),
                request.primaryMuscleGroup(),
                request.movementPattern(),
                request.mechanicsType()
        );

        return exerciseRepository.save(exercise);
    }

    @Transactional(readOnly = true)
    public List<Exercise> findAll() {
        return exerciseRepository.findAll(SORT_BY_NAME);
    }

    @Transactional(readOnly = true)
    public Exercise findById(UUID id) {
        return exerciseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found."));
    }

    @Transactional
    public Exercise update(UUID id, UpdateExerciseRequest request) {
        Exercise exercise = findById(id);
        ensureNameIsAvailable(request.name(), id);

        exercise.update(
                request.name(),
                request.primaryMuscleGroup(),
                request.movementPattern(),
                request.mechanicsType()
        );

        return exercise;
    }

    @Transactional
    public void delete(UUID id) {
        Exercise exercise = findById(id);
        exerciseRepository.delete(exercise);
    }

    private void ensureNameIsAvailable(String name, UUID currentExerciseId) {
        exerciseRepository.findByNameIgnoreCase(name.trim())
                .filter(existing -> !existing.getId().equals(currentExerciseId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Exercise name already exists.");
                });
    }
}
