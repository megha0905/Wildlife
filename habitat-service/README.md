# habitat-service

Owns the map of the protected estate: protected areas, the zones inside them, habitats within zones, and water resources. Other services check zone IDs against it.

| | |
|---|---|
| Owner | Member 2 |
| Port | 8082 |
| Database | habitat_db |
| Package | `com.wildlife.habitat` |
| Calls | nothing |
| Called by | wildlife, patrol, incident (zone checks) and reporting (summaries) |

## Run it

1. Start PostgreSQL from the repo root: `docker compose up -d` (creates `habitat_db` on first start).
2. Export the environment (from the repo root): `set -a; source .env; set +a`. Only `JWT_SECRET` is mandatory, everything else has a working local default.
3. Start the service:

```bash
cd habitat-service
mvn spring-boot:run
```

On start-up Flyway runs `src/main/resources/db/migration/V1__init.sql`, which creates the tables and loads demo data. Hibernate then only *validates* the schema (`ddl-auto: validate`), so the SQL file is the single source of truth for tables.

- Health: http://localhost:8082/actuator/health
- API docs (Swagger UI): http://localhost:8082/swagger-ui.html
- Through the gateway every path below is also reachable at `http://localhost:8080<path>`.

## Environment variables

| Variable | Default | Notes |
|---|---|---|
| `APP_TIMEZONE` | `Asia/Kolkata` | Decides what "today" and "this month" mean |
| `DB_HOST` | `localhost` |  |
| `DB_NAME` | `habitat_db` | This service's own database |
| `DB_PASSWORD` | `wildguard` |  |
| `DB_PORT` | `5432` |  |
| `DB_USERNAME` | `wildguard` |  |
| `JWT_SECRET` | `*(none)*` | **Required.** Same value as every other service, at least 32 characters. |
| `SERVER_PORT` | `8082` | HTTP port |

## Endpoints

Every path except `POST /api/auth/login` needs `Authorization: Bearer <token>`. Full request and response shapes are in CONTRACTS.md section 7 and in Swagger UI.

| Method | Path | Who can call it |
|---|---|---|
| GET | `/api/areas` | Any logged-in user |
| GET | `/api/areas/summary` | Any logged-in user |
| GET | `/api/areas/{id}` | Any logged-in user |
| GET | `/api/areas/{id}/zones` | Any logged-in user |
| POST | `/api/areas` | ADMIN, FOREST_OFFICER |
| PUT | `/api/areas/{id}` | ADMIN, FOREST_OFFICER |
| DELETE | `/api/areas/{id}` | ADMIN |
| GET | `/api/habitats` | Any logged-in user |
| GET | `/api/habitats/{id}` | Any logged-in user |
| POST | `/api/habitats` | ADMIN, FOREST_OFFICER |
| PUT | `/api/habitats/{id}` | ADMIN, FOREST_OFFICER |
| DELETE | `/api/habitats/{id}` | ADMIN, FOREST_OFFICER |
| GET | `/api/water-resources` | Any logged-in user |
| GET | `/api/water-resources/{id}` | Any logged-in user |
| POST | `/api/water-resources` | ADMIN, FOREST_OFFICER |
| PUT | `/api/water-resources/{id}` | ADMIN, FOREST_OFFICER |
| PATCH | `/api/water-resources/{id}/status` | ADMIN, FOREST_OFFICER, RANGER |
| DELETE | `/api/water-resources/{id}` | ADMIN, FOREST_OFFICER |
| GET | `/api/zones` | Any logged-in user |
| GET | `/api/zones/{id}` | Any logged-in user |
| POST | `/api/zones` | ADMIN, FOREST_OFFICER |
| PUT | `/api/zones/{id}` | ADMIN, FOREST_OFFICER |
| DELETE | `/api/zones/{id}` | ADMIN |

## Business rules

- Every zone belongs to one area; every habitat and water resource belongs to one zone. Codes are unique (case-insensitive).
- An area that still has zones, or a zone that still has habitats or water resources, cannot be deleted (409).
- `GET /api/zones/{id}` returns the shared `ZoneDto` shape other services rely on. Don't rename its fields without updating CONTRACTS.md.
- Water status (`AVAILABLE`, `LOW`, `DRY`, `CONTAMINATED`) can be updated by rangers in the field; anything not `AVAILABLE` counts as "needing attention" in the summary.
- `GET /api/areas/summary` lists every enum value, even with a count of 0, so charts keep stable categories.

## Demo data (V1__init.sql)

Areas 1 to 3 (Kanha, Kaziranga, Periyar), zones 1 to 6, several habitats and water resources. Zone IDs 1 to 6 are used by the seed data of other services.

## Code layout

```
src/main/java/com/wildlife/habitat/
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
cd habitat-service
mvn test
```

Unit tests use JUnit 5 and Mockito only. They need no database and no other service running.

- **HabitatServicesTest**: area with zones cannot be deleted; duplicate zone code is rejected case insensitively; summary lists every enum value and counts water needing attention
