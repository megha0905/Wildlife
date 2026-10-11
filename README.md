# WildGuard

Smart Wildlife Conservation & Protected Area Management: an enterprise web application that helps wildlife authorities manage animal records, protected areas, patrols, incidents, rescues, field resources and conservation programmes.

Built as standalone Spring Boot microservices behind a Spring Cloud Gateway, with a plain HTML, CSS and JavaScript frontend and one PostgreSQL database per service.

**Read [`CONTRACTS.md`](CONTRACTS.md) before writing any code.** It defines every port, endpoint, DTO, enum, role and environment variable. If you need to change a contract, change that file first in the same pull request.

## Services

| Service | Port | Database | Owner |
|---|---|---|---|
| api-gateway | 8080 | none | Team Lead |
| auth-service | 8081 | auth_db | Member 5 |
| habitat-service | 8082 | habitat_db | Member 2 |
| wildlife-service | 8083 | wildlife_db | Member 2 |
| resource-service | 8084 | resource_db | Member 3 |
| patrol-service | 8085 | patrol_db | Member 3 |
| incident-service | 8086 | incident_db | Member 4 |
| rescue-service | 8087 | rescue_db | Member 4 |
| programme-service | 8088 | programme_db | Member 5 |
| reporting-service | 8089 | none | Member 5 |
| frontend | 5173 | none | Team Lead |

```
 Browser ──► frontend (HTML/CSS/JS, 5173)
                │  all API calls
                ▼
          api-gateway (8080)  ── checks JWT, routes by path
                │
   ┌──────┬─────┼──────┬────────┬─────────┬────────┬──────────┬──────────┐
  auth habitat wildlife resource patrol  incident  rescue  programme  reporting
  8081  8082    8083     8084    8085     8086     8087     8088       8089
   │     │       │        │       │        │        │        │       (calls the
   └─────┴───────┴────────┴───────┴────────┴────────┴────────┘        others'
                 PostgreSQL 16 (docker), one DB per service           /summary)
```

Services call each other directly with OpenFeign (URLs from environment variables), forwarding the user's JWT. If another service is down, the caller still answers and adds a `warnings` entry.

## Prerequisites

- JDK 17+ (`java -version`)
- Maven 3.9+ (`mvn -v`)
- Docker Desktop (for PostgreSQL)
- Node.js 20+ and npm (frontend only)

## First-time setup

```bash
# 1. Environment
cp .env.example .env
#    edit .env and set JWT_SECRET to a random string of at least 32 characters

# 2. Database (creates all 8 databases on first start)
docker compose up -d
docker compose ps          # wait until postgres shows "healthy"
```

### Making the variables visible to Spring Boot

Spring Boot does not read `.env`. Before starting a service, export the variables in that terminal:

```bash
# macOS / Linux / Git Bash
set -a; source .env; set +a
```

```powershell
# Windows PowerShell (run from the repo root)
Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
  $name, $value = $_ -split '=', 2; [Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), 'Process')
}
```

In IntelliJ IDEA, add the variables to each run configuration instead (Run → Edit Configurations → Environment variables). The EnvFile plugin can load `.env` directly.

Every value except `JWT_SECRET` has a default that works locally, so in practice you only must set `JWT_SECRET`.

## Running in GitHub Codespaces (nothing to install)

1. On GitHub: **Code → Codespaces → ⋯ → New with options**, pick the **4-core** machine, **Create codespace**.
2. In the codespace, open `.env` (created automatically) and set `JWT_SECRET` to any random text of 32+ characters.
3. Terminal: `bash start-all.sh` (PostgreSQL + all 10 backend apps in the background; first start takes 5–10 minutes).
4. `bash status.sh` until every service is UP. If one stays DOWN, read `logs/<service>.log`.
5. New terminal: `cd frontend && npm install && npm run dev`, then open port 5173 from the **Ports** tab.
6. When finished: `bash stop-all.sh`, then stop the codespace so it doesn't use up your free hours.

In Codespaces the frontend calls `/api` on its own address and `frontend/server.js` forwards it to the gateway, so only port 5173 has to be opened.

## Running on your own computer

Start PostgreSQL, then the services, then the frontend. Order of services doesn't matter for start-up (none calls another while starting), but features that call other services need them running.

```bash
cd auth-service && mvn spring-boot:run          # repeat for each service, one terminal each
cd frontend && npm install && npm run dev        # http://localhost:5173
```

Service-specific instructions are in each service's own `README.md`.

### Dev logins

| Username | Password | Role |
|---|---|---|
| admin | Admin@123 | ADMIN |
| officer | Officer@123 | FOREST_OFFICER |
| ranger | Ranger@123 | RANGER |
| vet | Vet@123 | VET |
| analyst | Analyst@123 | ANALYST |

These are development credentials seeded by auth-service. Change or remove them before any real deployment.

## Useful URLs (once running)

| What | URL |
|---|---|
| App | http://localhost:5173 |
| Gateway | http://localhost:8080 |
| Service health through gateway | http://localhost:8080/api/gateway/health |
| API docs for a service | `http://localhost:<port>/swagger-ui.html` |

To call an API by hand, log in first and pass the token:

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"officer","password":"Officer@123"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')

curl -H "Authorization: Bearer $TOKEN" "localhost:8080/api/incidents/zone/1/current-month"
```

## Database tools

```bash
docker exec -it wildguard-postgres psql -U wildguard -d incident_db    # open a SQL shell
docker compose down -v && docker compose up -d                         # wipe everything and start fresh
```

If you change a service's `V1__init.sql` after it has already run, Flyway will refuse to start (checksum mismatch). During development, either add a new `V2__...sql` migration or wipe that service's database:

```bash
docker exec -it wildguard-postgres psql -U wildguard -d postgres \
  -c "DROP DATABASE incident_db;" -c "CREATE DATABASE incident_db;"
```

## Repository layout

```
wildguard/
├── CONTRACTS.md          single source of truth for all APIs
├── README.md
├── docker-compose.yml    PostgreSQL only
├── docker/postgres/init/ creates the 8 databases
├── .env.example
├── .devcontainer/        GitHub Codespaces setup (+ start-all.sh, status.sh, stop-all.sh)
├── .github/workflows/    builds and tests every module on each push
├── api-gateway/  auth-service/  habitat-service/  wildlife-service/  resource-service/
├── patrol-service/  incident-service/  rescue-service/  programme-service/  reporting-service/
└── frontend/
```

Each service folder is a standalone Maven project with no shared parent POM or shared library, so it builds on its own.
