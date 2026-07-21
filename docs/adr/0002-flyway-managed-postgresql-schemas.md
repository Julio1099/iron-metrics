# 0002 - Flyway Managed PostgreSQL Schemas

## Status

Accepted

## Context

The platform stores operational training and nutrition data today and will later expose analytics/BI read models. Schema drift must be controlled across local, test, and production environments.

## Decision

PostgreSQL is the system of record. Flyway owns schema changes. Hibernate uses `ddl-auto: validate`; it must not create or update schema objects. OLTP tables live in `public`, while analytics/read-model objects live in `analytics`.

Development-only seed data lives under `classpath:db/migration/dev` and is loaded only by the `dev` profile.

## Consequences

Every database change is explicit and reviewable. Tests can assert which migrations run in each profile, and analytics work can evolve without mixing BI objects into transactional tables.
