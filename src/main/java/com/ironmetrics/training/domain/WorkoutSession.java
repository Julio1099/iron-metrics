package com.ironmetrics.training.domain;

import com.ironmetrics.users.domain.UserAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "workout_sessions")
@EntityListeners(AuditingEntityListener.class)
public class WorkoutSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount userAccount;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "body_weight_kg", precision = 5, scale = 2)
    private BigDecimal bodyWeightKg;

    @OneToMany(mappedBy = "workoutSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkoutSet> sets = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkoutSession() {
    }

    public WorkoutSession(
            UserAccount userAccount,
            LocalDate sessionDate,
            String title,
            BigDecimal bodyWeightKg
    ) {
        this.userAccount = userAccount;
        this.sessionDate = sessionDate;
        this.title = title.trim();
        this.bodyWeightKg = bodyWeightKg;
    }

    public UUID getId() {
        return id;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getBodyWeightKg() {
        return bodyWeightKg;
    }

    public List<WorkoutSet> getSets() {
        return sets.stream()
                .sorted(Comparator.comparing(WorkoutSet::getSetOrder))
                .toList();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
