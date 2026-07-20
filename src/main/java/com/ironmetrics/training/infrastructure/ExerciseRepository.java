package com.ironmetrics.training.infrastructure;

import com.ironmetrics.training.domain.Exercise;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    Optional<Exercise> findByNameIgnoreCase(String name);
}
