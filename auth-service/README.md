# auth-service

Logs users in and issues the JWT that every other service trusts. Also lets an administrator manage user accounts and roles.

| | |
|---|---|
| Owner | Member 5 |
| Port | 8081 |
| Database | auth_db |
| Package | `com.wildlife.auth` |
| Calls | nothing |
| Called by | the gateway only (other services never call it; they validate the token themselves) |

## Run it

1. Start PostgreSQL from the repo root: `docker compose up -d` (creates `auth_db` on first start).
2. Export the environment (from the repo root): `set -a; source .env; set +a`. Only `JWT_SECRET` is mandatory, everything else has a working local default.
3. Start the service:

```bash
cd auth-service
mvn spring-boot:run
```

On start-up Flyway runs `src/main/resources/db/migration/V1__init.sql`, which creates the tables and loads demo data. Hibernate then only *validates* the schema (`ddl-auto: validate`), so the SQL file is the single source of truth for tables.

- Health: http://localhost:8081/actuator/health
- API docs (Swagger UI): http://localhost:8081/swagger-ui.html
- Through the gateway every path below is also reachable at `http://localhost:8080<path>`.

## Environment variables

| Variable | Default | Notes |
|---|---|---|
| `APP_TIMEZONE` | `Asia/Kolkata` | Decides what "today" and "this month" mean |
| `DB_HOST` | `localhost` |  |
| `DB_NAME` | `auth_db` | This service's own database |
| `DB_PASSWORD` | `wildguard` |  |
| `DB_PORT` | `5432` |  |
| `DB_USERNAME` | `wildguard` |  |
| `JWT_EXPIRATION_MINUTES` | `120` | How long a login lasts |
| `JWT_SECRET` | `*(none)*` | **Required.** Same value as every other service, at least 32 characters. |
| `SERVER_PORT` | `8081` | HTTP port |

## Endpoints

Every path except `POST /api/auth/login` needs `Authorization: Bearer <token>`. Full request and response shapes are in CONTRACTS.md section 7 and in Swagger UI.

| Method | Path | Who can call it |
|---|---|---|
| POST | `/api/auth/login` | Public |
| GET | `/api/auth/me` | Any logged-in user |
| POST | `/api/auth/change-password` | Any logged-in user |
| GET | `/api/users` | ADMIN |
| GET | `/api/users/{id}` | ADMIN |
| POST | `/api/users` | ADMIN |
| PUT | `/api/users/{id}` | ADMIN |
| PATCH | `/api/users/{id}/active` | ADMIN |
| PUT | `/api/users/{id}/password` | ADMIN |

## Business rules

- Passwords are stored as BCrypt hashes (strength 10). Plain passwords are never logged or returned.
- Tokens are HS256, signed with `JWT_SECRET`, issuer `wildguard-auth`. Claims: `sub` (username), `uid`, `name`, `roles[]`, `staffId`, `iat`, `exp` (CONTRACTS.md section 3).
- Wrong username and wrong password give the same message ("Invalid username or password"), so the API does not reveal which usernames exist.
- A deactivated user cannot log in. Admins cannot deactivate themselves.
- Usernames are unique (case-insensitive). Passwords must be at least 8 characters.
- Roles: `ADMIN`, `FOREST_OFFICER`, `RANGER`, `VET`, `ANALYST`. `staffId` links a login to a staff record in resource-service.

## Demo data (V1__init.sql)

Five development users (table `app_users`): admin / Admin@123, officer / Officer@123 (staffId 1), ranger / Ranger@123 (staffId 2), vet / Vet@123 (staffId 4), analyst / Analyst@123. Change these before any real deployment.

## Code layout

```
src/main/java/com/wildlife/auth/
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
cd auth-service
mvn test
```

Unit tests use JUnit 5 and Mockito only. They need no database and no other service running.

- **AuthServiceTest**: correct password returns token and records login; wrong password is rejected with generic message; inactive user cannot log in; unknown user gets same message
- **TokenRoundTripTest**: issued token carries contract claims; staff id is null when user has no staff link; expired token is rejected; token signed with another secret is rejected; short secret is refused
