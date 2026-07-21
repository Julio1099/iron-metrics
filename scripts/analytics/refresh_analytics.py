#!/usr/bin/env python3

import logging
import os
import sys

try:
    import psycopg
    from psycopg.rows import dict_row
except ImportError:
    print(
        "Missing dependency: install with `pip install \"psycopg[binary]\"`.",
        file=sys.stderr,
    )
    raise


def required_env(name: str) -> str:
    value = os.getenv(name)
    if not value:
        raise RuntimeError(f"Environment variable {name} is required.")
    return value


def dsn() -> str:
    explicit_dsn = os.getenv("IRON_METRICS_ANALYTICS_DB_DSN")
    if explicit_dsn:
        return explicit_dsn

    host = os.getenv("IRON_METRICS_DB_HOST", "localhost")
    port = os.getenv("IRON_METRICS_DB_PORT", "5432")
    database = os.getenv("IRON_METRICS_DB_NAME", "iron_metrics")
    user = required_env("IRON_METRICS_DB_USER")
    password = required_env("IRON_METRICS_DB_PASSWORD")

    return (
        f"host={host} port={port} dbname={database} "
        f"user={user} password={password}"
    )


def main() -> int:
    logging.basicConfig(
        level=os.getenv("IRON_METRICS_ETL_LOG_LEVEL", "INFO"),
        format="%(asctime)s %(levelname)s %(message)s",
    )
    logging.info("Starting analytics read model refresh.")

    with psycopg.connect(dsn(), row_factory=dict_row) as connection:
        with connection.cursor() as cursor:
            cursor.execute("SELECT * FROM analytics.refresh_body_recomposition_read_models()")
            result = cursor.fetchone()
        connection.commit()

    logging.info(
        "Completed analytics read model refresh: trainingVolumeRows=%s, "
        "exerciseProgressionRows=%s, bodyWeightRows=%s.",
        result["training_volume_rows"],
        result["exercise_progression_rows"],
        result["body_weight_rows"],
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
