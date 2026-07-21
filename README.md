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
- Docker image build with a multi-stage Dockerfile
- Kubernetes manifests for Minikube

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
- Dev-only Flyway seed data for showroom usage.
- OpenAPI contract metadata, paths, and JWT security scheme.
- Dockerfile and Docker build context contract.
- Kubernetes manifests for API/PostgreSQL runtime, probes, ConfigMap, Secret, and Kustomize.
- Versioned Kubernetes health probes.
- Analytics schema read models, idempotent OLTP-to-OLAP refresh, and BI read-only role.
- `/api/v1/analytics` read-only endpoints backed by the `analytics` schema.
- Power BI contract documentation and BI user operational script.
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

Swagger UI:

```text
http://localhost:8080/api/v1/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/api/v1/v3/api-docs
```

## Building the API Image

Build the production-like application image:

```bash
docker build -t iron-metrics:local .
```

The image is produced with a multi-stage build:

- Maven + Eclipse Temurin 21 for compilation.
- Eclipse Temurin 21 JRE Alpine for runtime.
- Non-root `ironmetrics` user.
- Port `8080` exposed.

## Running on Minikube

Sprint 4 adds Kubernetes manifests under:

```text
k8s
```

Apply them with Kustomize:

```bash
kubectl apply -k k8s
```

The API deployment uses:

```text
/api/v1/actuator/health/liveness
/api/v1/actuator/health/readiness
```

The full local Minikube workflow is documented in:

```text
docs/minikube.md
```

## Dev Showroom Seed

The `dev` profile loads realistic demo data from `src/main/resources/db/migration/dev`.

Demo login:

```text
email: demo@ironmetrics.dev
password: Password123!
```

The seed includes:

- A demo user with a BCrypt password hash.
- Four strength exercises.
- One workout session with four sets, including a guarded set where estimated 1RM is intentionally `null`.
- Four food items and nutrition log entries for later nutrition/API sprints.

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

## Analytics API

Sprint 5 exposes read-only analytics under:

```text
GET /api/v1/analytics/training-volume/daily
GET /api/v1/analytics/exercise-progressions/daily
GET /api/v1/analytics/body-weight/daily
```

All analytics endpoints require JWT authentication and filter by the authenticated user.

Optional query parameters:

```text
from=2026-07-01
to=2026-07-31
```

The endpoints read from the `analytics` schema only. Transactional writes continue to happen in `public`.

## Analytics ETL

The read models are refreshed idempotently by:

```sql
SELECT * FROM analytics.refresh_body_recomposition_read_models();
```

Operational Python runner:

```bash
pip install "psycopg[binary]"

IRON_METRICS_DB_USER=iron_metrics \
IRON_METRICS_DB_PASSWORD=iron_metrics_dev_password \
python scripts/analytics/refresh_analytics.py
```

Power BI should use the read-only `iron_metrics_bi` user, granted through the `iron_metrics_analytics_reader` role:

```bash
psql "$IRON_METRICS_ADMIN_DSN" \
  -v bi_password="$IRON_METRICS_BI_PASSWORD" \
  -f ops/postgres/create-bi-readonly-user.sql
```

The Power BI data contract lives in:

```text
docs/power-bi.md
```

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

## CI

GitHub Actions runs the Maven test suite on every push and pull request to `main`.

```text
.github/workflows/ci.yml
```

The pipeline uses Java 21 and lets Testcontainers start PostgreSQL during `mvn -B test`.

## Architecture Decisions

Initial ADRs live in:

```text
docs/adr
```

They document the versioned modular API, Flyway-managed PostgreSQL schemas, JWT/RFC 7807 security, and the Testcontainers quality gate.

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
- Sprint 3: dev showroom seed, OpenAPI/Swagger contract, GitHub Actions CI, initial ADRs.
- Sprint 4: Docker image, Kubernetes/Minikube manifests, liveness/readiness probes, runtime guide.
- Sprint 5: analytics read models, idempotent ETL refresh, BI read-only role, analytics endpoints, Power BI contract.
