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
- `/api/v1/exercises` CRUD integration flow.

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

## Sprint 1 API

The first CRUD surface is the exercise catalog:

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

Security is intentionally permissive in Sprint 1 so the base CRUD can be exercised end-to-end. Sprint 2 replaces this with JWT authentication and Bucket4j rate limiting.

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
├── auth
├── users
├── training
├── nutrition
├── analytics
├── config
└── shared
    ├── error
    ├── ratelimit
    └── security
```

Each feature package follows this internal split:

- `api`: controllers and request/response DTOs.
- `application`: use cases and application services.
- `domain`: domain models and business rules.
- `infrastructure`: persistence and external adapters.

## Sprint Guardrail

Sprint 1 is limited to foundation and CRUD plumbing. The next domain implementation will be the training 1RM calculation, written test-first:

- `RIR = 10 - RPE`
- `effective reps = performed reps + RIR`
- Epley formula uses effective reps
- ignore estimated 1RM when `RPE < 7`
- ignore estimated 1RM when effective reps are greater than `12`
