# programme-service

Conservation programmes (species recovery, habitat restoration, anti-poaching and so on) with activities, measurable targets and progress updates.

| | |
|---|---|
| Owner | Member 5 |
| Port | 8088 |
| Database | programme_db |
| Package | `com.wildlife.programme` |
| Calls | resource (`/api/staff/lookup` for lead and assignee names) |
| Called by | reporting (summary) |

## Run it

1. Start PostgreSQL from the repo root: `docker compose up -d` (creates `programme_db` on first start).
2. Export the environment (from the repo root): `set -a; source .env; set +a`. Only `JWT_SECRET` is mandatory, everything else has a working local default.
3. Start the service:

```bash
cd programme-service
mvn spring-boot:run
```

On start-up Flyway runs `src/main/resources/db/migration/V1__init.sql`, which creates the tables and loads demo data. Hibernate then only *validates* the schema (`ddl-auto: validate`), so the SQL file is the single source of truth for tables.

- Health: http://localhost:8088/actuator/health
- API docs (Swagger UI): http://localhost:8088/swagger-ui.html
- Through the gateway every path below is also reachable at `http://localhost:8080<path>`.

## Environment variables

| Variable | Default | Notes |
|---|---|---|
| `APP_TIMEZONE` | `Asia/Kolkata` | Decides what "today" and "this month" mean |
| `DB_HOST` | `localhost` |  |
| `DB_NAME` | `programme_db` | This service's own database |
| `DB_PASSWORD` | `wildguard` |  |
| `DB_PORT` | `5432` |  |
| `DB_USERNAME` | `wildguard` |  |
| `JWT_SECRET` | `*(none)*` | **Required.** Same value as every other service, at least 32 characters. |
| `RESOURCE_SERVICE_URL` | `http://localhost:8084` | base url of resource-service |
| `SERVER_PORT` | `8088` | HTTP port |

## Endpoints

Every path except `POST /api/auth/login` needs `Authorization: Bearer <token>`. Full request and response shapes are in CONTRACTS.md section 7 and in Swagger UI.

| Method | Path | Who can call it |
|---|---|---|
| GET | `/api/programmes` | Any logged-in user |
| GET | `/api/programmes/summary` | Any logged-in user |
| GET | `/api/programmes/{id}` | Any logged-in user |
| POST | `/api/programmes` | ADMIN, FOREST_OFFICER |
| PUT | `/api/programmes/{id}` | ADMIN, FOREST_OFFICER |
| PATCH | `/api/programmes/{id}/status` | ADMIN, FOREST_OFFICER |
| DELETE | `/api/programmes/{id}` | ADMIN |
| GET | `/api/programmes/{id}/activities` | Any logged-in user |
| POST | `/api/programmes/{id}/activities` | ADMIN, FOREST_OFFICER |
| PUT | `/api/programmes/{id}/activities/{activityId}` | ADMIN, FOREST_OFFICER, RANGER |
| DELETE | `/api/programmes/{id}/activities/{activityId}` | ADMIN, FOREST_OFFICER |
| GET | `/api/programmes/{id}/targets` | Any logged-in user |
| POST | `/api/programmes/{id}/targets` | ADMIN, FOREST_OFFICER |
| PUT | `/api/programmes/{id}/targets/{targetId}` | ADMIN, FOREST_OFFICER |
| DELETE | `/api/programmes/{id}/targets/{targetId}` | ADMIN, FOREST_OFFICER |
| GET | `/api/programmes/{id}/progress` | Any logged-in user |
| POST | `/api/programmes/{id}/progress` | ADMIN, FOREST_OFFICER, RANGER, VET |

## Business rules

- **Status moves:** `PLANNED → ACTIVE | CANCELLED`; `ACTIVE → ON_HOLD | COMPLETED | CANCELLED`; `ON_HOLD → ACTIVE | CANCELLED`. Completed and cancelled programmes are read-only.
- **Progress per target** = (current − baseline) / (target − baseline) × 100, clamped to 0–100 and rounded to one decimal. This works for falling targets too (for example, snares found going from 40 down to 5). Programme progress is the average over its targets.
- A progress update linked to a target must give a new value; it records the previous value and moves the target's current value.
- An activity is **overdue** when its planned end is before today and it isn't `DONE`.
- If resource-service is down, names come back null with a warning.

## Demo data (V1__init.sql)

Programmes 1 to 3 with activities, targets 1 to 3 and a few progress updates.

## Code layout

```
src/main/java/com/wildlife/programme/
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
cd programme-service
mvn test
```

Unit tests use JUnit 5 and Mockito only. They need no database and no other service running.

- **ProgrammeServiceTest**: progress update moves the target and remembers the previous value; target update needs a new value; completed programme is read only; status moves
- **ProgressCalculatorTest**: increasing target; decreasing target like snares found; clamped between zero and hundred; target equal to baseline; rounded to one decimal; programme average
