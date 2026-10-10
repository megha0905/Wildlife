# wildlife-service

Registry of species and individually tagged animals, with sightings and health records.

| | |
|---|---|
| Owner | Member 2 |
| Port | 8083 |
| Database | wildlife_db |
| Package | `com.wildlife.wildlife` |
| Calls | habitat-service (`GET /api/zones/{id}`) to check the zone and copy its name |
| Called by | patrol (sightings), rescue (animal status), reporting (summary) |

## Run it

1. Start PostgreSQL from the repo root: `docker compose up -d` (creates `wildlife_db` on first start).
2. Export the environment (from the repo root): `set -a; source .env; set +a`. Only `JWT_SECRET` is mandatory, everything else has a working local default.
3. Start the service:

```bash
cd wildlife-service
mvn spring-boot:run
```

On start-up Flyway runs `src/main/resources/db/migration/V1__init.sql`, which creates the tables and loads demo data. Hibernate then only *validates* the schema (`ddl-auto: validate`), so the SQL file is the single source of truth for tables.

- Health: http://localhost:8083/actuator/health
- API docs (Swagger UI): http://localhost:8083/swagger-ui.html
- Through the gateway every path below is also reachable at `http://localhost:8080<path>`.

## Environment variables

| Variable | Default | Notes |
|---|---|---|
| `APP_TIMEZONE` | `Asia/Kolkata` | Decides what "today" and "this month" mean |
| `DB_HOST` | `localhost` |  |
| `DB_NAME` | `wildlife_db` | This service's own database |
| `DB_PASSWORD` | `wildguard` |  |
| `DB_PORT` | `5432` |  |
| `DB_USERNAME` | `wildguard` |  |
| `HABITAT_SERVICE_URL` | `http://localhost:8082` | base url of habitat-service |
| `JWT_SECRET` | `*(none)*` | **Required.** Same value as every other service, at least 32 characters. |
| `SERVER_PORT` | `8083` | HTTP port |

## Endpoints

Every path except `POST /api/auth/login` needs `Authorization: Bearer <token>`. Full request and response shapes are in CONTRACTS.md section 7 and in Swagger UI.

| Method | Path | Who can call it |
|---|---|---|
| GET | `/api/animals` | Any logged-in user |
| GET | `/api/animals/summary` | Any logged-in user |
| GET | `/api/animals/tag/{tagId}` | Any logged-in user |
| GET | `/api/animals/{id}` | Any logged-in user |
| POST | `/api/animals` | ADMIN, FOREST_OFFICER, RANGER, VET |
| PUT | `/api/animals/{id}` | ADMIN, FOREST_OFFICER, RANGER, VET |
| PATCH | `/api/animals/{id}/status` | ADMIN, FOREST_OFFICER, RANGER, VET |
| PATCH | `/api/animals/{id}/sighting` | ADMIN, FOREST_OFFICER, RANGER, VET |
| DELETE | `/api/animals/{id}` | ADMIN |
| GET | `/api/animals/{id}/health-records` | Any logged-in user |
| POST | `/api/animals/{id}/health-records` | ADMIN, FOREST_OFFICER, VET |
| GET | `/api/species` | Any logged-in user |
| GET | `/api/species/{id}` | Any logged-in user |
| POST | `/api/species` | ADMIN, FOREST_OFFICER |
| PUT | `/api/species/{id}` | ADMIN, FOREST_OFFICER |
| DELETE | `/api/species/{id}` | ADMIN |

## Business rules

- Tag IDs are unique. Each animal belongs to one species.
- When an animal is saved with a zone, the zone is checked with habitat-service. Unknown zone or a zone in a different area → 422. If habitat-service is down the animal is still saved and the response includes a `warnings` entry.
- **Health records:** only the newest record (by date) changes the animal's current health. Back-filling an older record keeps history but does not overwrite the current state. A `DECEASED` record also sets status `DECEASED`.
- **Sightings** (`PATCH /{id}/sighting`): an older sighting never moves "last seen" backwards; a future timestamp is rejected; a sighting of a `MISSING` animal sets it back to `IN_WILD`.
- Status values: `IN_WILD`, `IN_RESCUE`, `TRANSFERRED`, `MISSING`, `DECEASED`. rescue-service changes status to `IN_RESCUE` and back as cases move.

## Demo data (V1__init.sql)

Species 1 to 5 and animals 1 to 8. Animal 5 (`ELE-PRY-007`) is `IN_RESCUE` to match rescue case 1.

## Code layout

```
src/main/java/com/wildlife/wildlife/
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
cd wildlife-service
mvn test
```

Unit tests use JUnit 5 and Mockito only. They need no database and no other service running.

- **AnimalServiceTest**: newest health record updates current health; back filled older record does not overwrite current health; deceased record also sets status deceased; older sighting does not move last seen backwards; newer sighting moves location and finds missing animal; future sighting is rejected
- **ZoneLookupTest**: known zone fills name and area; unknown zone is 422; zone in another area is 422; habitat down gives warning instead of failure
