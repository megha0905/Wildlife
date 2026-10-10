# resource-service

Staff, vehicles and equipment, and the time-boxed allocations that reserve them for patrols, incidents, rescues or programmes. It guarantees nothing is double-booked.

| | |
|---|---|
| Owner | Member 3 |
| Port | 8084 |
| Database | resource_db |
| Package | `com.wildlife.resource` |
| Calls | nothing |
| Called by | patrol (reserves team and vehicle), incident (reserves assigned staff), programme (staff names), reporting (summary) |

## Run it

1. Start PostgreSQL from the repo root: `docker compose up -d` (creates `resource_db` on first start).
2. Export the environment (from the repo root): `set -a; source .env; set +a`. Only `JWT_SECRET` is mandatory, everything else has a working local default.
3. Start the service:

```bash
cd resource-service
mvn spring-boot:run
```

On start-up Flyway runs `src/main/resources/db/migration/V1__init.sql`, which creates the tables and loads demo data. Hibernate then only *validates* the schema (`ddl-auto: validate`), so the SQL file is the single source of truth for tables.

- Health: http://localhost:8084/actuator/health
- API docs (Swagger UI): http://localhost:8084/swagger-ui.html
- Through the gateway every path below is also reachable at `http://localhost:8080<path>`.

## Environment variables

| Variable | Default | Notes |
|---|---|---|
| `APP_TIMEZONE` | `Asia/Kolkata` | Decides what "today" and "this month" mean |
| `DB_HOST` | `localhost` |  |
| `DB_NAME` | `resource_db` | This service's own database |
| `DB_PASSWORD` | `wildguard` |  |
| `DB_PORT` | `5432` |  |
| `DB_USERNAME` | `wildguard` |  |
| `JWT_SECRET` | `*(none)*` | **Required.** Same value as every other service, at least 32 characters. |
| `SERVER_PORT` | `8084` | HTTP port |

## Endpoints

Every path except `POST /api/auth/login` needs `Authorization: Bearer <token>`. Full request and response shapes are in CONTRACTS.md section 7 and in Swagger UI.

| Method | Path | Who can call it |
|---|---|---|
| GET | `/api/allocations` | Any logged-in user |
| GET | `/api/allocations/availability` | Any logged-in user |
| GET | `/api/allocations/{id}` | Any logged-in user |
| POST | `/api/allocations` | ADMIN, FOREST_OFFICER |
| PUT | `/api/allocations/{id}` | ADMIN, FOREST_OFFICER |
| PATCH | `/api/allocations/{id}/status` | ADMIN, FOREST_OFFICER, RANGER |
| GET | `/api/equipment` | Any logged-in user |
| GET | `/api/equipment/{id}` | Any logged-in user |
| POST | `/api/equipment` | ADMIN, FOREST_OFFICER |
| PUT | `/api/equipment/{id}` | ADMIN, FOREST_OFFICER |
| DELETE | `/api/equipment/{id}` | ADMIN |
| GET | `/api/resources/summary` | Any logged-in user |
| GET | `/api/staff` | Any logged-in user |
| GET | `/api/staff/lookup` | Any logged-in user |
| GET | `/api/staff/{id}` | Any logged-in user |
| POST | `/api/staff` | ADMIN, FOREST_OFFICER |
| PUT | `/api/staff/{id}` | ADMIN, FOREST_OFFICER |
| DELETE | `/api/staff/{id}` | ADMIN |
| GET | `/api/vehicles` | Any logged-in user |
| GET | `/api/vehicles/{id}` | Any logged-in user |
| POST | `/api/vehicles` | ADMIN, FOREST_OFFICER |
| PUT | `/api/vehicles/{id}` | ADMIN, FOREST_OFFICER |
| DELETE | `/api/vehicles/{id}` | ADMIN |

## Business rules

- **Conflict rule:** two allocations of the same resource clash when `existing.start < new.end AND new.start < existing.end`. Only `PLANNED` and `ACTIVE` allocations count. Back-to-back windows (one ends at 12:00, the next starts at 12:00) are fine. A clash returns 409 naming the conflicting allocation.
- End must be after start (422). Staff `ON_LEAVE`/`INACTIVE` and vehicles or equipment in `MAINTENANCE`/`RETIRED` cannot be allocated (422). Unknown resource → 422.
- Updating an allocation excludes itself from the conflict check.
- Allocation status moves: `PLANNED → ACTIVE | CANCELLED`, `ACTIVE → COMPLETED | CANCELLED`. Anything else is 409.
- `GET /api/staff/lookup?ids=1,2,3` returns name and designation for many staff in one call (used by other services to show names).
- `GET /api/allocations/availability` answers "is this resource free in this window?" without creating anything.

## Demo data (V1__init.sql)

Staff 1 to 8, vehicles 1 to 3, equipment 1 to 5, allocations 1 to 7 (some belong to patrol schedules 1 to 3).

## Code layout

```
src/main/java/com/wildlife/resource/
├── controller/   REST endpoints only: validate input, call a service, return a DTO
├── service/      business rules (start reading here)
├── repository/   Spring Data JPA interfaces
├── entity/       JPA entities and enums (match V1__init.sql)
├── dto/          request/response records exactly as in CONTRACTS.md

├── config/       security (JWT filter), OpenAPI, clock
└── exception/    error types and the handler that produces the standard error body
```

Errors always come back as `{timestamp, status, error, message, path, details[]}`. List endpoints are paginated with `?page=0&size=20&sort=field,asc` (max size 100) and return `{content, page, size, totalElements, totalPages}`.

## Tests

```bash
cd resource-service
mvn test
```

Unit tests use JUnit 5 and Mockito only. They need no database and no other service running.

- **AllocationServiceTest**: free resource is allocated as planned; overlapping allocation is a 409 naming the clash; only planned and active allocations count as conflicts; end before start is 422; staff on leave cannot be allocated; vehicle in maintenance cannot be allocated; unknown resource is 422; update excludes itself from the conflict check; legal and illegal status moves; planned cannot jump to completed
