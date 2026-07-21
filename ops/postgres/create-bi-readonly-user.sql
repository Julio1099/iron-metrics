\set ON_ERROR_STOP on

\if :{?bi_password}
\else
\echo 'Usage: psql ... -v bi_password=<strong-password> -f ops/postgres/create-bi-readonly-user.sql'
\quit 1
\endif

SELECT format('CREATE ROLE iron_metrics_bi LOGIN PASSWORD %L', :'bi_password')
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_roles
    WHERE rolname = 'iron_metrics_bi'
)\gexec

ALTER ROLE iron_metrics_bi PASSWORD :'bi_password';

SELECT format('GRANT CONNECT ON DATABASE %I TO iron_metrics_bi', current_database())\gexec
GRANT iron_metrics_analytics_reader TO iron_metrics_bi;

REVOKE ALL ON ALL TABLES IN SCHEMA public FROM iron_metrics_bi;
REVOKE ALL ON SCHEMA public FROM iron_metrics_bi;
