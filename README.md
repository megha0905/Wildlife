# api-gateway

The single entry point for the frontend. It checks the JWT on every request, then forwards the request to the right service based on the path. It has no database and no business logic.

| | |
|---|---|
| Owner | Team Lead |
| Port | 8080 |
| Database | none |
| Package | `com.wildlife.gateway` |
| Built with | Spring Cloud Gateway (reactive, WebFlux) |

## Run it

```bash
# from the repo root
set -a; source .env; set +a      # JWT_SECRET must be set, same value as the services
cd api-gateway
mvn spring-boot:run
```

The gateway starts even if no service is running. Requests to a service that is down get a 503 in the standard error format: `"incident service is unavailable"`.

- Health of the gateway itself: http://localhost:8080/actuator/health
- Health of every service (needs a token): `GET http://localhost:8080/api/gateway/health` → `{"services": {"auth": "UP", "incident": "DOWN", ...}}`. The frontend's sidebar status dot uses this.

## Environment variables

| Variable | Default | Notes |
|---|---|---|
| `AUTH_SERVICE_URL` | `http://localhost:8081` | base url of auth-service |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated origins allowed to call the gateway |
| `HABITAT_SERVICE_URL` | `http://localhost:8082` | base url of habitat-service |
| `INCIDENT_SERVICE_URL` | `http://localhost:8086` | base url of incident-service |
| `JWT_SECRET` | `*(none)*` | **Required.** Same value as every other service, at least 32 characters. |
| `PATROL_SERVICE_URL` | `http://localhost:8085` | base url of patrol-service |
| `PROGRAMME_SERVICE_URL` | `http://localhost:8088` | base url of programme-service |
| `REPORTING_SERVICE_URL` | `http://localhost:8089` | base url of reporting-service |
| `RESCUE_SERVICE_URL` | `http://localhost:8087` | base url of rescue-service |
| `RESOURCE_SERVICE_URL` | `http://localhost:8084` | base url of resource-service |
| `SERVER_PORT` | `8080` | HTTP port |
| `WILDLIFE_SERVICE_URL` | `http://localhost:8083` | base url of wildlife-service |

## Routes

| Path prefix | Goes to |
|---|---|
| `/api/auth/**`, `/api/users/**` | auth-service (`AUTH_SERVICE_URL`) |
| `/api/areas/**`, `/api/zones/**`, `/api/habitats/**`, `/api/water-resources/**` | habitat-service |
| `/api/species/**`, `/api/animals/**` | wildlife-service |
| `/api/staff/**`, `/api/vehicles/**`, `/api/equipment/**`, `/api/allocations/**`, `/api/resources/**` | resource-service |
| `/api/patrols/**` | patrol-service |
| `/api/incidents/**` | incident-service |
| `/api/rescues/**` | rescue-service |
| `/api/programmes/**` | programme-service |
| `/api/reports/**` | reporting-service |

Routes are defined in `src/main/resources/application.yml`. Paths are forwarded unchanged (no prefix stripping), so `GET :8080/api/incidents/5` becomes `GET :8086/api/incidents/5`.

## How security works here

1. `CorsConfig` answers browser preflight requests and allows `CORS_ALLOWED_ORIGINS` (the Vite dev server by default).
2. `JwtAuthenticationWebFilter` runs before routing. `POST /api/auth/login`, `OPTIONS` requests and anything outside `/api/` pass without a token. Everything else needs `Authorization: Bearer <token>` with a valid signature (HS256, `JWT_SECRET`), issuer `wildguard-auth` and an unexpired `exp`. Otherwise: 401 in the standard error format.
3. The request is forwarded **with the original Authorization header**. Each service validates the token again and applies its own role rules (`@PreAuthorize`). The gateway only answers "is this a real, current login?"; roles are checked by the services.

`GatewayErrorHandler` turns connection failures and timeouts into 503 and anything else into the standard `{timestamp, status, error, message, path, details[]}` body, so the frontend handles every error the same way.

## Code layout

```
src/main/java/com/wildlife/gateway/
├── GatewayApplication.java
├── config/       GatewayProperties (service URLs, CORS), JwtValidator, CorsConfig
├── filter/       JwtAuthenticationWebFilter
├── controller/   GatewayHealthController
└── exception/    ApiError, ErrorResponseWriter, GatewayErrorHandler
```

## Tests

```bash
cd api-gateway
mvn test
```

Unit tests use JUnit 5 and Mockito only. They need no database and no other service running.

- **JwtValidatorTest**: accepts valid token; rejects expired wrong issuer and wrong secret
- **JwtAuthenticationWebFilterTest**: login and preflight and actuator are public; everything else under api needs a token
