# WildGuard: Service Contracts

**Smart Wildlife Conservation & Protected Area Management**

This file is the single source of truth. Every service, the gateway and the frontend follow it exactly. If a contract has to change, change it **here first**, in the same pull request as the code, and tell the team.

Contract version: **1.1** (all modules built; see the change log in section 14)

---

## 1. Services, ports, databases

| # | Module | Service folder | Port | Database | Owns base paths | Team member |
|---|--------|----------------|------|----------|-----------------|-------------|
| 01 | Gateway | `api-gateway` | 8080 | none | routes everything under `/api/**` | Team Lead |
| 02 | Authentication | `auth-service` | 8081 | `auth_db` | `/api/auth`, `/api/users` | Member 5 |
| 03 | Habitat & protected areas | `habitat-service` | 8082 | `habitat_db` | `/api/areas`, `/api/zones`, `/api/habitats`, `/api/water-resources` | Member 2 |
| 04 | Wildlife & animal records | `wildlife-service` | 8083 | `wildlife_db` | `/api/species`, `/api/animals` | Member 2 |
| 05 | Field staff & resources | `resource-service` | 8084 | `resource_db` | `/api/staff`, `/api/vehicles`, `/api/equipment`, `/api/allocations`, `/api/resources` | Member 3 |
| 06 | Patrol management | `patrol-service` | 8085 | `patrol_db` | `/api/patrols` | Member 3 |
| 07 | Incident & threat management | `incident-service` | 8086 | `incident_db` | `/api/incidents` | Member 4 |
| 08 | Rescue & rehabilitation | `rescue-service` | 8087 | `rescue_db` | `/api/rescues` | Member 4 |
| 09 | Conservation programmes | `programme-service` | 8088 | `programme_db` | `/api/programmes` | Member 5 |
| 10 | Dashboard & reporting | `reporting-service` | 8089 | none | `/api/reports` | Member 5 |
| 11 | Frontend | `frontend` | 5173 | none | React + TypeScript (Vite) | Team Lead |

All databases live in **one PostgreSQL 16 container** (`docker-compose.yml` in the repo root). Each service connects only to its own database.

---

## 2. Stack and project rules

| Area | Rule |
|---|---|
| Language / build | Java 17, Maven, `spring-boot-starter-parent` **3.3.5** directly in every `pom.xml` |
| Cloud | Spring Cloud **2023.0.3** (gateway, OpenFeign) via `spring-cloud-dependencies` BOM in each POM that needs it |
| Persistence | Spring Data JPA, PostgreSQL driver, Flyway. `spring.jpa.hibernate.ddl-auto: validate` |
| Security | Spring Security, JWT HS256 with `jjwt` **0.12.6** (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) |
| API docs | `springdoc-openapi-starter-webmvc-ui` **2.6.0** (`/swagger-ui.html` on each service's own port) |
| Validation | Bean Validation (`spring-boot-starter-validation`) |
| Tests | JUnit 5 + Mockito (`spring-boot-starter-test`) |
| Frontend | React 18 + TypeScript, Vite 5, React Router 6, Leaflet (OpenStreetMap tiles) for maps |
| **Not allowed** | Eureka, Config Server, Kafka, RabbitMQ, Resilience4j, PostGIS, Redis, a shared parent POM, a shared library |

**Package layout** (every Java service):

```
com.wildlife.<service>
├── <Service>Application.java
├── controller/   REST controllers
├── service/      business logic
├── repository/   Spring Data repositories
├── entity/       JPA entities and enums stored in the DB
├── dto/          request/response records
├── config/       security, JWT filter, Feign, OpenAPI
├── client/       Feign clients (only in services that call others)
└── exception/    ApiError, custom exceptions, @RestControllerAdvice
```

`<service>` values: `gateway`, `auth`, `habitat`, `wildlife`, `resource`, `patrol`, `incident`, `rescue`, `programme`, `reporting`.

Every service exposes `GET /actuator/health` (public) for start-up checks.

---

## 3. Common conventions

### 3.1 IDs, dates, money
- IDs: `Long` in Java, `BIGSERIAL`/`BIGINT` in PostgreSQL, numbers in JSON.
- Cross-service references are **IDs only**, with no foreign keys across databases. Column names end in `_id` (`zone_id`, `staff_id`).
- Where another service's name is useful in lists, a **snapshot** column is stored next to the ID (e.g. `staff_name`), filled at write time.
- Dates: `LocalDate` → `"2026-10-10"`. Date-times: `LocalDateTime` → `"2026-10-10T09:30:00"`, **local time in `APP_TIMEZONE`** (default `Asia/Kolkata`), no offset. PostgreSQL columns: `DATE` and `TIMESTAMP`.
- Money: `BigDecimal` in Java, `NUMERIC(14,2)` in SQL, JSON number, INR.
- Coordinates: `latitude`/`longitude` as `Double` (`DOUBLE PRECISION`), WGS84 decimal degrees. Validation: latitude −90..90, longitude −180..180.
- Enum values travel as their exact UPPER_SNAKE_CASE names.

### 3.2 Pagination
Every list endpoint that can grow is paginated with Spring's standard parameters:

`?page=0&size=20&sort=field,asc` (page is 0-based; default size 20; max size 100)

Response body, **always this shape** (never Spring's raw `Page` JSON):

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 57,
  "totalPages": 3
}
```

Below, `Page<X>` means this wrapper around `X` items. Small, bounded lists (e.g. zones of one area, history of one incident) return plain JSON arrays and are marked `X[]`.

### 3.3 Error body
Every error from every service (and the gateway's own 401s) uses this body, produced by a `@RestControllerAdvice` named `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-10-10T09:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Vehicle KA-05-1234 is already allocated from 2026-10-12T06:00 to 2026-10-12T14:00",
  "path": "/api/allocations",
  "details": []
}
```

`details` is always present (empty array if nothing to add). For validation failures it holds one string per field: `"tagId: must not be blank"`.

| Situation | Status |
|---|---|
| Bean Validation failure, malformed JSON, bad enum value, bad query param | 400 Bad Request |
| Missing/invalid/expired token | 401 Unauthorized |
| Valid token but role not allowed | 403 Forbidden |
| Entity not found | 404 Not Found |
| Duplicate unique value, illegal workflow transition, allocation overlap, delete while referenced | 409 Conflict |
| Business rule violated by otherwise valid input (e.g. end before start) | 422 Unprocessable Entity |
| Unexpected exception | 500 Internal Server Error (message: `"Unexpected server error"`, no stack trace) |

Suggested exception classes per service (each service has its own copy): `ResourceNotFoundException` → 404, `ConflictException` → 409, `BusinessRuleException` → 422.

### 3.4 Warnings for cross-service calls
Any response DTO that was built with data from another service has a field:

```json
"warnings": ["Zone 7 could not be loaded from habitat-service; zone name omitted."]
```

Rules:
- Feign calls are wrapped in plain `try/catch (Exception e)`. On failure the service **still completes its own work**, logs at WARN, and adds a human-readable string to `warnings`. No Resilience4j.
- `warnings` is an empty array when everything worked. It is never `null`.
- The one exception: when a referenced ID is **required to exist** and the other service answers **404**, the caller returns **422** (e.g. creating an incident in a zone that habitat-service says does not exist). If the other service is merely unreachable, the request proceeds with a warning.

### 3.5 Service-to-service calls
- OpenFeign, `@EnableFeignClients`, URL from config: `@FeignClient(name = "habitat", url = "${services.habitat.url}")`.
- Each calling service declares in `application.yml`:
  ```yaml
  services:
    habitat:
      url: ${HABITAT_SERVICE_URL:http://localhost:8082}
  ```
- Calls go **directly service-to-service**, not through the gateway.
- A Feign `RequestInterceptor` copies the incoming `Authorization` header to the outgoing call, so the downstream service sees the same user and roles.
- Feign timeouts: `connectTimeout: 2000`, `readTimeout: 5000` (ms).

---

## 4. Security

### 4.1 Roles

| Role | Who | Summary |
|---|---|---|
| `ADMIN` | System administrator | Everything, including user management |
| `FOREST_OFFICER` | Divisional / range forest officer | Plans and approves: areas, patrols, verification and closure of incidents, allocations, programmes |
| `RANGER` | Field ranger / forest guard | Field work: reports incidents, logs patrols, records sightings |
| `VET` | Wildlife veterinarian | Health records, rescue treatment and rehabilitation |
| `ANALYST` | Planning / research analyst | Read-only everywhere, reports |

Shorthand in the endpoint tables:
- **ALL** = any authenticated user (all five roles)
- **ADM** = ADMIN, **FO** = FOREST_OFFICER, **RG** = RANGER, **VET** = VET, **AN** = ANALYST

### 4.2 JWT
- Issued only by `auth-service` on `POST /api/auth/login`.
- Algorithm **HS256**. Key = UTF-8 bytes of env `JWT_SECRET` (**at least 32 characters**; services refuse to start if shorter). Use `Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))`.
- Lifetime: env `JWT_EXPIRATION_MINUTES` (default `120`). No refresh tokens; the frontend logs out on 401.
- Header sent by clients: `Authorization: Bearer <token>`.

**Claims** (exact names):

| Claim | Type | Example | Meaning |
|---|---|---|---|
| `iss` | string | `"wildguard-auth"` | Always this value. Validators reject other issuers. |
| `sub` | string | `"officer"` | Username |
| `uid` | number | `2` | User ID in auth_db |
| `name` | string | `"Megha Gupta"` | Full name (for display and "changed by" snapshots) |
| `roles` | string[] | `["FOREST_OFFICER"]` | Role names **without** `ROLE_` prefix |
| `staffId` | number or null | `1` | Linked staff ID in resource-service, if any |
| `iat` | number | epoch seconds | Issued at |
| `exp` | number | epoch seconds | Expiry |

Example payload:
```json
{ "iss": "wildguard-auth", "sub": "officer", "uid": 2, "name": "Megha Gupta",
  "roles": ["FOREST_OFFICER"], "staffId": 1, "iat": 1760068800, "exp": 1760076000 }
```

### 4.3 Validation in each service
- **Gateway:** validates signature, issuer and expiry for every `/api/**` request except public paths, returns 401 in the standard error body if invalid, and forwards the original `Authorization` header unchanged. It does **not** check roles.
- **Every backend service:** has its own `JwtAuthenticationFilter` (a `OncePerRequestFilter`) that validates the token again and builds a `UsernamePasswordAuthenticationToken` with authorities `ROLE_<role>`. Stateless session, CSRF disabled. Role checks with `@PreAuthorize("hasAnyRole('ADMIN','FOREST_OFFICER')")` on controller methods (`@EnableMethodSecurity`).
- The authenticated principal exposes `uid`, `sub`, `name`, `roles`, `staffId` so services can fill "reported by / changed by" fields.
- 401 and 403 responses from Spring Security use the standard error body (custom `AuthenticationEntryPoint` and `AccessDeniedHandler`).

**Public paths** (no token): `POST /api/auth/login`, `GET /actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`. Everything else requires a valid token.

---

## 5. Shared enums

Each service declares its own copy of the enums it uses, with exactly these values.

### Wildlife
| Enum | Values |
|---|---|
| `SpeciesCategory` | `MAMMAL, BIRD, REPTILE, AMPHIBIAN, FISH, INVERTEBRATE` |
| `ConservationStatus` (IUCN) | `LEAST_CONCERN, NEAR_THREATENED, VULNERABLE, ENDANGERED, CRITICALLY_ENDANGERED, EXTINCT_IN_THE_WILD, DATA_DEFICIENT, NOT_EVALUATED` |
| `Sex` | `MALE, FEMALE, UNKNOWN` |
| `HealthStatus` | `HEALTHY, INJURED, SICK, UNDER_TREATMENT, RECOVERING, DECEASED, UNKNOWN` |
| `AnimalStatus` | `IN_WILD, IN_RESCUE, TRANSFERRED, MISSING, DECEASED` |
| `HealthRecordType` | `CHECKUP, OBSERVATION, INJURY_ASSESSMENT, TREATMENT, VACCINATION, POST_MORTEM` |

### Habitat
| Enum | Values |
|---|---|
| `AreaType` | `NATIONAL_PARK, WILDLIFE_SANCTUARY, TIGER_RESERVE, CONSERVATION_RESERVE, COMMUNITY_RESERVE, BIOSPHERE_RESERVE` |
| `ZoneType` | `CORE, BUFFER, ECO_SENSITIVE, TOURISM` |
| `HabitatType` | `GRASSLAND, TROPICAL_DRY_FOREST, TROPICAL_MOIST_FOREST, WETLAND, RIVERINE, DESERT, MONTANE, MANGROVE, SCRUBLAND` |
| `WaterSourceType` | `RIVER, STREAM, LAKE, POND, WATERHOLE, ARTIFICIAL_WATERHOLE, SPRING` |
| `WaterStatus` | `AVAILABLE, LOW, DRY, CONTAMINATED` |
| `HabitatCondition` | `GOOD, FAIR, DEGRADED, CRITICAL` |

### Resources
| Enum | Values |
|---|---|
| `Designation` | `FOREST_OFFICER, RANGE_OFFICER, FOREST_GUARD, VETERINARIAN, DRIVER, TRACKER, RESEARCHER` |
| `StaffStatus` | `ACTIVE, ON_LEAVE, INACTIVE` |
| `VehicleType` | `JEEP, PICKUP, MOTORCYCLE, BOAT, RESCUE_VAN, TRACTOR` |
| `EquipmentType` | `CAMERA_TRAP, GPS_UNIT, RADIO, BINOCULARS, DRONE, TRANQUILIZER_GUN, FIRST_AID_KIT, RESCUE_CAGE, NIGHT_VISION` |
| `ResourceStatus` (vehicles, equipment) | `AVAILABLE, IN_USE, MAINTENANCE, RETIRED` |
| `ResourceKind` | `STAFF, VEHICLE, EQUIPMENT` |
| `AllocationPurpose` | `PATROL, INCIDENT, RESCUE, PROGRAMME, OTHER` |
| `AllocationStatus` | `PLANNED, ACTIVE, COMPLETED, CANCELLED` |

### Patrols
| Enum | Values |
|---|---|
| `Shift` | `MORNING, EVENING, NIGHT` |
| `PatrolStatus` (schedule) | `SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, MISSED` |
| `RouteDifficulty` | `EASY, MODERATE, HARD` |
| `ObservationType` (patrol log) | `ROUTINE, ANIMAL_SIGHTING, SUSPICIOUS_ACTIVITY, SNARE_FOUND, INFRASTRUCTURE_DAMAGE, FIRE, OTHER` |

### Incidents
| Enum | Values |
|---|---|
| `IncidentType` | `WILDLIFE_INJURY, HUMAN_ANIMAL_CONFLICT, POACHING, ILLEGAL_LOGGING, ENCROACHMENT, FOREST_FIRE, OTHER_THREAT` |
| `Severity` | `LOW, MEDIUM, HIGH, CRITICAL` |
| `IncidentStatus` | `REPORTED, VERIFIED, IN_PROGRESS, RESOLVED, CLOSED` |
| `AssignmentRole` | `LEAD, INVESTIGATOR, SUPPORT, VETERINARY` |

### Rescue
| Enum | Values |
|---|---|
| `RescueCause` | `INJURY, ORPHANED, HUMAN_CONFLICT, SNARE, ROAD_ACCIDENT, ILLNESS, DISPLACED, OTHER` |
| `RescueStatus` | `REGISTERED, UNDER_TREATMENT, REHABILITATING, TRANSFERRED, RELEASED` |

### Programmes
| Enum | Values |
|---|---|
| `ProgrammeType` | `SPECIES_RECOVERY, HABITAT_RESTORATION, ANTI_POACHING, COMMUNITY_OUTREACH, RESEARCH_MONITORING, WATER_MANAGEMENT` |
| `ProgrammeStatus` | `PLANNED, ACTIVE, ON_HOLD, COMPLETED, CANCELLED` |
| `ActivityStatus` | `NOT_STARTED, IN_PROGRESS, DONE, BLOCKED` |

### Auth
| Enum | Values |
|---|---|
| `Role` | `ADMIN, FOREST_OFFICER, RANGER, VET, ANALYST` |

---

## 6. Workflows

### 6.1 Incident workflow

```
REPORTED ──► VERIFIED ──► IN_PROGRESS ──► RESOLVED ──► CLOSED
```

| From | To | Allowed roles | Required in request |
|---|---|---|---|
| REPORTED | VERIFIED | ADM, FO | — |
| VERIFIED | IN_PROGRESS | ADM, FO | at least one **active assignment** must exist, else 422 |
| IN_PROGRESS | RESOLVED | ADM, FO, RG, VET | `note` (resolution notes, not blank) else 422 |
| RESOLVED | CLOSED | ADM, FO | — |

- Only these four moves are legal. Anything else (skipping, going back, same status) → **409** with message `"Cannot move incident INC-2026-0004 from REPORTED to RESOLVED"`.
- A role that may call the endpoint but not perform that particular move → **403**.
- Every successful move writes an `incident_status_history` row (from, to, changedByUserId, changedByName, changedAt, note).
- Timestamps set on the incident: `verifiedAt`, `resolvedAt`, `closedAt`.
- New incidents always start as `REPORTED`.
- Status values are never deleted; incidents are never hard-deleted except by ADMIN while still `REPORTED`.

### 6.2 Rescue workflow

```
REGISTERED ──► UNDER_TREATMENT ──► REHABILITATING ──┬──► TRANSFERRED
                                                     └──► RELEASED
```

| From | To | Allowed roles | Required in request |
|---|---|---|---|
| REGISTERED | UNDER_TREATMENT | ADM, FO, VET | — |
| UNDER_TREATMENT | REHABILITATING | ADM, VET | at least one treatment record, else 422 |
| REHABILITATING | TRANSFERRED | ADM, FO, VET | `transfer` object, else 422 |
| REHABILITATING | RELEASED | ADM, FO | `release` object, else 422; the latest rehab progress entry must have `fitForRelease = true`, else 422 |

- Anything else → **409** `"Cannot move rescue case RSC-2026-0002 from REGISTERED to RELEASED"`.
- `TRANSFERRED` and `RELEASED` are final.
- Every move writes a `rescue_status_history` row.
- **Side effect on wildlife-service** (if the case has an `animalId`): see 9.4. Failures add a warning, never block the move.

### 6.3 Patrol schedule workflow

```
SCHEDULED ──► IN_PROGRESS ──► COMPLETED
    │               
    ├──► CANCELLED
    └──► MISSED
```
Legal: SCHEDULED→IN_PROGRESS, SCHEDULED→CANCELLED, SCHEDULED→MISSED, IN_PROGRESS→COMPLETED. Others → 409. Logs can be added only while `IN_PROGRESS` or `COMPLETED` (else 422).

### 6.4 Allocation rules (resource-service)
- An allocation reserves **one** resource (staff member, vehicle or equipment item) for a time window `[startAt, endAt)`.
- `endAt` must be after `startAt`, else 422.
- **Conflict:** a new or updated allocation conflicts if the same `resourceKind` + `resourceId` already has an allocation with status `PLANNED` or `ACTIVE` whose window **overlaps** (`existing.startAt < new.endAt AND new.startAt < existing.endAt`). Conflict → **409** naming the resource and the clashing window.
- Touching windows (one ends exactly when the next starts) are **not** a conflict.
- Cannot allocate a vehicle/equipment in `MAINTENANCE` or `RETIRED`, or staff who are `INACTIVE` or `ON_LEAVE` → 422.
- Status moves: PLANNED→ACTIVE, PLANNED→CANCELLED, ACTIVE→COMPLETED, ACTIVE→CANCELLED. Others → 409.

### 6.5 Programme status workflow (programme-service)

```
PLANNED ──► ACTIVE ──► COMPLETED
   │          │ ▲
   │          ▼ │
   │        ON_HOLD
   └──► CANCELLED ◄── (from PLANNED, ACTIVE or ON_HOLD)
```
Legal: PLANNED→ACTIVE, PLANNED→CANCELLED, ACTIVE→ON_HOLD, ACTIVE→COMPLETED, ACTIVE→CANCELLED, ON_HOLD→ACTIVE, ON_HOLD→CANCELLED. Others → 409. Roles: ADM, FO.
- `COMPLETED` and `CANCELLED` are final, and the programme becomes read-only: editing it or adding/changing activities, targets or progress → 409 `"Programme KNH-ASD-2026 is COMPLETED and read-only"`.
- A programme can be deleted only while `PLANNED` (409 otherwise).

---

## 7. Endpoints

Conventions in the tables:
- All paths are as seen through the gateway (`http://localhost:8080`). Services expose the same paths on their own port.
- Request/response types refer to the DTOs listed under each service.
- `POST` that creates returns **201 Created** with the created DTO and a `Location` header. `DELETE` returns **204**. Others **200**.
- Fields marked `*` are required. Server-generated fields (`id`, codes, timestamps, snapshots) are ignored if sent.

---

### 7.1 auth-service (8081)

| Method | Path | Request | Response | Roles |
|---|---|---|---|---|
| POST | `/api/auth/login` | `LoginRequest` | `LoginResponse` | public |
| GET | `/api/auth/me` | — | `UserResponse` | ALL |
| POST | `/api/auth/change-password` | `ChangePasswordRequest` | 204 | ALL |
| GET | `/api/users?role=&active=&q=` | — | `Page<UserResponse>` | ADM |
| GET | `/api/users/{id}` | — | `UserResponse` | ADM |
| POST | `/api/users` | `CreateUserRequest` | `UserResponse` | ADM |
| PUT | `/api/users/{id}` | `UpdateUserRequest` | `UserResponse` | ADM |
| PATCH | `/api/users/{id}/active` | `{ "active": boolean* }` | `UserResponse` | ADM |
| PUT | `/api/users/{id}/password` | `{ "newPassword": string* }` | 204 | ADM |

DTOs:
```
LoginRequest           { username*, password* }
LoginResponse          { accessToken, tokenType: "Bearer", expiresIn (seconds), user: UserResponse }
UserResponse           { id, username, fullName, email, roles: Role[], staffId, active, createdAt, lastLoginAt }
CreateUserRequest      { username* (3-50, [a-z0-9._-]), password* (min 8), fullName*, email* (email), roles*: Role[] (min 1), staffId }
UpdateUserRequest      { fullName*, email*, roles*: Role[] (min 1), staffId }
ChangePasswordRequest  { currentPassword*, newPassword* (min 8) }
```
Rules: usernames unique, case-insensitive (409). Login with wrong password or inactive user → 401 `"Invalid username or password"`. Passwords stored with BCrypt. An ADMIN cannot deactivate themselves (422).

---

### 7.2 habitat-service (8082)

| Method | Path | Request | Response | Roles |
|---|---|---|---|---|
| GET | `/api/areas?q=&type=&state=` | — | `Page<AreaResponse>` | ALL |
| GET | `/api/areas/{id}` | — | `AreaDetailResponse` | ALL |
| POST | `/api/areas` | `AreaRequest` | `AreaResponse` | ADM, FO |
| PUT | `/api/areas/{id}` | `AreaRequest` | `AreaResponse` | ADM, FO |
| DELETE | `/api/areas/{id}` | — | 204 (409 if it has zones) | ADM |
| GET | `/api/areas/{id}/zones` | — | `ZoneResponse[]` | ALL |
| GET | `/api/areas/summary` | — | `HabitatSummary` | ALL |
| GET | `/api/zones?areaId=&type=&q=` | — | `Page<ZoneResponse>` | ALL |
| GET | `/api/zones/{id}` | — | `ZoneResponse` | ALL |
| POST | `/api/zones` | `ZoneRequest` | `ZoneResponse` | ADM, FO |
| PUT | `/api/zones/{id}` | `ZoneRequest` | `ZoneResponse` | ADM, FO |
| DELETE | `/api/zones/{id}` | — | 204 (409 if it has habitats or water resources) | ADM |
| GET | `/api/habitats?zoneId=&areaId=&type=&condition=` | — | `Page<HabitatResponse>` | ALL |
| GET | `/api/habitats/{id}` | — | `HabitatResponse` | ALL |
| POST | `/api/habitats` | `HabitatRequest` | `HabitatResponse` | ADM, FO |
| PUT | `/api/habitats/{id}` | `HabitatRequest` | `HabitatResponse` | ADM, FO |
| DELETE | `/api/habitats/{id}` | — | 204 | ADM, FO |
| GET | `/api/water-resources?zoneId=&areaId=&type=&status=` | — | `Page<WaterResourceResponse>` | ALL |
| GET | `/api/water-resources/{id}` | — | `WaterResourceResponse` | ALL |
| POST | `/api/water-resources` | `WaterResourceRequest` | `WaterResourceResponse` | ADM, FO |
| PUT | `/api/water-resources/{id}` | `WaterResourceRequest` | `WaterResourceResponse` | ADM, FO |
| PATCH | `/api/water-resources/{id}/status` | `WaterStatusUpdate` | `WaterResourceResponse` | ADM, FO, RG |
| DELETE | `/api/water-resources/{id}` | — | 204 | ADM, FO |

DTOs:
```
AreaRequest          { name*, code* (2-20, unique), areaType*, state*, district, totalAreaSqKm* (>0),
                       establishedYear, centerLatitude*, centerLongitude*, description }
AreaResponse         { id, name, code, areaType, state, district, totalAreaSqKm, establishedYear,
                       centerLatitude, centerLongitude, description, zoneCount }
AreaDetailResponse   AreaResponse + { zones: ZoneResponse[] }
ZoneRequest          { areaId*, name*, code* (unique), zoneType*, areaSqKm (>0),
                       centerLatitude*, centerLongitude*, description }
ZoneResponse         { id, areaId, areaName, name, code, zoneType, areaSqKm,
                       centerLatitude, centerLongitude, description }
HabitatRequest       { zoneId*, name*, habitatType*, condition*, areaSqKm (>0),
                       latitude, longitude, dominantVegetation, lastSurveyDate, notes }
HabitatResponse      { id, zoneId, zoneName, areaId, name, habitatType, condition, areaSqKm,
                       latitude, longitude, dominantVegetation, lastSurveyDate, notes }
WaterResourceRequest { zoneId*, name*, sourceType*, status*, perennial* (boolean),
                       latitude*, longitude*, capacityCubicM (>0), lastInspectedAt, notes }
WaterResourceResponse{ id, zoneId, zoneName, areaId, name, sourceType, status, perennial,
                       latitude, longitude, capacityCubicM, lastInspectedAt, notes }
WaterStatusUpdate    { status*, inspectedAt (defaults to now), notes }
HabitatSummary       { totalAreas, totalZones, totalAreaSqKm, habitatsByCondition: {HabitatCondition: count},
                       waterByStatus: {WaterStatus: count}, waterSourcesNeedingAttention (LOW+DRY+CONTAMINATED) }
```
`areaId` on habitat and water resource responses is derived from the zone (not stored separately).

---

### 7.3 wildlife-service (8083)

| Method | Path | Request | Response | Roles |
|---|---|---|---|---|
| GET | `/api/species?q=&category=&conservationStatus=` | — | `Page<SpeciesResponse>` | ALL |
| GET | `/api/species/{id}` | — | `SpeciesResponse` | ALL |
| POST | `/api/species` | `SpeciesRequest` | `SpeciesResponse` | ADM, FO |
| PUT | `/api/species/{id}` | `SpeciesRequest` | `SpeciesResponse` | ADM, FO |
| DELETE | `/api/species/{id}` | — | 204 (409 if animals use it) | ADM |
| GET | `/api/animals?q=&speciesId=&healthStatus=&status=&areaId=&zoneId=` | — | `Page<AnimalResponse>` | ALL |
| GET | `/api/animals/{id}` | — | `AnimalResponse` | ALL |
| GET | `/api/animals/tag/{tagId}` | — | `AnimalResponse` | ALL |
| POST | `/api/animals` | `AnimalRequest` | `AnimalResponse` | ADM, FO, RG, VET |
| PUT | `/api/animals/{id}` | `AnimalRequest` | `AnimalResponse` | ADM, FO, RG, VET |
| PATCH | `/api/animals/{id}/status` | `AnimalStatusUpdate` | `AnimalResponse` | ADM, FO, RG, VET (RG because rescue-service forwards a ranger's token when a ranger registers a rescue) |
| PATCH | `/api/animals/{id}/sighting` | `SightingUpdate` | `AnimalResponse` | ADM, FO, RG, VET |
| DELETE | `/api/animals/{id}` | — | 204 | ADM |
| GET | `/api/animals/{id}/health-records` | — | `HealthRecordResponse[]` | ALL |
| POST | `/api/animals/{id}/health-records` | `HealthRecordRequest` | `HealthRecordResponse` | ADM, FO, VET |
| GET | `/api/animals/summary` | — | `WildlifeSummary` | ALL |

DTOs:
```
SpeciesRequest      { commonName* (unique), scientificName* (unique), category*, conservationStatus*,
                      protectionSchedule (e.g. "Schedule I"), description }
SpeciesResponse     { id, commonName, scientificName, category, conservationStatus,
                      protectionSchedule, description, animalCount }
AnimalRequest       { tagId* (unique, stored upper-case, [A-Z0-9-]{3,30}), name, speciesId*, sex*,
                      birthYear (<= current year), birthYearEstimated (default true),
                      healthStatus*, status*, identificationMarks,
                      areaId, zoneId, lastSeenLatitude, lastSeenLongitude, lastSeenAt, notes }
AnimalResponse      { id, tagId, name, speciesId, speciesCommonName, speciesScientificName,
                      conservationStatus, sex, birthYear, birthYearEstimated, ageYears,
                      healthStatus, status, identificationMarks,
                      areaId, zoneId, zoneName, lastSeenLatitude, lastSeenLongitude, lastSeenAt,
                      notes, createdAt, updatedAt, warnings[] }
AnimalStatusUpdate  { status*, healthStatus, latitude, longitude, areaId, zoneId,
                      sourceReference (e.g. "RESCUE:RSC-2026-0002"), note }
SightingUpdate      { seenAt*, latitude*, longitude*, zoneId, areaId, healthStatus, sourceReference, note }
HealthRecordRequest { recordDate*, recordType*, healthStatus*, weightKg (>0), diagnosis,
                      treatment, vetName, notes }
HealthRecordResponse{ id, animalId, recordDate, recordType, healthStatus, weightKg, diagnosis,
                      treatment, vetName, recordedByUserId, recordedByName, notes, createdAt }
WildlifeSummary     { totalSpecies, totalAnimals, byHealthStatus: {HealthStatus: count},
                      byStatus: {AnimalStatus: count}, byConservationStatus: {ConservationStatus: count},
                      needingAttention (INJURED+SICK+UNDER_TREATMENT), notSeenFor30Days }
```
Rules:
- `ageYears` = current year − `birthYear` (null if unknown).
- `zoneName` is the snapshot name from habitat-service. When `zoneId` is set on create/update, wildlife-service calls habitat `GET /api/zones/{id}`: 404 → 422; unreachable → saved with a warning and `zoneName` null. If the zone belongs to another area than `areaId`, 422. If `areaId` is missing it is taken from the zone.
- A new health record updates the animal's `healthStatus` only if it is the newest health record by `recordDate`. `DECEASED` health also sets status `DECEASED`.
- `SightingUpdate` moves last-seen fields forward only if `seenAt` is newer than the stored `lastSeenAt`.
- `PATCH /status` with `DECEASED` also sets health `DECEASED`. `IN_WILD` with coordinates also updates last-seen location and time (used on release).

---

### 7.4 resource-service (8084)

| Method | Path | Request | Response | Roles |
|---|---|---|---|---|
| GET | `/api/staff?q=&designation=&status=&areaId=` | — | `Page<StaffResponse>` | ALL |
| GET | `/api/staff/{id}` | — | `StaffResponse` | ALL |
| GET | `/api/staff/lookup?ids=1,2,3` | — | `StaffRef[]` | ALL |
| POST | `/api/staff` | `StaffRequest` | `StaffResponse` | ADM, FO |
| PUT | `/api/staff/{id}` | `StaffRequest` | `StaffResponse` | ADM, FO |
| DELETE | `/api/staff/{id}` | — | 204 (409 if any allocation exists; deactivate instead) | ADM |
| GET | `/api/vehicles?type=&status=&areaId=` | — | `Page<VehicleResponse>` | ALL |
| GET | `/api/vehicles/{id}` | — | `VehicleResponse` | ALL |
| POST | `/api/vehicles` | `VehicleRequest` | `VehicleResponse` | ADM, FO |
| PUT | `/api/vehicles/{id}` | `VehicleRequest` | `VehicleResponse` | ADM, FO |
| DELETE | `/api/vehicles/{id}` | — | 204 (409 if allocated) | ADM |
| GET | `/api/equipment?type=&status=&areaId=` | — | `Page<EquipmentResponse>` | ALL |
| GET | `/api/equipment/{id}` | — | `EquipmentResponse` | ALL |
| POST | `/api/equipment` | `EquipmentRequest` | `EquipmentResponse` | ADM, FO |
| PUT | `/api/equipment/{id}` | `EquipmentRequest` | `EquipmentResponse` | ADM, FO |
| DELETE | `/api/equipment/{id}` | — | 204 (409 if allocated) | ADM |
| GET | `/api/allocations?resourceKind=&resourceId=&purpose=&referenceId=&status=&from=&to=` | — | `Page<AllocationResponse>` | ALL |
| GET | `/api/allocations/{id}` | — | `AllocationResponse` | ALL |
| GET | `/api/allocations/availability?resourceKind=&resourceId=&startAt=&endAt=` | — | `AvailabilityResponse` | ALL |
| POST | `/api/allocations` | `AllocationRequest` | `AllocationResponse` | ADM, FO |
| PUT | `/api/allocations/{id}` | `AllocationRequest` | `AllocationResponse` | ADM, FO |
| PATCH | `/api/allocations/{id}/status` | `{ "status": AllocationStatus* }` | `AllocationResponse` | ADM, FO, RG (RG because patrol-service forwards a ranger's token when a patrol starts or completes) |
| GET | `/api/resources/summary` | — | `ResourceSummary` | ALL |

DTOs:
```
StaffRequest        { employeeCode* (unique), fullName*, designation*, status*, phone, email,
                      homeAreaId, joinedOn }
StaffResponse       { id, employeeCode, fullName, designation, status, phone, email, homeAreaId, joinedOn }
StaffRef            { id, employeeCode, fullName, designation, status }
VehicleRequest      { registrationNumber* (unique), vehicleType*, status*, make, model,
                      seatingCapacity (>0), homeAreaId, lastServicedOn }
VehicleResponse     VehicleRequest fields + { id }
EquipmentRequest    { assetTag* (unique), name*, equipmentType*, status*, homeAreaId, purchasedOn, notes }
EquipmentResponse   EquipmentRequest fields + { id }
AllocationRequest   { resourceKind*, resourceId*, purpose*, referenceId, referenceLabel,
                      startAt*, endAt*, notes }
AllocationResponse  { id, resourceKind, resourceId, resourceLabel, purpose, referenceId, referenceLabel,
                      startAt, endAt, status, allocatedByUserId, allocatedByName, notes, createdAt }
AvailabilityResponse{ resourceKind, resourceId, startAt, endAt, available: boolean,
                      conflicts: AllocationResponse[] }
ResourceSummary     { totalStaff, staffByStatus: {StaffStatus: count}, staffByDesignation: {Designation: count},
                      vehiclesByStatus: {ResourceStatus: count}, equipmentByStatus: {ResourceStatus: count},
                      activeAllocations, allocationsToday }
```
- `resourceLabel` is computed inside resource-service (staff name, vehicle registration, or asset tag + name).
- `referenceId` + `purpose` link to the other service's record (e.g. purpose `PATROL`, referenceId = patrol schedule ID). `referenceLabel` is a free-text snapshot such as `"Patrol PS-12, Mukki core, 12 Oct morning"`.
- **Vehicle / equipment `status` is not changed automatically by allocations**; `IN_USE` is set by users. Conflicts are checked only against allocations (6.4).

---

### 7.5 patrol-service (8085)

| Method | Path | Request | Response | Roles |
|---|---|---|---|---|
| GET | `/api/patrols/teams?areaId=&active=` | — | `Page<TeamResponse>` | ALL |
| GET | `/api/patrols/teams/{id}` | — | `TeamResponse` | ALL |
| POST | `/api/patrols/teams` | `TeamRequest` | `TeamResponse` | ADM, FO |
| PUT | `/api/patrols/teams/{id}` | `TeamRequest` | `TeamResponse` | ADM, FO |
| DELETE | `/api/patrols/teams/{id}` | — | 204 (409 if it has schedules) | ADM |
| GET | `/api/patrols/routes?areaId=&zoneId=` | — | `Page<RouteResponse>` | ALL |
| GET | `/api/patrols/routes/{id}` | — | `RouteResponse` | ALL |
| POST | `/api/patrols/routes` | `RouteRequest` | `RouteResponse` | ADM, FO |
| PUT | `/api/patrols/routes/{id}` | `RouteRequest` | `RouteResponse` | ADM, FO |
| DELETE | `/api/patrols/routes/{id}` | — | 204 (409 if it has schedules) | ADM |
| GET | `/api/patrols/schedules?teamId=&routeId=&zoneId=&status=&from=&to=` | — | `Page<ScheduleResponse>` | ALL |
| GET | `/api/patrols/schedules/{id}` | — | `ScheduleDetailResponse` | ALL |
| POST | `/api/patrols/schedules` | `ScheduleRequest` | `ScheduleResponse` | ADM, FO |
| PUT | `/api/patrols/schedules/{id}` | `ScheduleRequest` | `ScheduleResponse` (only while `SCHEDULED`, else 409) | ADM, FO |
| PATCH | `/api/patrols/schedules/{id}/status` | `ScheduleStatusUpdate` | `ScheduleResponse` | ADM, FO, RG |
| GET | `/api/patrols/schedules/{id}/logs` | — | `LogResponse[]` | ALL |
| POST | `/api/patrols/schedules/{id}/logs` | `LogRequest` | `LogResponse` | ADM, FO, RG |
| POST | `/api/patrols/logs/{logId}/escalate` | `EscalateRequest` | `LogResponse` (with `incidentId`) | ADM, FO, RG |
| GET | `/api/patrols/summary` | — | `PatrolSummary` | ALL |

DTOs:
```
TeamRequest          { name* (unique), areaId*, leaderStaffId*, memberStaffIds*: Long[] (min 1), active (default true) }
TeamResponse         { id, name, areaId, leaderStaffId, leaderName, members: StaffRef[], active, warnings[] }
Waypoint             { sequence*, latitude*, longitude*, label }
RouteRequest         { name*, areaId*, zoneId*, distanceKm* (>0), estimatedHours (>0), difficulty*,
                       waypoints: Waypoint[] (stored in table patrol_route_waypoints), description }
RouteResponse        RouteRequest fields + { id, zoneName, warnings[] }
ScheduleRequest      { teamId*, routeId*, patrolDate*, shift*, startTime* ("06:00"), endTime* ("12:00"),
                       vehicleId, objective }
ScheduleResponse     { id, code ("PS-<id>"), teamId, teamName, routeId, routeName, areaId, zoneId, zoneName,
                       patrolDate, shift, startTime, endTime, status, vehicleId, allocationIds: Long[],
                       objective, startedAt, completedAt, warnings[] }
ScheduleDetailResponse ScheduleResponse + { team: TeamResponse, route: RouteResponse, logs: LogResponse[] }
ScheduleStatusUpdate { status*, note }
LogRequest           { loggedAt*, observationType*, latitude*, longitude*, description*,
                       animalTagId, severityHint (Severity) }
LogResponse          { id, scheduleId, loggedAt, observationType, latitude, longitude, description,
                       animalTagId, severityHint, reportedByUserId, reportedByName, incidentId, createdAt, warnings[] }
EscalateRequest      { incidentType*, severity*, title* }
PatrolSummary        { schedulesToday, schedulesThisMonthByStatus: {PatrolStatus: count},
                       logsThisMonth, suspiciousActivityThisMonth, distancePatrolledKmThisMonth (completed only) }
```
Integration:
- Team create/update: look up `leaderStaffId` + `memberStaffIds` via resource `GET /api/staff/lookup`; unknown IDs → 422; unreachable → warning, names null.
- Route create/update: verify `zoneId` via habitat `GET /api/zones/{id}` (404 → 422; zone not in `areaId` → 422; unreachable → warning).
- Schedule create: creates one **allocation** per team member (purpose `PATROL`, `referenceId` = schedule ID) and one for `vehicleId` if given, via resource `POST /api/allocations`. A **409 from resource-service fails the whole schedule create with 409** and rolls back any allocations already made (best-effort `PATCH .../status CANCELLED`). If resource-service is unreachable, the schedule is saved with a warning and empty `allocationIds`.
- Schedule `CANCELLED`: cancels its allocations (best effort, warning on failure). `IN_PROGRESS`: sets allocations `ACTIVE`. `COMPLETED`: sets them `COMPLETED`.
- Log with `animalTagId`: calls wildlife `PATCH /api/animals/{id}/sighting` after resolving the tag via `GET /api/animals/tag/{tagId}` (failures → warning only).
- Escalate: calls incident `POST /api/incidents` with `sourcePatrolLogId`, location, zone and area from the schedule's route; stores the returned `incidentId` on the log. Already escalated → 409. Incident-service unreachable → **503** (nothing to degrade to).

---

### 7.6 incident-service (8086)

| Method | Path | Request | Response | Roles |
|---|---|---|---|---|
| GET | `/api/incidents?type=&severity=&status=&areaId=&zoneId=&from=&to=&q=` | — | `Page<IncidentResponse>` | ALL |
| GET | `/api/incidents/{id}` | — | `IncidentDetailResponse` | ALL |
| POST | `/api/incidents` | `IncidentRequest` | `IncidentResponse` | ADM, FO, RG, VET |
| PUT | `/api/incidents/{id}` | `IncidentRequest` | `IncidentResponse` (not when `CLOSED` → 409) | ADM, FO |
| DELETE | `/api/incidents/{id}` | — | 204 (only while `REPORTED`, else 409) | ADM |
| POST | `/api/incidents/{id}/transition` | `IncidentTransitionRequest` | `IncidentResponse` | ADM, FO, RG, VET (per-move rules in 6.1) |
| GET | `/api/incidents/{id}/history` | — | `StatusHistoryResponse[]` | ALL |
| GET | `/api/incidents/{id}/assignments` | — | `AssignmentResponse[]` | ALL |
| POST | `/api/incidents/{id}/assignments` | `AssignmentRequest` | `AssignmentResponse` | ADM, FO |
| DELETE | `/api/incidents/{id}/assignments/{assignmentId}` | — | 204 (sets `active=false`) | ADM, FO |
| **GET** | **`/api/incidents/zone/{zoneId}/current-month?status=`** | — | **`ZoneMonthlyIncidentReport`** | **ALL** |
| GET | `/api/incidents/summary?year=&month=` | — | `IncidentSummary` | ALL |

DTOs:
```
IncidentRequest          { title* (max 150), incidentType*, severity*, description*,
                           areaId*, zoneId*, latitude*, longitude*, locationDescription,
                           occurredAt* (not in future), animalId, speciesId, sourcePatrolLogId }
IncidentResponse         { id, incidentCode ("INC-2026-0001"), title, incidentType, severity, status, description,
                           areaId, zoneId, zoneName, latitude, longitude, locationDescription,
                           occurredAt, reportedAt, reportedByUserId, reportedByName,
                           verifiedAt, resolvedAt, closedAt, resolutionNotes,
                           animalId, speciesId, sourcePatrolLogId,
                           assignedPersonnel: AssignedPersonnel[], warnings[] }
IncidentDetailResponse   IncidentResponse + { history: StatusHistoryResponse[] }
AssignedPersonnel        { assignmentId, staffId, staffName, designation, assignmentRole, assignedAt }
IncidentTransitionRequest{ toStatus*, note }
StatusHistoryResponse    { id, fromStatus (null for creation), toStatus, changedByUserId, changedByName, changedAt, note }
AssignmentRequest        { staffId*, assignmentRole*, note }
AssignmentResponse       { id, incidentId, staffId, staffName, designation, assignmentRole, assignedAt,
                           assignedByName, active, note, allocationId, warnings[] }
IncidentSummary          { year, month, totalThisMonth, openIncidents (REPORTED+VERIFIED+IN_PROGRESS),
                           byStatus: {IncidentStatus: count}, byType: {IncidentType: count},
                           bySeverity: {Severity: count}, criticalOpen }
```

**Mandatory endpoint: monthly zone review**

`GET /api/incidents/zone/{zoneId}/current-month?status=VERIFIED`

- "Current month" = from the 1st of the current month 00:00 (inclusive) to the 1st of next month 00:00 (exclusive), in `APP_TIMEZONE`, matched on **`occurredAt`**.
- `status` is optional. When given, `incidents` contains only that status. **`statusSummary` always counts all five statuses for the month, ignoring the filter**, so the screen can show the full picture while the list is filtered.
- Incidents sorted by `occurredAt` descending. Not paginated (bounded to one zone and one month).
- `zoneName` and `areaName` come from habitat `GET /api/zones/{zoneId}`. If habitat is unreachable, they are null and a warning is added. If habitat answers 404, the endpoint still returns 200 with an empty or full list (incidents are stored data) plus a warning `"Zone 9 does not exist in habitat-service"`.
- Invalid `status` value → 400.

```
ZoneMonthlyIncidentReport {
  zoneId, zoneName, areaId, areaName,
  month: "2026-10",
  periodStart: "2026-10-01T00:00:00",
  periodEnd:   "2026-11-01T00:00:00",
  statusFilter: "VERIFIED" | null,
  totalIncidents,                  // count across all statuses this month
  statusSummary: { "REPORTED": 2, "VERIFIED": 1, "IN_PROGRESS": 3, "RESOLVED": 1, "CLOSED": 0 },
  incidents: [ {
      id, incidentCode, title, incidentType, severity, status,
      occurredAt, latitude, longitude, locationDescription,
      assignedPersonnel: [ { assignmentId, staffId, staffName, designation, assignmentRole, assignedAt } ]
  } ],
  warnings: []
}
```

Other rules:
- `incidentCode` = `INC-<year of reportedAt>-<4-digit sequence>` from a PostgreSQL sequence.
- Create: verifies `zoneId` via habitat (404 → 422; zone not in `areaId` → 422; unreachable → saved with warning). Writes the first history row (`fromStatus` null → `REPORTED`).
- Assignment create: looks up staff via resource `GET /api/staff/{id}` (404 → 422; unreachable → saved with warning and null name) and stores `staffName` + `designation` snapshots. Staff already actively assigned to this incident → 409. Also creates an allocation (purpose `INCIDENT`, `referenceId` = incident ID, window now → now + 24h); a 409 from resource-service becomes a **warning** (the assignment still stands), and `allocationId` stays null.
- Only active assignments count as "assigned personnel".

---

### 7.7 rescue-service (8087)

| Method | Path | Request | Response | Roles |
|---|---|---|---|---|
| GET | `/api/rescues?status=&cause=&areaId=&zoneId=&from=&to=&q=` | — | `Page<RescueResponse>` | ALL |
| GET | `/api/rescues/{id}` | — | `RescueDetailResponse` | ALL |
| POST | `/api/rescues` | `RescueRequest` | `RescueResponse` | ADM, FO, RG, VET |
| PUT | `/api/rescues/{id}` | `RescueRequest` | `RescueResponse` (not when final → 409) | ADM, FO, VET |
| POST | `/api/rescues/{id}/transition` | `RescueTransitionRequest` | `RescueResponse` | ADM, FO, VET (per-move rules in 6.2) |
| GET | `/api/rescues/{id}/treatments` | — | `TreatmentResponse[]` | ALL |
| POST | `/api/rescues/{id}/treatments` | `TreatmentRequest` | `TreatmentResponse` | ADM, VET |
| GET | `/api/rescues/{id}/rehab-progress` | — | `RehabProgressResponse[]` | ALL |
| POST | `/api/rescues/{id}/rehab-progress` | `RehabProgressRequest` | `RehabProgressResponse` | ADM, FO, VET |
| GET | `/api/rescues/{id}/history` | — | `RescueHistoryResponse[]` | ALL |
| GET | `/api/rescues/summary` | — | `RescueSummary` | ALL |

DTOs:
```
RescueRequest           { animalId, speciesId, speciesName*, animalDescription, cause*, intakeCondition* (HealthStatus),
                          rescuedAt* (not in future), latitude*, longitude*, areaId, zoneId,
                          incidentId, facilityName*, notes }
RescueResponse          { id, caseNumber ("RSC-2026-0001"), animalId, animalTagId, speciesId, speciesName,
                          animalDescription, cause, intakeCondition, status, rescuedAt, latitude, longitude,
                          areaId, zoneId, incidentId, facilityName, registeredByName, notes,
                          transfer: TransferInfo | null, release: ReleaseInfo | null,
                          createdAt, updatedAt, warnings[] }
RescueDetailResponse    RescueResponse + { treatments: TreatmentResponse[], rehabProgress: RehabProgressResponse[],
                          history: RescueHistoryResponse[] }
RescueTransitionRequest { toStatus*, note, transfer: TransferInfo, release: ReleaseInfo }
TransferInfo            { destinationFacility*, reason*, transferDate* }
ReleaseInfo             { releaseDate*, releaseAreaId*, releaseZoneId*, latitude*, longitude*, notes }
TreatmentRequest        { treatmentDate*, diagnosis*, procedure*, medications, vetStaffId, vetName*, notes }
TreatmentResponse       TreatmentRequest fields + { id, rescueId, createdAt }
RehabProgressRequest    { assessedOn*, enclosure, behaviourNotes*, weightKg (>0), fitForRelease* (boolean), assessedBy* }
RehabProgressResponse   RehabProgressRequest fields + { id, rescueId, createdAt }
RescueHistoryResponse   { id, fromStatus, toStatus, changedByUserId, changedByName, changedAt, note }
RescueSummary           { activeCases (REGISTERED+UNDER_TREATMENT+REHABILITATING), byStatus: {RescueStatus: count},
                          byCause: {RescueCause: count}, releasedThisYear, transferredThisYear, admittedThisMonth }
```
Integration:
- Create with `animalId`: verifies via wildlife `GET /api/animals/{id}` (404 → 422; unreachable → warning) and copies `tagId` into `animalTagId`.
- Create with `incidentId`: verifies via incident `GET /api/incidents/{id}` (404 → 422; unreachable → warning).
- See 9.4 for wildlife status updates on transitions.
- `caseNumber` = `RSC-<year>-<4-digit sequence>`.

---

### 7.8 programme-service (8088)

| Method | Path | Request | Response | Roles |
|---|---|---|---|---|
| GET | `/api/programmes?status=&type=&areaId=&q=` | — | `Page<ProgrammeResponse>` | ALL |
| GET | `/api/programmes/{id}` | — | `ProgrammeDetailResponse` | ALL |
| POST | `/api/programmes` | `ProgrammeRequest` | `ProgrammeResponse` | ADM, FO |
| PUT | `/api/programmes/{id}` | `ProgrammeRequest` | `ProgrammeResponse` | ADM, FO |
| PATCH | `/api/programmes/{id}/status` | `{ "status": ProgrammeStatus* }` | `ProgrammeResponse` | ADM, FO |
| DELETE | `/api/programmes/{id}` | — | 204 (only while `PLANNED`) | ADM |
| GET | `/api/programmes/{id}/activities` | — | `ActivityResponse[]` | ALL |
| POST | `/api/programmes/{id}/activities` | `ActivityRequest` | `ActivityResponse` | ADM, FO |
| PUT | `/api/programmes/{id}/activities/{activityId}` | `ActivityRequest` | `ActivityResponse` | ADM, FO, RG |
| DELETE | `/api/programmes/{id}/activities/{activityId}` | — | 204 | ADM, FO |
| GET | `/api/programmes/{id}/targets` | — | `TargetResponse[]` | ALL |
| POST | `/api/programmes/{id}/targets` | `TargetRequest` | `TargetResponse` | ADM, FO |
| PUT | `/api/programmes/{id}/targets/{targetId}` | `TargetRequest` | `TargetResponse` | ADM, FO |
| DELETE | `/api/programmes/{id}/targets/{targetId}` | — | 204 | ADM, FO |
| GET | `/api/programmes/{id}/progress` | — | `ProgressUpdateResponse[]` | ALL |
| POST | `/api/programmes/{id}/progress` | `ProgressUpdateRequest` | `ProgressUpdateResponse` | ADM, FO, RG, VET |
| GET | `/api/programmes/summary` | — | `ProgrammeSummary` | ALL |

DTOs:
```
ProgrammeRequest       { code* (unique), name*, programmeType*, objective*, description,
                         startDate*, endDate* (>= startDate), budgetInr (>=0), leadStaffId,
                         areaIds: Long[], speciesIds: Long[] }
ProgrammeResponse      { id, code, name, programmeType, status, objective, description, startDate, endDate,
                         budgetInr, leadStaffId, leadStaffName, areaIds, speciesIds,
                         progressPercent, activityCount, activitiesDone, targetCount, warnings[] }
ProgrammeDetailResponse ProgrammeResponse + { activities: ActivityResponse[], targets: TargetResponse[],
                          recentProgress: ProgressUpdateResponse[] (latest 10) }
ActivityRequest        { title*, description, plannedStart*, plannedEnd* (>= plannedStart), status*,
                         assignedStaffId, areaId }
ActivityResponse       ActivityRequest fields + { id, programmeId, assignedStaffName, overdue (boolean) }
TargetRequest          { metricName*, unit*, baselineValue*, targetValue*, currentValue (defaults to baseline), dueDate }
TargetResponse         TargetRequest fields + { id, programmeId, progressPercent, lastUpdatedAt }
ProgressUpdateRequest  { targetId, updateDate*, newValue (required if targetId set), note* }
ProgressUpdateResponse { id, programmeId, targetId, targetMetricName, updateDate, previousValue, newValue,
                         note, reportedByUserId, reportedByName, createdAt }
ProgrammeSummary       { byStatus: {ProgrammeStatus: count}, byType: {ProgrammeType: count},
                         activeProgrammes, averageProgressPercent (active only), overdueActivities,
                         totalBudgetInrActive }
```
Rules:
- Target `progressPercent` = `(current − baseline) / (target − baseline) × 100`, clamped 0..100, rounded to 1 decimal. If `target == baseline`, 100 when `current == target`, else 0. Works for decreasing targets too (e.g. snare count from 40 → 0).
- Programme `progressPercent` = average of its targets' progress; 0 if no targets.
- A progress update with `targetId` sets that target's `currentValue = newValue` and records `previousValue`.
- Activity `overdue` = status not `DONE` and `plannedEnd` < today.
- `leadStaffName` and `assignedStaffName` come from resource `GET /api/staff/lookup` at read time (unreachable → null + warning).

---

### 7.9 reporting-service (8089, no database)

| Method | Path | Request | Response | Roles |
|---|---|---|---|---|
| GET | `/api/reports/dashboard` | — | `DashboardResponse` | ALL |
| GET | `/api/reports/incidents/monthly?year=&month=` | — | `IncidentSummary` (proxied from incident-service) + `warnings[]` | ALL |
| GET | `/api/reports/zones/{zoneId}` | — | `ZoneReport` | ALL |

```
DashboardResponse {
  generatedAt,
  habitat:    HabitatSummary   | null,
  wildlife:   WildlifeSummary  | null,
  resources:  ResourceSummary  | null,
  patrols:    PatrolSummary    | null,
  incidents:  IncidentSummary  | null,   // current month
  rescues:    RescueSummary    | null,
  programmes: ProgrammeSummary | null,
  servicesOnline: { "habitat": true, "wildlife": true, ... },
  warnings: []
}
ZoneReport {
  zone: ZoneResponse | null,
  animalsInZone: number | null,           // wildlife GET /api/animals?zoneId=&size=1 → totalElements
  incidentsThisMonth: ZoneMonthlyIncidentReport | null,
  waterResources: WaterResourceResponse[] | null,   // habitat GET /api/water-resources?zoneId=&size=100
  warnings: []
}
```
Reporting calls each service's `/summary` endpoint in turn, with the caller's token. Any failed call → that section is `null`, `servicesOnline.<name> = false`, and a warning. The endpoint itself always returns 200 unless the caller is unauthenticated.

---

## 8. Cross-service call map

| Caller | Calls | Endpoint | Used for | On 404 | On unreachable |
|---|---|---|---|---|---|
| wildlife | habitat | `GET /api/zones/{id}` | validate zone, snapshot `zoneName` | 422 | warning |
| resource | — | — | — | — | — |
| patrol | resource | `GET /api/staff/lookup?ids=` | team members | 422 if any ID missing | warning |
| patrol | habitat | `GET /api/zones/{id}` | route zone | 422 | warning |
| patrol | resource | `POST /api/allocations`, `PATCH /api/allocations/{id}/status` | reserve team + vehicle | 409 → fail schedule | warning |
| patrol | wildlife | `GET /api/animals/tag/{tagId}`, `PATCH /api/animals/{id}/sighting` | sightings in logs | warning | warning |
| patrol | incident | `POST /api/incidents` | escalate log | — | 503 |
| incident | habitat | `GET /api/zones/{id}` | validate zone, names in monthly view | 422 on create; warning on monthly view | warning |
| incident | resource | `GET /api/staff/{id}`, `POST /api/allocations` | assignment snapshot + allocation | 422 (staff) / warning (allocation 409) | warning |
| rescue | wildlife | `GET /api/animals/{id}`, `PATCH /api/animals/{id}/status` | validate animal, sync status | 422 on create; warning on transition | warning |
| rescue | incident | `GET /api/incidents/{id}` | validate linked incident | 422 | warning |
| programme | resource | `GET /api/staff/lookup?ids=` | staff names | null name | warning |
| reporting | all except auth | `/summary` endpoints, plus 9.3 calls | dashboard and reports | section null | section null + warning |

No cycles at start-up: no service calls another while starting.

---

## 9. Cross-service DTOs (what callers rely on)

Callers declare their **own** copy of these records inside `client/` (e.g. `com.wildlife.incident.client.ZoneDto`). Callers may ignore fields they don't need (`@JsonIgnoreProperties(ignoreUnknown = true)` on every client DTO), but **providers must not rename or remove** these fields without updating this file.

### 9.1 From habitat-service
```
ZoneDto  (GET /api/zones/{id})
{ id, areaId, areaName, name, code, zoneType, areaSqKm, centerLatitude, centerLongitude, description }
```

### 9.2 From resource-service
```
StaffDto (GET /api/staff/{id})            { id, employeeCode, fullName, designation, status, phone, email, homeAreaId, joinedOn }
StaffRef (GET /api/staff/lookup?ids=...)  { id, employeeCode, fullName, designation, status }
AllocationCreate (POST /api/allocations)  { resourceKind, resourceId, purpose, referenceId, referenceLabel, startAt, endAt, notes }
AllocationDto (response)                  { id, resourceKind, resourceId, resourceLabel, purpose, referenceId, startAt, endAt, status }
```

### 9.3 From wildlife-service
```
AnimalDto (GET /api/animals/{id}, GET /api/animals/tag/{tagId})
{ id, tagId, name, speciesId, speciesCommonName, sex, healthStatus, status, areaId, zoneId, lastSeenAt }
AnimalStatusUpdate (PATCH /api/animals/{id}/status)
{ status, healthStatus, latitude, longitude, areaId, zoneId, sourceReference, note }
SightingUpdate (PATCH /api/animals/{id}/sighting)
{ seenAt, latitude, longitude, zoneId, areaId, healthStatus, sourceReference, note }
Page (GET /api/animals?zoneId=..&size=1) → read totalElements only
```

### 9.4 Rescue → wildlife status sync
| Rescue move | PATCH body sent to wildlife |
|---|---|
| case created (with `animalId`) | `{ status: IN_RESCUE, healthStatus: <intakeCondition>, sourceReference: "RESCUE:<caseNumber>" }` |
| → UNDER_TREATMENT | `{ status: IN_RESCUE, healthStatus: UNDER_TREATMENT, sourceReference }` |
| → REHABILITATING | `{ status: IN_RESCUE, healthStatus: RECOVERING, sourceReference }` |
| → TRANSFERRED | `{ status: TRANSFERRED, sourceReference, note: "<destinationFacility>" }` |
| → RELEASED | `{ status: IN_WILD, healthStatus: HEALTHY, latitude, longitude, areaId, zoneId, sourceReference }` |

### 9.5 From incident-service
```
IncidentDto (GET /api/incidents/{id})     { id, incidentCode, title, incidentType, severity, status, areaId, zoneId }
IncidentCreate (POST /api/incidents, from patrol escalation)
{ title, incidentType, severity, description, areaId, zoneId, latitude, longitude,
  locationDescription, occurredAt, sourcePatrolLogId }
ZoneMonthlyIncidentReport (see 7.6)
```

### 9.6 Summaries for reporting
`HabitatSummary`, `WildlifeSummary`, `ResourceSummary`, `PatrolSummary`, `IncidentSummary`, `RescueSummary`, `ProgrammeSummary` exactly as defined in section 7. Map keys are always the **full set** of enum values, with 0 for missing ones, so charts don't need null checks.

---

## 10. Gateway route table (api-gateway, 8080)

Spring Cloud Gateway (reactive). Routes are plain `Path` predicates to URLs from env. No path rewriting.

| Route id | Predicates (`Path=`) | Target URI |
|---|---|---|
| `auth` | `/api/auth/**, /api/users/**` | `${AUTH_SERVICE_URL:http://localhost:8081}` |
| `habitat` | `/api/areas/**, /api/zones/**, /api/habitats/**, /api/water-resources/**` | `${HABITAT_SERVICE_URL:http://localhost:8082}` |
| `wildlife` | `/api/species/**, /api/animals/**` | `${WILDLIFE_SERVICE_URL:http://localhost:8083}` |
| `resource` | `/api/staff/**, /api/vehicles/**, /api/equipment/**, /api/allocations/**, /api/resources/**` | `${RESOURCE_SERVICE_URL:http://localhost:8084}` |
| `patrol` | `/api/patrols/**` | `${PATROL_SERVICE_URL:http://localhost:8085}` |
| `incident` | `/api/incidents/**` | `${INCIDENT_SERVICE_URL:http://localhost:8086}` |
| `rescue` | `/api/rescues/**` | `${RESCUE_SERVICE_URL:http://localhost:8087}` |
| `programme` | `/api/programmes/**` | `${PROGRAMME_SERVICE_URL:http://localhost:8088}` |
| `reporting` | `/api/reports/**` | `${REPORTING_SERVICE_URL:http://localhost:8089}` |

Gateway also:
- Validates JWT (section 4.3) with a `GlobalFilter`, skipping public paths.
- CORS for `${CORS_ALLOWED_ORIGINS:http://localhost:5173}`: methods GET, POST, PUT, PATCH, DELETE, OPTIONS; headers `Authorization`, `Content-Type`; `allowCredentials: false`; preflight `OPTIONS` is allowed without a token.
- Downstream unreachable → **503** in the standard error body, message `"<route id> service is unavailable"`.
- `GET /api/gateway/health` (ALL) → `{ "services": { "auth": "UP"|"DOWN", ... } }` by calling each service's `/actuator/health` (used by the frontend status indicator).

---

## 11. Environment variables

| Variable | Used by | Default (dev) | Notes |
|---|---|---|---|
| `JWT_SECRET` | gateway + all services | none, **required** | ≥ 32 chars, same value everywhere |
| `JWT_EXPIRATION_MINUTES` | auth | `120` | |
| `DB_HOST` | services with DB | `localhost` | |
| `DB_PORT` | services with DB | `5432` | |
| `DB_USERNAME` | services with DB | `wildguard` | |
| `DB_PASSWORD` | services with DB | `wildguard` | |
| `DB_NAME` | services with DB | the service's own DB name (section 1) | rarely overridden |
| `APP_TIMEZONE` | all Java services | `Asia/Kolkata` | "today" and "current month"; also set as JVM default zone at start-up |
| `AUTH_SERVICE_URL` | gateway | `http://localhost:8081` | |
| `HABITAT_SERVICE_URL` | gateway, wildlife, patrol, incident, reporting | `http://localhost:8082` | |
| `WILDLIFE_SERVICE_URL` | gateway, patrol, rescue, reporting | `http://localhost:8083` | |
| `RESOURCE_SERVICE_URL` | gateway, patrol, incident, programme, reporting | `http://localhost:8084` | |
| `PATROL_SERVICE_URL` | gateway, reporting | `http://localhost:8085` | |
| `INCIDENT_SERVICE_URL` | gateway, patrol, rescue, reporting | `http://localhost:8086` | |
| `RESCUE_SERVICE_URL` | gateway, reporting | `http://localhost:8087` | |
| `PROGRAMME_SERVICE_URL` | gateway, reporting | `http://localhost:8088` | |
| `REPORTING_SERVICE_URL` | gateway | `http://localhost:8089` | |
| `CORS_ALLOWED_ORIGINS` | gateway | `http://localhost:5173` | comma-separated |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | docker-compose | `wildguard` / `wildguard` | must match `DB_USERNAME` / `DB_PASSWORD` |
| `VITE_API_BASE_URL` | frontend | `http://localhost:8080` | all browser calls go to the gateway |

Standard `application.yml` datasource block (each service, with its own DB name):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:incident_db}
    username: ${DB_USERNAME:wildguard}
    password: ${DB_PASSWORD:wildguard}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
app:
  timezone: ${APP_TIMEZONE:Asia/Kolkata}
security:
  jwt:
    secret: ${JWT_SECRET}
```
Spring Boot does not read `.env` files by itself. Export the variables in your shell or IDE run configuration (README shows how).

---

## 12. Seed data (shared IDs)

Each service's `V1__init.sql` seeds a little data. **IDs must match this table** so cross-references line up across databases. Insert with explicit IDs, then reset the sequence: `SELECT setval(pg_get_serial_sequence('zones','id'), (SELECT MAX(id) FROM zones));`

**auth_db users** (dev passwords, BCrypt-hashed in the migration):

| id | username | password | roles | fullName | staffId |
|---|---|---|---|---|---|
| 1 | admin | `Admin@123` | ADMIN | Srishti Sao | null |
| 2 | officer | `Officer@123` | FOREST_OFFICER | Megha Gupta | 1 |
| 3 | ranger | `Ranger@123` | RANGER | Soni Kumari | 2 |
| 4 | vet | `Vet@123` | VET | Dr. Neha Fatima | 4 |
| 5 | analyst | `Analyst@123` | ANALYST | Sakshi Rathi | null |

**habitat_db areas and zones:**

| area id | code | name | type | state |
|---|---|---|---|---|
| 1 | KNH | Kanha Tiger Reserve | TIGER_RESERVE | Madhya Pradesh |
| 2 | KZR | Kaziranga National Park | NATIONAL_PARK | Assam |
| 3 | PRY | Periyar Tiger Reserve | TIGER_RESERVE | Kerala |

| zone id | area | code | name | type |
|---|---|---|---|---|
| 1 | 1 | KNH-MUK | Mukki Core | CORE |
| 2 | 1 | KNH-KHT | Khatia Buffer | BUFFER |
| 3 | 2 | KZR-KOH | Kohora Central | CORE |
| 4 | 2 | KZR-AGR | Agaratoli Eastern | CORE |
| 5 | 3 | PRY-THK | Thekkady Core | CORE |
| 6 | 3 | PRY-KUM | Kumily Buffer | BUFFER |

**resource_db staff:**

| id | code | name | designation | home area |
|---|---|---|---|---|
| 1 | EMP-001 | Megha Gupta | FOREST_OFFICER | 1 |
| 2 | EMP-002 | Soni Kumari | RANGE_OFFICER | 1 |
| 3 | EMP-003 | Srishti Sao | FOREST_GUARD | 1 |
| 4 | EMP-004 | Dr. Neha Fatima | VETERINARIAN | 3 |
| 5 | EMP-005 | Sakshi Rathi | FOREST_GUARD | 2 |
| 6 | EMP-006 | Suresh Nair | RANGE_OFFICER | 3 |
| 7 | EMP-007 | Imran Khan | DRIVER | 1 |
| 8 | EMP-008 | Lakshmi Pillai | RESEARCHER | 2 |

Vehicles 1–3 (jeeps/rescue van), equipment 1–5 (camera traps, GPS, radio, tranquilizer gun).

**wildlife_db:** species 1 Bengal Tiger, 2 Asian Elephant, 3 Greater One-horned Rhinoceros, 4 Indian Leopard, 5 Gharial. Animals 1–8 spread over zones 1, 3, 4, 5 (e.g. animal 1 `TGR-KNH-001` in zone 1; animal 5 `ELE-PRY-007` in zone 5, status `IN_RESCUE`).

**incident_db:** 6+ incidents in zone 1 and zone 3 with `occurred_at` **relative to the current month** so the monthly view always has data:
`date_trunc('month', LOCALTIMESTAMP) + interval '2 days 06:30'` etc. (use `LOCALTIMESTAMP`, not `now()`, for `TIMESTAMP` columns), covering every status, with assignments to staff 2, 3 and 5. Keep seeded `occurred_at` values no later than "today" by using small offsets (day 1–2 of the month) or `LOCALTIMESTAMP - interval 'n hours'`.

**rescue_db:** case 1 for animal 5 (`UNDER_TREATMENT`, with one treatment), case 2 without animal (`REHABILITATING`).

**patrol_db:** team 1 "Mukki Tigers" (area 1, leader 2, members 2, 3), route 1 in zone 1, schedules for today and recent days.

**programme_db:** programme 1 "Kanha Anti-Snare Drive" (ANTI_POACHING, area 1) with targets and progress; programme 2 "Kaziranga Wetland Restoration".

---

## 13. Repository layout

```
wildguard/                     ← repo root
├── CONTRACTS.md
├── README.md
├── docker-compose.yml         PostgreSQL only
├── docker/postgres/init/01-create-databases.sql
├── .env.example
├── .gitignore
├── .devcontainer/             GitHub Codespaces setup
├── .github/workflows/         build and test every module on each push
├── start-all.sh  status.sh  stop-all.sh
├── api-gateway/
├── auth-service/
├── habitat-service/
├── wildlife-service/
├── resource-service/
├── patrol-service/
├── incident-service/
├── rescue-service/
├── programme-service/
├── reporting-service/
└── frontend/
```

Each service folder is a standalone Maven project (`mvn spring-boot:run` inside it) with its own `README.md`.

---

## 14. Change log

| Version | Change |
|---|---|
| 1.0 | Initial contracts |
| 1.1 | Added 6.5 (programme status workflow, read-only once completed or cancelled, delete only while planned). Confirmed while building every module: RG may call `PATCH /api/allocations/{id}/status` and `PATCH /api/animals/{id}/status` (patrol and rescue forward a ranger's token); detail responses (`ScheduleDetailResponse`, `IncidentDetailResponse`, `RescueDetailResponse`, `ProgrammeDetailResponse`) are the base response with extra fields added at the top level, not nested; reporting passes each service's summary through unchanged; the frontend uses React Router 6 and Leaflet. No endpoint paths changed. |
