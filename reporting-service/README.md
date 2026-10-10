# reporting-service

Builds the dashboard and cross-service reports by calling the other services' summary endpoints. It has no database of its own.

| | |
|---|---|
| Owner | Member 5 |
| Port | 8089 |
| Database | none |
| Package | `com.wildlife.reporting` |
| Calls | habitat, wildlife, resource, patrol, incident, rescue, programme (their `/summary` and report endpoints) |
| Called by | the gateway only |

## Run it

1. Export the environment from the repo root: `set -a; source .env; set +a` (only `JWT_SECRET` is mandatory).
2. Start it:

```bash
cd reporting-service
mvn spring-boot:run
```

- Health: http://localhost:8089/actuator/health
- API docs (Swagger UI): http://localhost:8089/swagger-ui.html

## Environment variables

| Variable | Default | Notes |
|---|---|---|
| `APP_TIMEZONE` | `Asia/Kolkata` | Decides what "today" and "this month" mean |
| `HABITAT_SERVICE_URL` | `http://localhost:8082` | base url of habitat-service |
| `INCIDENT_SERVICE_URL` | `http://localhost:8086` | base url of incident-service |
| `JWT_SECRET` | `*(none)*` | **Required.** Same value as every other service, at least 32 characters. |
| `PATROL_SERVICE_URL` | `http://localhost:8085` | base url of patrol-service |
| `PROGRAMME_SERVICE_URL` | `http://localhost:8088` | base url of programme-service |
| `RESCUE_SERVICE_URL` | `http://localhost:8087` | base url of rescue-service |
| `RESOURCE_SERVICE_URL` | `http://localhost:8084` | base url of resource-service |
| `SERVER_PORT` | `8089` | HTTP port |
| `WILDLIFE_SERVICE_URL` | `http://localhost:8083` | base url of wildlife-service |

## Endpoints

Every path except `POST /api/auth/login` needs `Authorization: Bearer <token>`. Full request and response shapes are in CONTRACTS.md section 7 and in Swagger UI.

| Method | Path | Who can call it |
|---|---|---|
| GET | `/api/reports/dashboard` | Any logged-in user |
| GET | `/api/reports/incidents/monthly` | Any logged-in user |
| GET | `/api/reports/zones/{zoneId}` | Any logged-in user |

## Business rules

- Each section of the dashboard comes from one service and is passed through unchanged (as JSON). If a service is down its section is `null`, `servicesOnline` shows it as `false` and `warnings` explains, while the rest of the dashboard still loads.
- `/api/reports/incidents/monthly?year=&month=` returns incident-service's monthly summary plus a `warnings` array.
- `/api/reports/zones/{zoneId}` combines the zone (habitat), the number of animals in it (wildlife), this month's incidents (incident) and its water resources (habitat).
- The user's token is forwarded on every call, so the user only sees what they're allowed to see.

## Code layout

```
src/main/java/com/wildlife/reporting/
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
cd reporting-service
mvn test
```

Unit tests use JUnit 5 and Mockito only. They need no database and no other service running.

- **ReportServiceTest**: dashboard keeps working when one service is down; monthly incidents adds a warnings array; zone report reads totals from pages
