package com.ironmetrics.training.infrastructure;

import com.ironmetrics.training.domain.WorkoutSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {

    Optional<WorkoutSession> findByIdAndUserAccountId(UUID id, UUID userAccountId);

    @EntityGraph(attributePaths = {"sets", "sets.exercise"})
    List<WorkoutSession> findDistinctByUserAccountIdOrderBySessionDateDescCreatedAtDesc(UUID userAccountId);
}
