package com.ironmetrics.analytics.api;

import com.ironmetrics.analytics.application.AnalyticsQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
@Tag(name = "Analytics")
public class AnalyticsController {

    private final AnalyticsQueryService analyticsQueryService;

    public AnalyticsController(AnalyticsQueryService analyticsQueryService) {
        this.analyticsQueryService = analyticsQueryService;
    }

    @GetMapping("/training-volume/daily")
    @Operation(summary = "List daily training volume analytics for the authenticated user")
    public List<TrainingVolumeDailyResponse> trainingVolumeDaily(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return analyticsQueryService.findTrainingVolumeDaily(currentUserId(jwt), from, to)
                .stream()
                .map(TrainingVolumeDailyResponse::from)
                .toList();
    }

    @GetMapping("/exercise-progressions/daily")
    @Operation(summary = "List daily exercise progression analytics for the authenticated user")
    public List<ExerciseProgressionDailyResponse> exerciseProgressionDaily(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return analyticsQueryService.findExerciseProgressionDaily(currentUserId(jwt), from, to)
                .stream()
                .map(ExerciseProgressionDailyResponse::from)
                .toList();
    }

    @GetMapping("/body-weight/daily")
    @Operation(summary = "List daily body weight analytics for the authenticated user")
    public List<BodyWeightDailyResponse> bodyWeightDaily(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return analyticsQueryService.findBodyWeightDaily(currentUserId(jwt), from, to)
                .stream()
                .map(BodyWeightDailyResponse::from)
                .toList();
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
