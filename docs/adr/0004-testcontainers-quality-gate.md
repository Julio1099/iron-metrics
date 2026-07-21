# 0004 - Testcontainers Quality Gate

## Status

Accepted

## Context

The project depends on PostgreSQL behavior, Flyway migrations, JPA mappings, security filters, and HTTP contracts. Unit tests alone do not cover those integration boundaries.

## Decision

Business rules are introduced test-first. Integration tests use Testcontainers with PostgreSQL. The CI pipeline runs `mvn -B test`, allowing Testcontainers to start its own database instead of relying on a shared CI service database.

## Consequences

The test suite is slower than pure unit tests, but it catches migration, persistence, security, and API contract regressions before merge.
