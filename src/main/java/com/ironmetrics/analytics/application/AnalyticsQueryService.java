package com.ironmetrics.analytics.application;

import com.ironmetrics.analytics.domain.BodyWeightDaily;
import com.ironmetrics.analytics.domain.ExerciseProgressionDaily;
import com.ironmetrics.analytics.domain.TrainingVolumeDaily;
import com.ironmetrics.analytics.infrastructure.AnalyticsQueryRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AnalyticsQueryService {

    private final AnalyticsQueryRepository analyticsQueryRepository;

    public AnalyticsQueryService(AnalyticsQueryRepository analyticsQueryRepository) {
        this.analyticsQueryRepository = analyticsQueryRepository;
    }

    @Transactional(readOnly = true)
    public List<TrainingVolumeDaily> findTrainingVolumeDaily(UUID userId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        return analyticsQueryRepository.findTrainingVolumeDaily(userId, from, to);
    }

    @Transactional(readOnly = true)
    public List<ExerciseProgressionDaily> findExerciseProgressionDaily(UUID userId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        return analyticsQueryRepository.findExerciseProgressionDaily(userId, from, to);
    }

    @Transactional(readOnly = true)
    public List<BodyWeightDaily> findBodyWeightDaily(UUID userId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        return analyticsQueryRepository.findBodyWeightDaily(userId, from, to);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before or equal to to.");
        }
    }
}
