CREATE TABLE analytics.training_volume_daily (
    user_id UUID NOT NULL,
    training_date DATE NOT NULL,
    total_sets INTEGER NOT NULL,
    total_repetitions INTEGER NOT NULL,
    total_volume_kg NUMERIC(12, 2) NOT NULL,
    average_rpe NUMERIC(4, 2),
    max_estimated_one_rep_max_kg NUMERIC(7, 2),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_training_volume_daily PRIMARY KEY (user_id, training_date)
);

CREATE INDEX idx_training_volume_daily_date
    ON analytics.training_volume_daily (training_date DESC);

CREATE TABLE analytics.exercise_progression_daily (
    user_id UUID NOT NULL,
    exercise_id UUID NOT NULL,
    exercise_name VARCHAR(120) NOT NULL,
    primary_muscle_group VARCHAR(40) NOT NULL,
    movement_pattern VARCHAR(40) NOT NULL,
    training_date DATE NOT NULL,
    performed_sets INTEGER NOT NULL,
    total_repetitions INTEGER NOT NULL,
    total_volume_kg NUMERIC(12, 2) NOT NULL,
    top_load_kg NUMERIC(6, 2) NOT NULL,
    max_estimated_one_rep_max_kg NUMERIC(7, 2),
    average_rpe NUMERIC(4, 2),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_exercise_progression_daily PRIMARY KEY (user_id, exercise_id, training_date)
);

CREATE INDEX idx_exercise_progression_daily_exercise_date
    ON analytics.exercise_progression_daily (exercise_id, training_date DESC);

CREATE TABLE analytics.body_weight_daily (
    user_id UUID NOT NULL,
    measurement_date DATE NOT NULL,
    body_weight_kg NUMERIC(5, 2) NOT NULL,
    source_sessions INTEGER NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_body_weight_daily PRIMARY KEY (user_id, measurement_date)
);

CREATE INDEX idx_body_weight_daily_date
    ON analytics.body_weight_daily (measurement_date DESC);

CREATE OR REPLACE FUNCTION analytics.refresh_body_recomposition_read_models()
RETURNS TABLE (
    training_volume_rows INTEGER,
    exercise_progression_rows INTEGER,
    body_weight_rows INTEGER
)
LANGUAGE plpgsql
AS $$
BEGIN
    TRUNCATE TABLE
        analytics.training_volume_daily,
        analytics.exercise_progression_daily,
        analytics.body_weight_daily;

    INSERT INTO analytics.training_volume_daily (
        user_id,
        training_date,
        total_sets,
        total_repetitions,
        total_volume_kg,
        average_rpe,
        max_estimated_one_rep_max_kg,
        updated_at
    )
    SELECT
        workout_sessions.user_id,
        workout_sessions.session_date,
        COUNT(workout_sets.id)::INTEGER,
        SUM(workout_sets.repetitions)::INTEGER,
        ROUND(SUM(workout_sets.load_kg * workout_sets.repetitions), 2),
        ROUND(AVG(workout_sets.rpe), 2),
        MAX(workout_sets.estimated_one_rep_max_kg),
        now()
    FROM public.workout_sessions
    JOIN public.workout_sets
        ON workout_sets.workout_session_id = workout_sessions.id
    GROUP BY workout_sessions.user_id, workout_sessions.session_date;

    GET DIAGNOSTICS training_volume_rows = ROW_COUNT;

    INSERT INTO analytics.exercise_progression_daily (
        user_id,
        exercise_id,
        exercise_name,
        primary_muscle_group,
        movement_pattern,
        training_date,
        performed_sets,
        total_repetitions,
        total_volume_kg,
        top_load_kg,
        max_estimated_one_rep_max_kg,
        average_rpe,
        updated_at
    )
    SELECT
        workout_sessions.user_id,
        exercises.id,
        exercises.name,
        exercises.primary_muscle_group,
        exercises.movement_pattern,
        workout_sessions.session_date,
        COUNT(workout_sets.id)::INTEGER,
        SUM(workout_sets.repetitions)::INTEGER,
        ROUND(SUM(workout_sets.load_kg * workout_sets.repetitions), 2),
        MAX(workout_sets.load_kg),
        MAX(workout_sets.estimated_one_rep_max_kg),
        ROUND(AVG(workout_sets.rpe), 2),
        now()
    FROM public.workout_sessions
    JOIN public.workout_sets
        ON workout_sets.workout_session_id = workout_sessions.id
    JOIN public.exercises
        ON exercises.id = workout_sets.exercise_id
    GROUP BY
        workout_sessions.user_id,
        exercises.id,
        exercises.name,
        exercises.primary_muscle_group,
        exercises.movement_pattern,
        workout_sessions.session_date;

    GET DIAGNOSTICS exercise_progression_rows = ROW_COUNT;

    INSERT INTO analytics.body_weight_daily (
        user_id,
        measurement_date,
        body_weight_kg,
        source_sessions,
        updated_at
    )
    SELECT
        user_id,
        session_date,
        ROUND(AVG(body_weight_kg), 2)::NUMERIC(5, 2),
        COUNT(*)::INTEGER,
        now()
    FROM public.workout_sessions
    WHERE body_weight_kg IS NOT NULL
    GROUP BY user_id, session_date;

    GET DIAGNOSTICS body_weight_rows = ROW_COUNT;

    RETURN NEXT;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'iron_metrics_analytics_reader') THEN
        CREATE ROLE iron_metrics_analytics_reader NOLOGIN;
    END IF;
END;
$$;

GRANT USAGE ON SCHEMA analytics TO iron_metrics_analytics_reader;
GRANT SELECT ON ALL TABLES IN SCHEMA analytics TO iron_metrics_analytics_reader;
ALTER DEFAULT PRIVILEGES IN SCHEMA analytics
    GRANT SELECT ON TABLES TO iron_metrics_analytics_reader;
REVOKE INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER
    ON ALL TABLES IN SCHEMA analytics FROM iron_metrics_analytics_reader;
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM iron_metrics_analytics_reader;
REVOKE EXECUTE ON FUNCTION analytics.refresh_body_recomposition_read_models() FROM PUBLIC;
REVOKE ALL ON FUNCTION analytics.refresh_body_recomposition_read_models() FROM iron_metrics_analytics_reader;

SELECT * FROM analytics.refresh_body_recomposition_read_models();
