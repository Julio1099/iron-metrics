package com.ironmetrics.training.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "exercises")
@EntityListeners(AuditingEntityListener.class)
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_muscle_group", nullable = false, length = 40)
    private MuscleGroup primaryMuscleGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_pattern", nullable = false, length = 40)
    private MovementPattern movementPattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "mechanics_type", nullable = false, length = 40)
    private MechanicsType mechanicsType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Exercise() {
    }

    public Exercise(
            String name,
            MuscleGroup primaryMuscleGroup,
            MovementPattern movementPattern,
            MechanicsType mechanicsType
    ) {
        this.name = normalizeName(name);
        this.primaryMuscleGroup = primaryMuscleGroup;
        this.movementPattern = movementPattern;
        this.mechanicsType = mechanicsType;
    }

    public void update(
            String name,
            MuscleGroup primaryMuscleGroup,
            MovementPattern movementPattern,
            MechanicsType mechanicsType
    ) {
        this.name = normalizeName(name);
        this.primaryMuscleGroup = primaryMuscleGroup;
        this.movementPattern = movementPattern;
        this.mechanicsType = mechanicsType;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MuscleGroup getPrimaryMuscleGroup() {
        return primaryMuscleGroup;
    }

    public MovementPattern getMovementPattern() {
        return movementPattern;
    }

    public MechanicsType getMechanicsType() {
        return mechanicsType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private String normalizeName(String value) {
        return value.trim();
    }
}
