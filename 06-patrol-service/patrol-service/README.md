# patrol-service

Patrol teams, routes with waypoints, scheduled patrols and the field logs rangers write during a patrol. A log can be escalated into an incident.

| | |
|---|---|
| Owner | Member 3 |
| Port | 8085 |
| Database | patrol_db |
| Package | `com.wildlife.patrol` |
| Calls | habitat (zone check), resource (reserve team members and vehicle), wildlife (record sightings), incident (create incident on escalation) |
| Called by | reporting (summary) |

## Run it

1. Start PostgreSQL from the repo root: `docker compose up -d` (creates `patrol_db` on first start).
2. Export the environment (from the repo root): `set -a; source .env; set +a`. Only `JWT_SECRET` is mandatory, everything else has a working local default.
3. Start the service:

```bash
cd patrol-service
mvn spring-boot:run
```

On start-up Flyway runs `src/main/resources/db/migration/V1__init.sql`, which creates the tables and loads demo data. Hibernate then only *validates* the schema (`ddl-auto: validate`), so the SQL file is the single source of truth for tables.

- Health: http://localhost:8085/actuator/health
- API docs (Swagger UI): http://localhost:8085/swagger-ui.html
- Through the gateway every path below is also reachable at `http://localhost:8080<path>`.

## Environment variables

| Variable | Default | Notes |
|---|---|---|
| `APP_TIMEZONE` | `Asia/Kolkata` | Decides what "today" and "this month" mean |
| `DB_HOST` | `localhost` |  |
| `DB_NAME` | `patrol_db` | This service's own database |
| `DB_PASSWORD` | `wildguard` |  |
| `DB_PORT` | `5432` |  |
| `DB_USERNAME` | `wildguard` |  |
| `HABITAT_SERVICE_URL` | `http://localhost:8082` | base url of habitat-service |
| `INCIDENT_SERVICE_URL` | `http://localhost:8086` | base url of incident-service |
| `JWT_SECRET` | `*(none)*` | **Required.** Same value as every other service, at least 32 characters. |
| `RESOURCE_SERVICE_URL` | `http://localhost:8084` | base url of resource-service |
| `SERVER_PORT` | `8085` | HTTP port |
| `WILDLIFE_SERVICE_URL` | `http://localhost:8083` | base url of wildlife-service |

## Endpoints

Every path except `POST /api/auth/login` needs `Authorization: Bearer <token>`. Full request and response shapes are in CONTRACTS.md section 7 and in Swagger UI.

| Method | Path | Who can call it |
|---|---|---|
| GET | `/api/patrols/routes` | Any logged-in user |
| GET | `/api/patrols/routes/{id}` | Any logged-in user |
| POST | `/api/patrols/routes` | ADMIN, FOREST_OFFICER |
| PUT | `/api/patrols/routes/{id}` | ADMIN, FOREST_OFFICER |
| DELETE | `/api/patrols/routes/{id}` | ADMIN |
| GET | `/api/patrols/schedules` | Any logged-in user |
| GET | `/api/patrols/schedules/{id}` | Any logged-in user |
| POST | `/api/patrols/schedules` | ADMIN, FOREST_OFFICER |
| PUT | `/api/patrols/schedules/{id}` | ADMIN, FOREST_OFFICER |
| PATCH | `/api/patrols/schedules/{id}/status` | ADMIN, FOREST_OFFICER, RANGER |
| GET | `/api/patrols/schedules/{id}/logs` | Any logged-in user |
| POST | `/api/patrols/schedules/{id}/logs` | ADMIN, FOREST_OFFICER, RANGER |
| POST | `/api/patrols/logs/{logId}/escalate` | ADMIN, FOREST_OFFICER, RANGER |
| GET | `/api/patrols/summary` | Any logged-in user |
| GET | `/api/patrols/teams` | Any logged-in user |
| GET | `/api/patrols/teams/{id}` | Any logged-in user |
| POST | `/api/patrols/teams` | ADMIN, FOREST_OFFICER |
| PUT | `/api/patrols/teams/{id}` | ADMIN, FOREST_OFFICER |
| DELETE | `/api/patrols/teams/{id}` | ADMIN |

## Business rules

- **Status workflow:** `SCHEDULED → IN_PROGRESS | CANCELLED | MISSED`, `IN_PROGRESS → COMPLETED`. Other moves are 409. Logs can only be added once a patrol is `IN_PROGRESS`.
- Only `NIGHT` shifts may end after midnight (end time earlier than start time).
- **Creating a schedule reserves resources:** one `PATROL` allocation per team member plus the vehicle. If any reservation returns 409 the schedule is refused and the reservations already made are cancelled. If resource-service is unreachable the schedule is saved with a warning.
- Status changes are mirrored to the allocations (`IN_PROGRESS` → `ACTIVE`, `COMPLETED` → `COMPLETED`, `CANCELLED`/`MISSED` → `CANCELLED`); failures here are warnings, never errors.
- An `ANIMAL_SIGHTING` log with a tag ID updates that animal's last-seen location in wildlife-service (warning if it fails).
- **Escalation:** `POST /api/patrols/logs/{logId}/escalate` creates an incident from the log and stores its ID. If incident-service is down the call returns 503, because the whole point of the call is the incident.

## Demo data (V1__init.sql)

Teams 1 and 2, routes 1 and 2, schedules 1 to 3, logs 1 and 2 (log 2 was escalated to incident 3).

## Code layout

```
src/main/java/com/wildlife/patrol/
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
cd patrol-service
mvn test
```

Unit tests use JUnit 5 and Mockito only. They need no database and no other service running.

- **PatrolWorkflowTest**: legal moves; illegal moves are 409; logs only once started; only night patrols may cross midnight
- **ScheduleServiceTest**: reserves every member and the vehicle; conflict fails the schedule and cancels what was reserved; resource service down saves schedule with warning
