# Iron Metrics

Iron Metrics is a RESTful API for tracking and analyzing body recomposition data, focused on strength training and nutrition. The project uses a transactional PostgreSQL schema for API writes and a separate analytics schema for BI/ETL reads.

## Architecture Rules

- Java 21 and Spring Boot 3.
- Every HTTP route must be served under `/api/v1`.
- Database changes must be versioned with Flyway. Do not use `hibernate ddl-auto: update`.
- Transactional data lives in the `public` schema.
- Analytical/read-model data lives in the `analytics` schema.
- Test data migrations live in `classpath:db/migration/dev` and run only with the `dev` profile.
- API errors must use RFC 7807 Problem Details through `@ControllerAdvice`.
- Business logic must be introduced test-first with JUnit 5, Mockito, and Testcontainers when integration is involved.

## Tech Stack

- Java 21
- Spring Boot 3.5.x
- PostgreSQL 16
- Flyway
- Spring Security and JWT
- Bucket4j
- JUnit 5, Mockito, and Testcontainers
- Docker Compose for local infrastructure
- Kubernetes/Minikube planned for later sprints

## Local Requirements

The recommended local workflow is WSL2 Ubuntu, because Docker Engine is installed there without Docker Desktop.

- Java 21
- Maven
- Docker Engine
- Docker Compose plugin

Check the WSL environment:

```bash
java -version
mvn -version
docker --version
docker compose version
```

## Running PostgreSQL

From WSL:

```bash
cd /mnt/c/Users/julii/OneDrive/Desktop/iron-metrics
docker compose up -d postgres
```

The development database defaults are:

- Database: `iron_metrics`
- User: `iron_metrics`
- Password: `iron_metrics_dev_password`
- Port: `5432`

Stop the database:

```bash
docker compose down
```

Remove the local database volume:

```bash
docker compose down -v
```

## Running Tests

From WSL:

```bash
mvn test
```

Integration tests use Testcontainers and start an isolated PostgreSQL container.

Current test coverage:

- RFC 7807 error contract through `@ControllerAdvice`.
- Flyway schema and core table creation with PostgreSQL Testcontainers.
- JWT registration/login and protected business routes.
- CORS preflight behavior for allowed and rejected origins.
- Bucket4j rate limiting for auth and authenticated routes.
- `/api/v1/exercises` CRUD integration flow.
- `/api/v1/workout-sessions` and workout set integration flow.
- Estimated 1RM domain calculation and guard clauses.

## Running the API

Start PostgreSQL first, then run:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The API base path is:

```text
http://localhost:8080/api/v1
```

Health endpoint:

```text
http://localhost:8080/api/v1/actuator/health
```

## Authentication

Sprint 2 protects business routes with JWT bearer tokens.

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
```

Register payload:

```json
{
  "email": "julio@example.com",
  "displayName": "Julio",
  "password": "Password123!"
}
```

Login payload:

```json
{
  "email": "julio@example.com",
  "password": "Password123!"
}
```

Use the returned token in protected requests:

```http
Authorization: Bearer <accessToken>
```

## Training API

Exercise catalog:

```text
POST   /api/v1/exercises
GET    /api/v1/exercises
GET    /api/v1/exercises/{id}
PUT    /api/v1/exercises/{id}
DELETE /api/v1/exercises/{id}
```

Example payload:

```json
{
  "name": "Bench Press",
  "primaryMuscleGroup": "CHEST",
  "movementPattern": "PUSH",
  "mechanicsType": "COMPOUND"
}
```

Valid enum values:

- `primaryMuscleGroup`: `CHEST`, `BACK`, `SHOULDERS`, `BICEPS`, `TRICEPS`, `QUADRICEPS`, `HAMSTRINGS`, `GLUTES`, `CALVES`, `CORE`, `FULL_BODY`, `OTHER`
- `movementPattern`: `SQUAT`, `HINGE`, `PUSH`, `PULL`, `LUNGE`, `CARRY`, `ROTATION`, `ISOLATION`, `OTHER`
- `mechanicsType`: `COMPOUND`, `ISOLATION`

Workout sessions:

```text
POST /api/v1/workout-sessions
GET  /api/v1/workout-sessions
POST /api/v1/workout-sessions/{id}/sets
```

Create session payload:

```json
{
  "title": "Upper A",
  "sessionDate": "2026-07-20",
  "bodyWeightKg": "82.40"
}
```

Add set payload:

```json
{
  "exerciseId": "00000000-0000-0000-0000-000000000000",
  "setOrder": 1,
  "loadKg": "100.00",
  "repetitions": 5,
  "rpe": "9.0"
}
```

Estimated 1RM is calculated with the Epley formula using effective repetitions:

- `RIR = 10 - RPE`
- `effective reps = performed reps + RIR`
- skip estimated 1RM when `RPE < 7`
- skip estimated 1RM when effective reps are greater than `12`

## Security Controls

JWT settings:

```yaml
iron-metrics:
  security:
    jwt:
      issuer: iron-metrics
      secret: iron-metrics-local-development-secret-key-with-at-least-256-bits
      access-token-ttl: PT1H
```

CORS is environment-driven. The default local origins are `http://localhost:3000` and `http://localhost:8080`; the `prod` profile requires `IRON_METRICS_CORS_ALLOWED_ORIGINS`.

Bucket4j rate limits are configurable:

```yaml
iron-metrics:
  rate-limit:
    auth:
      capacity: 20
      refill-tokens: 20
      refill-period: PT1M
    authenticated:
      capacity: 120
      refill-tokens: 120
      refill-period: PT1M
```

## Flyway Layout

Production-safe migrations:

```text
src/main/resources/db/migration/core
```

Development-only seed migrations:

```text
src/main/resources/db/migration/dev
```

The `dev` profile loads both locations. The default and `prod` profiles load only `core`.

## Package Layout

```text
com.ironmetrics
|-- auth
|-- users
|-- training
|-- nutrition
|-- analytics
|-- config
`-- shared
    |-- error
    `-- security
```

Each feature package follows this internal split:

- `api`: controllers and request/response DTOs.
- `application`: use cases and application services.
- `domain`: domain models and business rules.
- `infrastructure`: persistence and external adapters.

## Sprint Status

- Sprint 1: foundation, Flyway/PostgreSQL schemas, RFC 7807 errors, exercise CRUD.
- Sprint 2: JWT authentication, CORS, Bucket4j rate limiting, estimated 1RM, workout sessions and sets.
