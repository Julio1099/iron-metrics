package com.ironmetrics.training.infrastructure;

import com.ironmetrics.training.domain.WorkoutSet;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, UUID> {
}
