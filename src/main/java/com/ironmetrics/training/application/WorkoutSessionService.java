package com.ironmetrics.training.application;

import com.ironmetrics.training.api.CreateWorkoutSessionRequest;
import com.ironmetrics.training.api.CreateWorkoutSetRequest;
import com.ironmetrics.training.api.WorkoutSessionResponse;
import com.ironmetrics.training.api.WorkoutSetResponse;
import com.ironmetrics.training.domain.EstimatedOneRepMaxCalculator;
import com.ironmetrics.training.domain.Exercise;
import com.ironmetrics.training.domain.WorkoutSession;
import com.ironmetrics.training.domain.WorkoutSet;
import com.ironmetrics.training.infrastructure.ExerciseRepository;
import com.ironmetrics.training.infrastructure.WorkoutSessionRepository;
import com.ironmetrics.training.infrastructure.WorkoutSetRepository;
import com.ironmetrics.users.domain.UserAccount;
import com.ironmetrics.users.infrastructure.UserAccountRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkoutSessionService {

    private final WorkoutSessionRepository workoutSessionRepository;
    private final WorkoutSetRepository workoutSetRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserAccountRepository userAccountRepository;
    private final EstimatedOneRepMaxCalculator estimatedOneRepMaxCalculator;

    public WorkoutSessionService(
            WorkoutSessionRepository workoutSessionRepository,
            WorkoutSetRepository workoutSetRepository,
            ExerciseRepository exerciseRepository,
            UserAccountRepository userAccountRepository,
            EstimatedOneRepMaxCalculator estimatedOneRepMaxCalculator
    ) {
        this.workoutSessionRepository = workoutSessionRepository;
        this.workoutSetRepository = workoutSetRepository;
        this.exerciseRepository = exerciseRepository;
        this.userAccountRepository = userAccountRepository;
        this.estimatedOneRepMaxCalculator = estimatedOneRepMaxCalculator;
    }

    @Transactional
    public WorkoutSessionResponse create(UUID userId, CreateWorkoutSessionRequest request) {
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found."));

        WorkoutSession workoutSession = workoutSessionRepository.save(new WorkoutSession(
                userAccount,
                request.sessionDate(),
                request.title(),
                request.bodyWeightKg()
        ));

        return WorkoutSessionResponse.from(workoutSession);
    }

    @Transactional(readOnly = true)
    public List<WorkoutSessionResponse> findAll(UUID userId) {
        return workoutSessionRepository.findDistinctByUserAccountIdOrderBySessionDateDescCreatedAtDesc(userId)
                .stream()
                .map(WorkoutSessionResponse::from)
                .toList();
    }

    @Transactional
    public WorkoutSetResponse addSet(UUID userId, UUID workoutSessionId, CreateWorkoutSetRequest request) {
        WorkoutSession workoutSession = workoutSessionRepository.findByIdAndUserAccountId(workoutSessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout session not found."));
        Exercise exercise = exerciseRepository.findById(request.exerciseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found."));

        BigDecimal estimatedOneRepMaxKg = estimatedOneRepMaxCalculator
                .calculate(request.loadKg(), request.repetitions(), request.rpe())
                .map(estimatedOneRepMax -> estimatedOneRepMax.value())
                .orElse(null);

        WorkoutSet workoutSet = workoutSetRepository.save(new WorkoutSet(
                workoutSession,
                exercise,
                request.setOrder(),
                request.loadKg(),
                request.repetitions(),
                request.rpe(),
                estimatedOneRepMaxKg
        ));

        return WorkoutSetResponse.from(workoutSet);
    }
}
