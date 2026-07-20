CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(254) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_users_email_lower ON users (lower(email));

CREATE TABLE exercises (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    primary_muscle_group VARCHAR(40) NOT NULL,
    movement_pattern VARCHAR(40) NOT NULL,
    mechanics_type VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_exercises_primary_muscle_group CHECK (
        primary_muscle_group IN (
            'CHEST',
            'BACK',
            'SHOULDERS',
            'BICEPS',
            'TRICEPS',
            'QUADRICEPS',
            'HAMSTRINGS',
            'GLUTES',
            'CALVES',
            'CORE',
            'FULL_BODY',
            'OTHER'
        )
    ),
    CONSTRAINT chk_exercises_movement_pattern CHECK (
        movement_pattern IN (
            'SQUAT',
            'HINGE',
            'PUSH',
            'PULL',
            'LUNGE',
            'CARRY',
            'ROTATION',
            'ISOLATION',
            'OTHER'
        )
    ),
    CONSTRAINT chk_exercises_mechanics_type CHECK (
        mechanics_type IN ('COMPOUND', 'ISOLATION')
    )
);

CREATE UNIQUE INDEX uk_exercises_name_lower ON exercises (lower(name));

CREATE TABLE workout_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    session_date DATE NOT NULL,
    title VARCHAR(120) NOT NULL,
    body_weight_kg NUMERIC(5, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_workout_sessions_body_weight_positive CHECK (
        body_weight_kg IS NULL OR body_weight_kg > 0
    )
);

CREATE INDEX idx_workout_sessions_user_date ON workout_sessions (user_id, session_date DESC);

CREATE TABLE workout_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workout_session_id UUID NOT NULL REFERENCES workout_sessions (id) ON DELETE CASCADE,
    exercise_id UUID NOT NULL REFERENCES exercises (id),
    set_order INTEGER NOT NULL,
    load_kg NUMERIC(6, 2) NOT NULL,
    repetitions INTEGER NOT NULL,
    rpe NUMERIC(3, 1) NOT NULL,
    estimated_one_rep_max_kg NUMERIC(7, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_workout_sets_session_order UNIQUE (workout_session_id, set_order),
    CONSTRAINT chk_workout_sets_order_positive CHECK (set_order > 0),
    CONSTRAINT chk_workout_sets_load_non_negative CHECK (load_kg >= 0),
    CONSTRAINT chk_workout_sets_repetitions_positive CHECK (repetitions > 0),
    CONSTRAINT chk_workout_sets_rpe_range CHECK (rpe >= 1 AND rpe <= 10),
    CONSTRAINT chk_workout_sets_estimated_1rm_positive CHECK (
        estimated_one_rep_max_kg IS NULL OR estimated_one_rep_max_kg > 0
    )
);

CREATE INDEX idx_workout_sets_session ON workout_sets (workout_session_id);
CREATE INDEX idx_workout_sets_exercise ON workout_sets (exercise_id);

CREATE TABLE food_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(160) NOT NULL,
    serving_size_grams NUMERIC(7, 2) NOT NULL,
    calories INTEGER NOT NULL,
    protein_grams NUMERIC(7, 2) NOT NULL,
    carbohydrate_grams NUMERIC(7, 2) NOT NULL,
    fat_grams NUMERIC(7, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_food_items_serving_positive CHECK (serving_size_grams > 0),
    CONSTRAINT chk_food_items_calories_non_negative CHECK (calories >= 0),
    CONSTRAINT chk_food_items_protein_non_negative CHECK (protein_grams >= 0),
    CONSTRAINT chk_food_items_carbohydrate_non_negative CHECK (carbohydrate_grams >= 0),
    CONSTRAINT chk_food_items_fat_non_negative CHECK (fat_grams >= 0)
);

CREATE UNIQUE INDEX uk_food_items_name_lower ON food_items (lower(name));

CREATE TABLE nutrition_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    food_item_id UUID NOT NULL REFERENCES food_items (id),
    log_date DATE NOT NULL,
    quantity_grams NUMERIC(8, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_nutrition_logs_quantity_positive CHECK (quantity_grams > 0)
);

CREATE INDEX idx_nutrition_logs_user_date ON nutrition_logs (user_id, log_date DESC);
CREATE INDEX idx_nutrition_logs_food_item ON nutrition_logs (food_item_id);
