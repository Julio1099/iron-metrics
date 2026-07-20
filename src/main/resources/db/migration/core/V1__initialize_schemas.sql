CREATE SCHEMA IF NOT EXISTS analytics;

COMMENT ON SCHEMA public IS 'Transactional OLTP schema used by the Iron Metrics API.';
COMMENT ON SCHEMA analytics IS 'Read-optimized OLAP schema populated from transactional data.';
