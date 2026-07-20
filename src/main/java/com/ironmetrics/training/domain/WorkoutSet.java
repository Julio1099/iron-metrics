package com.ironmetrics.training.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "workout_sets")
@EntityListeners(AuditingEntityListener.class)
public class WorkoutSet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_session_id", nullable = false)
    private WorkoutSession workoutSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "set_order", nullable = false)
    private int setOrder;

    @Column(name = "load_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal loadKg;

    @Column(nullable = false)
    private int repetitions;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal rpe;

    @Column(name = "estimated_one_rep_max_kg", precision = 7, scale = 2)
    private BigDecimal estimatedOneRepMaxKg;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkoutSet() {
    }

    public WorkoutSet(
            WorkoutSession workoutSession,
            Exercise exercise,
            int setOrder,
            BigDecimal loadKg,
            int repetitions,
            BigDecimal rpe,
            BigDecimal estimatedOneRepMaxKg
    ) {
        this.workoutSession = workoutSession;
        this.exercise = exercise;
        this.setOrder = setOrder;
        this.loadKg = loadKg;
        this.repetitions = repetitions;
        this.rpe = rpe;
        this.estimatedOneRepMaxKg = estimatedOneRepMaxKg;
    }

    public UUID getId() {
        return id;
    }

    public WorkoutSession getWorkoutSession() {
        return workoutSession;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public int getSetOrder() {
        return setOrder;
    }

    public BigDecimal getLoadKg() {
        return loadKg;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public BigDecimal getRpe() {
        return rpe;
    }

    public BigDecimal getEstimatedOneRepMaxKg() {
        return estimatedOneRepMaxKg;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
