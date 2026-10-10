# incident-service

Records threats and emergencies (poaching, injury, conflict, fire and so on), assigns staff to them and moves each one through a controlled workflow. Home of the **mandatory monthly zone report**.

| | |
|---|---|
| Owner | Member 4 |
| Port | 8086 |
| Database | incident_db |
| Package | `com.wildlife.incident` |
| Calls | habitat (zone check and name), resource (staff names, and an `INCIDENT` allocation for each assignment) |
| Called by | patrol (escalation), rescue (link check), reporting |

## Run it

1. Start PostgreSQL from the repo root: `docker compose up -d` (creates `incident_db` on first start).
2. Export the environment (from the repo root): `set -a; source .env; set +a`. Only `JWT_SECRET` is mandatory, everything else has a working local default.
3. Start the service:

```bash
cd incident-service
mvn spring-boot:run
```

On start-up Flyway runs `src/main/resources/db/migration/V1__init.sql`, which creates the tables and loads demo data. Hibernate then only *validates* the schema (`ddl-auto: validate`), so the SQL file is the single source of truth for tables.

- Health: http://localhost:8086/actuator/health
- API docs (Swagger UI): http://localhost:8086/swagger-ui.html
- Through the gateway every path below is also reachable at `http://localhost:8080<path>`.

## Environment variables

| Variable | Default | Notes |
|---|---|---|
| `APP_TIMEZONE` | `Asia/Kolkata` | Decides what "today" and "this month" mean |
| `DB_HOST` | `localhost` |  |
| `DB_NAME` | `incident_db` | This service's own database |
| `DB_PASSWORD` | `wildguard` |  |
| `DB_PORT` | `5432` |  |
| `DB_USERNAME` | `wildguard` |  |
| `HABITAT_SERVICE_URL` | `http://localhost:8082` | base url of habitat-service |
| `JWT_SECRET` | `*(none)*` | **Required.** Same value as every other service, at least 32 characters. |
| `RESOURCE_SERVICE_URL` | `http://localhost:8084` | base url of resource-service |
| `SERVER_PORT` | `8086` | HTTP port |

## Endpoints

Every path except `POST /api/auth/login` needs `Authorization: Bearer <token>`. Full request and response shapes are in CONTRACTS.md section 7 and in Swagger UI.

| Method | Path | Who can call it |
|---|---|---|
| GET | `/api/incidents` | Any logged-in user |
| GET | `/api/incidents/zone/{zoneId}/current-month` | Any logged-in user |
| GET | `/api/incidents/summary` | Any logged-in user |
| GET | `/api/incidents/{id}` | Any logged-in user |
| POST | `/api/incidents` | ADMIN, FOREST_OFFICER, RANGER, VET |
| PUT | `/api/incidents/{id}` | ADMIN, FOREST_OFFICER |
| DELETE | `/api/incidents/{id}` | ADMIN |
| POST | `/api/incidents/{id}/transition` | ADMIN, FOREST_OFFICER, RANGER, VET |
| GET | `/api/incidents/{id}/history` | Any logged-in user |
| GET | `/api/incidents/{id}/assignments` | Any logged-in user |
| POST | `/api/incidents/{id}/assignments` | ADMIN, FOREST_OFFICER |
| DELETE | `/api/incidents/{id}/assignments/{assignmentId}` | ADMIN, FOREST_OFFICER |

## Business rules

- **Workflow:** `REPORTED → VERIFIED → IN_PROGRESS → RESOLVED → CLOSED`, one step at a time. Checks run in this order: illegal move → 409, wrong role → 403, missing requirement → 422.
  - Verify, start work and close: `ADMIN`, `FOREST_OFFICER`. Resolve: also `RANGER` and `VET`. `ANALYST` can only read.
  - `IN_PROGRESS` needs at least one active assignment. `RESOLVED` needs resolution notes.
  - Every change is written to the status history with who and when.
- Incident codes are generated as `INC-<year>-<4 digits>` from a database sequence.
- **Assignments** create a 24-hour `INCIDENT` allocation in resource-service. If that person is already booked the assignment is still saved (an emergency outranks a patrol) and the response carries a warning.

### The mandatory example: incidents in a zone this month

`GET /api/incidents/zone/{zoneId}/current-month?status=IN_PROGRESS`

- "This month" is the calendar month in `APP_TIMEZONE` (Asia/Kolkata by default), from the 1st at 00:00 to the 1st of next month, by `occurredAt`.
- `status` is optional. It filters the `incidents` list, but `statusSummary` and `totalIncidents` always count the whole month, so the UI can show the counts as filter buttons.
- Each incident includes its location and assigned personnel (names fetched in one batch call).
- If habitat-service is down, the incidents are still returned with `zoneName: null` and a warning.

```bash
curl -H "Authorization: Bearer $TOKEN" "localhost:8080/api/incidents/zone/1/current-month?status=IN_PROGRESS"
```

The frontend screen that uses it is **Monthly zone review** (`frontend/src/pages/ZoneMonthlyPage.tsx`).

## Demo data (V1__init.sql)

Eight incidents. Their dates are computed relative to the current month when the migration runs, so a fresh database always has incidents "this month". If you keep the same database into next month, wipe it to get fresh dates (see the root README).

## Code layout

```
src/main/java/com/wildlife/incident/
├── controller/   REST endpoints only: validate input, call a service, return a DTO
├── service/      business rules (start reading here)
├── repository/   Spring Data JPA interfaces
├── entity/       JPA entities and enums (match V1__init.sql)
├── dto/          request/response records exactly as in CONTRACTS.md
├── client/       OpenFeign clients for other services, plus their DTOs
├── config/       security (JWT filter), OpenAPI, clock, Feign token forwarding
└── exception/    error types and the handler that produces the standard error body
```

Errors always come back as `{timestamp, status, error, message, path, details[]}`. List endpoints are paginated with `?page=0&size=20&sort=field,asc` (max size 100) and return `{content, page, size, totalElements, totalPages}`.

## Tests

```bash
cd incident-service
mvn test
```

Unit tests use JUnit 5 and Mockito only. They need no database and no other service running.

- **IncidentWorkflowTest**: happy path by an officer; skipping or going back is 409; ranger can resolve but not verify or close; vet can resolve; analyst can make no moves; starting work needs an active assignment; resolving needs notes; illegal move is reported before role
- **ZoneMonthlyReportServiceTest**: queries exactly the current month window; month is decided in indian time not utc; summary counts all statuses while list is filtered; each incident carries its assigned personnel; habitat down still returns the incidents with a warning; unknown zone gives empty list and warning
