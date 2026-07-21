# Power BI Analytics Contract

This document defines how Power BI should consume Iron Metrics analytical data.

Power BI must connect with the read-only database user `iron_metrics_bi`, which receives permissions through the `iron_metrics_analytics_reader` role. The API application user remains responsible for OLTP writes and ETL refresh execution.

## Connection

Recommended connection mode:

- Source: PostgreSQL.
- Database: `iron_metrics`.
- Schema: `analytics`.
- User: `iron_metrics_bi`.
- Access: read-only.

Create or rotate the BI user with:

```bash
psql "$IRON_METRICS_ADMIN_DSN" \
  -v bi_password="$IRON_METRICS_BI_PASSWORD" \
  -f ops/postgres/create-bi-readonly-user.sql
```

The password must come from a secret manager, Kubernetes Secret, or local environment variable. Do not commit BI credentials.

## Refresh Flow

The analytical tables are derived from transactional tables in `public`.

Run the idempotent ETL refresh:

```bash
pip install "psycopg[binary]"

IRON_METRICS_DB_USER=iron_metrics \
IRON_METRICS_DB_PASSWORD=iron_metrics_dev_password \
python scripts/analytics/refresh_analytics.py
```

The script calls:

```sql
SELECT * FROM analytics.refresh_body_recomposition_read_models();
```

## Data Dictionary

### analytics.training_volume_daily

Grain: one row per user and training date.

| Column | Type | Description |
| --- | --- | --- |
| `user_id` | UUID | Owner of the training data. |
| `training_date` | DATE | Workout session date. |
| `total_sets` | INTEGER | Number of performed sets. |
| `total_repetitions` | INTEGER | Sum of performed repetitions. |
| `total_volume_kg` | NUMERIC(12,2) | Sum of `load_kg * repetitions`. |
| `average_rpe` | NUMERIC(4,2) | Mean RPE for the day. |
| `max_estimated_one_rep_max_kg` | NUMERIC(7,2) | Best estimated 1RM for the day, ignoring guarded/null values. |
| `updated_at` | TIMESTAMPTZ | Last ETL refresh timestamp. |

### analytics.exercise_progression_daily

Grain: one row per user, exercise, and training date.

| Column | Type | Description |
| --- | --- | --- |
| `user_id` | UUID | Owner of the training data. |
| `exercise_id` | UUID | Source exercise identifier. |
| `exercise_name` | VARCHAR(120) | Exercise name at refresh time. |
| `primary_muscle_group` | VARCHAR(40) | Main muscle group. |
| `movement_pattern` | VARCHAR(40) | Movement pattern. |
| `training_date` | DATE | Workout session date. |
| `performed_sets` | INTEGER | Sets for that exercise and day. |
| `total_repetitions` | INTEGER | Repetitions for that exercise and day. |
| `total_volume_kg` | NUMERIC(12,2) | Volume for that exercise and day. |
| `top_load_kg` | NUMERIC(6,2) | Heaviest load used that day. |
| `max_estimated_one_rep_max_kg` | NUMERIC(7,2) | Best estimated 1RM for that exercise and day. |
| `average_rpe` | NUMERIC(4,2) | Mean RPE for that exercise and day. |
| `updated_at` | TIMESTAMPTZ | Last ETL refresh timestamp. |

### analytics.body_weight_daily

Grain: one row per user and measurement date.

| Column | Type | Description |
| --- | --- | --- |
| `user_id` | UUID | Owner of the body weight data. |
| `measurement_date` | DATE | Workout session date where body weight was logged. |
| `body_weight_kg` | NUMERIC(5,2) | Average body weight for the day. |
| `source_sessions` | INTEGER | Number of sessions contributing body weight values. |
| `updated_at` | TIMESTAMPTZ | Last ETL refresh timestamp. |

## Relacionamentos

Expected model relationships in Power BI:

- `analytics.training_volume_daily[user_id]` to user dimension, when a user dimension is introduced.
- `analytics.exercise_progression_daily[user_id]` to the same user dimension.
- `analytics.body_weight_daily[user_id]` to the same user dimension.
- `analytics.exercise_progression_daily[exercise_id]` to an exercise dimension, when a dedicated dimension is introduced.
- Date fields should connect to a shared calendar table:
  - `training_volume_daily[training_date]`
  - `exercise_progression_daily[training_date]`
  - `body_weight_daily[measurement_date]`

## Medidas iniciais

Suggested DAX measures:

```DAX
Total Volume Kg = SUM('training_volume_daily'[total_volume_kg])

Total Sets = SUM('training_volume_daily'[total_sets])

Average RPE = AVERAGE('training_volume_daily'[average_rpe])

Best Estimated 1RM Kg = MAX('exercise_progression_daily'[max_estimated_one_rep_max_kg])

Body Weight Kg = AVERAGE('body_weight_daily'[body_weight_kg])

Volume Per Set Kg =
DIVIDE([Total Volume Kg], [Total Sets])
```

## Guardrails

- Power BI must use `iron_metrics_bi`, never the application user.
- `iron_metrics_bi` must consume only the `analytics` schema.
- The BI user must not execute the ETL function.
- The BI user must not read tables in `public`.
- The API exposes analytics as read-only endpoints under `/api/v1/analytics`.
