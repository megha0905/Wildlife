# WildGuard team guide

How the 12 zips map to the 5 of you, how to get each module into the shared GitHub repository, and how to run everything together.

## 1. Who owns which zip

| Zip | Folder it creates | Owner | Branch |
|---|---|---|---|
| `00-platform-root.zip` | `wildguard/` (repo root: CONTRACTS.md, README, docker-compose, this guide) | Team Lead | `main` |
| `01-api-gateway.zip` | `api-gateway/` | Team Lead | `feature/api-gateway` |
| `02-auth-service.zip` | `auth-service/` | Member 5 | `feature/auth-service` |
| `03-habitat-service.zip` | `habitat-service/` | Member 2 | `feature/habitat-service` |
| `04-wildlife-service.zip` | `wildlife-service/` | Member 2 | `feature/wildlife-service` |
| `05-resource-service.zip` | `resource-service/` | Member 3 | `feature/resource-service` |
| `06-patrol-service.zip` | `patrol-service/` | Member 3 | `feature/patrol-service` |
| `07-incident-service.zip` | `incident-service/` | Member 4 | `feature/incident-service` |
| `08-rescue-service.zip` | `rescue-service/` | Member 4 | `feature/rescue-service` |
| `09-programme-service.zip` | `programme-service/` | Member 5 | `feature/programme-service` |
| `10-reporting-service.zip` | `reporting-service/` | Member 5 | `feature/reporting-service` |
| `11-frontend.zip` | `frontend/` | Team Lead | `feature/frontend` |

Every service zip is a complete, standalone Maven project with its own README, tests and database migration. It has no shared parent POM or shared library, so it builds on its own. The zips contain no `target/`, `node_modules/`, `.env` or secrets.

**Before you change anything, read `CONTRACTS.md`.** Every endpoint, DTO field, enum, role and port is defined there. Other people's code depends on those names.

## 2. Team Lead: create the repository (do this first)

1. On GitHub, create an **empty** repository called `wildguard` (no README, no .gitignore, no licence) and add the four members as collaborators.
2. On your machine:

```bash
unzip 00-platform-root.zip          # creates ./wildguard
cd wildguard
git init -b main
git add .
git commit -m "Platform root: contracts, docker-compose, README, team guide"
git remote add origin https://github.com/<your-account>/wildguard.git
git push -u origin main
```

3. Recommended: in GitHub → Settings → Branches, protect `main` so changes only arrive through pull requests.
4. Then add your own modules the same way as everyone else (section 3), using `01-api-gateway.zip` and `11-frontend.zip`.

## 3. Every member: add your module

Do this once per zip you own (Member 2, 3, 4 and 5 each have two or three). Use one branch per module so each pull request stays small.

```bash
# first time only
git clone https://github.com/<team-lead-account>/wildguard.git
cd wildguard

# for each of your modules, e.g. incident-service
git checkout main
git pull
git checkout -b feature/incident-service
unzip ../07-incident-service.zip -d .     # creates ./incident-service next to CONTRACTS.md
git status                                # should list only incident-service/
git add incident-service
git commit -m "Add incident-service"
git push -u origin feature/incident-service
```

Then open a pull request on GitHub from `feature/incident-service` into `main`. The Team Lead reviews and merges.

**Windows (PowerShell) instead of `unzip`:**

```powershell
Expand-Archive -Path ..\07-incident-service.zip -DestinationPath .
```

Check that you get `wildguard\incident-service\pom.xml`, **not** `wildguard\incident-service\incident-service\pom.xml`. If you see a double folder, move the inner one up before committing.

**Before you push, check that nothing generated is included.** `git status` must not show `target/`, `node_modules/`, `.env` or `.idea/`. The root `.gitignore` already excludes them, as long as you unzip *inside* the repository.

### Suggested merge order

Any order works, because no service needs another to start. Merging in this order lets you test as you go:

1. root (`main`) → 2. auth-service → 3. api-gateway → 4. habitat-service → 5. resource-service → 6. wildlife-service → 7. incident-service → 8. patrol-service → 9. rescue-service → 10. programme-service → 11. reporting-service → 12. frontend

## 4. Run the whole system

One-time setup, from the repo root:

```bash
cp .env.example .env              # then set JWT_SECRET to a random string of 32+ characters
docker compose up -d              # PostgreSQL 16, creates all 8 databases
docker compose ps                 # wait for "healthy"
```

Then open one terminal per service. In **each** terminal, load the environment first (Spring Boot does not read `.env` by itself):

```bash
set -a; source .env; set +a       # macOS / Linux / Git Bash (PowerShell: see the root README)
```

Start in this order (the order only matters for what you can test, not for start-up):

| # | Terminal | Command | Check |
|---|---|---|---|
| 1 | auth | `cd auth-service && mvn spring-boot:run` | http://localhost:8081/actuator/health |
| 2 | habitat | `cd habitat-service && mvn spring-boot:run` | :8082 |
| 3 | wildlife | `cd wildlife-service && mvn spring-boot:run` | :8083 |
| 4 | resource | `cd resource-service && mvn spring-boot:run` | :8084 |
| 5 | patrol | `cd patrol-service && mvn spring-boot:run` | :8085 |
| 6 | incident | `cd incident-service && mvn spring-boot:run` | :8086 |
| 7 | rescue | `cd rescue-service && mvn spring-boot:run` | :8087 |
| 8 | programme | `cd programme-service && mvn spring-boot:run` | :8088 |
| 9 | reporting | `cd reporting-service && mvn spring-boot:run` | :8089 |
| 10 | gateway | `cd api-gateway && mvn spring-boot:run` | http://localhost:8080/actuator/health |
| 11 | frontend | `cd frontend && npm install && npm run dev` | http://localhost:5173 |

Log in at http://localhost:5173 as `officer` / `Officer@123` (all dev users are in the root README).

Ten JVMs need roughly 4–5 GB of RAM. On a smaller laptop, run only what you're working on plus the services it calls (each service README lists them under "Calls"). Missing services show up as warnings, not crashes.

### Quick end-to-end check

1. Dashboard loads and the sidebar says "All services online".
2. **Monthly zone review**: pick zone 1 and you see this month's incidents with their personnel. Click "In progress" and the list filters while the counts stay.
3. **Incident board**: open a `VERIFIED` incident, assign a staff member, then click "Start work". It moves to In progress.
4. **Patrol planner**: schedule the same team twice at overlapping times on the same day. The second schedule is refused with a 409 naming the clashing allocation.
5. **Rescue tracker**: open case 2 (leopard cub) and release it.
6. Stop incident-service and reload the dashboard. The incident section is empty, a warning names the missing service, and the rest still works.

## 5. Day-to-day Git rules

- Never commit straight to `main`. Branch from an up-to-date `main`, push, open a pull request.
- Branch names: `feature/<module>` for new work, `fix/<module>-<short-description>` for bug fixes (e.g. `fix/patrol-service-night-shift`).
- Keep each pull request inside your own module's folder. If you need to change **CONTRACTS.md**, do it in the same pull request as the code, say so in the description, and tell the people whose services call yours (CONTRACTS.md section 8).
- Run `mvn test` (or `npm run typecheck` for the frontend) before pushing.
- Never commit `.env`, real passwords or a real `JWT_SECRET`.
- If your migration has already run and you need a schema change, add `V2__<what>.sql`. Don't edit `V1__init.sql` (Flyway will refuse to start; see the root README for how to reset a database during development).

Keeping your branch up to date while a pull request is open:

```bash
git checkout main && git pull
git checkout feature/incident-service
git merge main
git push
```

## 6. When something doesn't work

| Symptom | Likely cause |
|---|---|
| `JWT_SECRET must be set and at least 32 characters long` at start-up | You didn't run `set -a; source .env; set +a` in that terminal, or the secret is too short |
| Every request through the gateway returns 401 | The gateway and the services have different `JWT_SECRET` values |
| `Connection refused` to `localhost:5432` | `docker compose up -d` not run, or Docker Desktop not started |
| `Validate failed: Migration checksum mismatch` | `V1__init.sql` was edited after it ran. Reset that database (root README) |
| `Schema-validation: missing column` | An entity and `V1__init.sql` disagree. The SQL file wins; fix the entity |
| Frontend shows "Can't reach the API gateway" | Gateway isn't running on 8080, or `VITE_API_BASE_URL` is wrong |
| CORS error in the browser console | The frontend origin isn't in `CORS_ALLOWED_ORIGINS` |
| A section shows a warning such as "habitat-service is unavailable" | That service isn't running. This is the designed behaviour, not a bug |
| Monthly zone review is empty | The seed incidents are dated in the month the database was created. Reset `incident_db` to regenerate them |
