# 0001 - Versioned Modular Spring Boot API

## Status

Accepted

## Context

Iron Metrics needs a REST API that can evolve without breaking existing clients. The codebase also needs clear ownership boundaries while staying simple enough for early sprints.

## Decision

All public routes are exposed under `/api/v1` through the Spring Boot servlet context path. Feature code is organized by business capability, then by `api`, `application`, `domain`, and `infrastructure`.

## Consequences

Controllers keep short mappings such as `/exercises`, while clients see `/api/v1/exercises`. New features should fit into a feature package before shared abstractions are introduced.
