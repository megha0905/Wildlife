# rescue-service

Tracks injured, orphaned or displaced animals from intake through veterinary treatment and rehabilitation to release or transfer.

| | |
|---|---|
| Owner | Member 4 |
| Port | 8087 |
| Database | rescue_db |
| Package | `com.wildlife.rescue` |
| Calls | wildlife (check the animal and change its status), incident (check the linked incident exists) |
| Called by | reporting (summary) |

## Run it

1. Start PostgreSQL from the repo root: `docker compose up -d` (creates `rescue_db` on first start).
2. Export the environment (from the repo root): `set -a; source .env; set +a`. Only `JWT_SECRET` is mandatory, everything else has a working local default.
3. Start the service:

```bash
cd rescue-service
mvn spring-boot:run
```

On start-up Flyway runs `src/main/resources/db/migration/V1__init.sql`, which creates the tables and loads demo data. Hibernate then only *validates* the schema (`ddl-auto: validate`), so the SQL file is the single source of truth for tables.

- Health: http://localhost:8087/actuator/health
- API docs (Swagger UI): http://localhost:8087/swagger-ui.html
- Through the gateway every path below is also reachable at `http://localhost:8080<path>`.

## Environment variables

| Variable | Default | Notes |
|---|---|---|
| `APP_TIMEZONE` | `Asia/Kolkata` | Decides what "today" and "this month" mean |
| `DB_HOST` | `localhost` |  |
| `DB_NAME` | `rescue_db` | This service's own database |
| `DB_PASSWORD` | `wildguard` |  |
| `DB_PORT` | `5432` |  |
| `DB_USERNAME` | `wildguard` |  |
| `INCIDENT_SERVICE_URL` | `http://localhost:8086` | base url of incident-service |
| `JWT_SECRET` | `*(none)*` | **Required.** Same value as every other service, at least 32 characters. |
| `SERVER_PORT` | `8087` | HTTP port |
| `WILDLIFE_SERVICE_URL` | `http://localhost:8083` | base url of wildlife-service |

## Endpoints

Every path except `POST /api/auth/login` needs `Authorization: Bearer <token>`. Full request and response shapes are in CONTRACTS.md section 7 and in Swagger UI.

| Method | Path | Who can call it |
|---|---|---|
| GET | `/api/rescues` | Any logged-in user |
| GET | `/api/rescues/summary` | Any logged-in user |
| GET | `/api/rescues/{id}` | Any logged-in user |
| POST | `/api/rescues` | ADMIN, FOREST_OFFICER, RANGER, VET |
| PUT | `/api/rescues/{id}` | ADMIN, FOREST_OFFICER, VET |
| POST | `/api/rescues/{id}/transition` | ADMIN, FOREST_OFFICER, VET |
| GET | `/api/rescues/{id}/treatments` | Any logged-in user |
| POST | `/api/rescues/{id}/treatments` | ADMIN, VET |
| GET | `/api/rescues/{id}/rehab-progress` | Any logged-in user |
| POST | `/api/rescues/{id}/rehab-progress` | ADMIN, FOREST_OFFICER, VET |
| GET | `/api/rescues/{id}/history` | Any logged-in user |

## Business rules

- **Workflow:** `REGISTERED → UNDER_TREATMENT → REHABILITATING → RELEASED | TRANSFERRED`. `RELEASED` and `TRANSFERRED` are final.
  - Moving to `REHABILITATING` needs at least one treatment record.
  - `RELEASED` needs a `release` object (date, area, zone, coordinates) **and** the latest rehab assessment must say `fitForRelease: true`.
  - `TRANSFERRED` needs a `transfer` object (destination, reason, date).
  - Treatments can be added by `ADMIN` and `VET` only.
- Case numbers are `RSC-<year>-<4 digits>`.
- **Keeping wildlife-service in step** (CONTRACTS.md section 9.4): registering a case for a tagged animal sets it `IN_RESCUE`; release sets it `IN_WILD` with the release location as last seen; transfer sets `TRANSFERRED`. If wildlife-service is down, the rescue still moves and the response carries a warning.

## Demo data (V1__init.sql)

Case 1: animal 5 (`ELE-PRY-007`) under treatment, linked to incident 8. Case 2: an untagged leopard cub, rehabilitating, latest assessment fit for release (good for demonstrating a release).

## Code layout

```
src/main/java/com/wildlife/rescue/
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
cd rescue-service
mvn test
```

Unit tests use JUnit 5 and Mockito only. They need no database and no other service running.

- **RescueServiceTest**: release records details and sends animal back into the wild; wildlife down still releases with a warning
- **RescueWorkflowTest**: vet treats and rehabilitates; officer releases when fit; illegal moves are 409; transferred and released are final; rehabilitation needs a treatment; transfer needs details; release needs details and a fit assessment; roles per move
