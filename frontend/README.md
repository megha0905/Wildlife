# frontend

The WildGuard web app: React 18 + TypeScript, built with Vite. It talks only to the API gateway.

| | |
|---|---|
| Owner | Team Lead |
| Port | 5173 (Vite dev server) |
| Talks to | api-gateway at `VITE_API_BASE_URL` (default `http://localhost:8080`) |
| Libraries | react, react-dom, react-router-dom 6, leaflet (maps with OpenStreetMap tiles). No UI framework; all styling is in `src/styles.css` |

## Run it

Requirements: Node.js 20+ and npm.

```bash
cd frontend
cp .env.example .env        # optional, only if the gateway isn't on localhost:8080
npm install                 # first time only; creates package-lock.json, commit that file
npm run dev                 # http://localhost:5173
```

Log in with one of the dev users from the root README (for example `officer` / `Officer@123`). The login page also has a "Development logins" list that fills the form for you.

The gateway (and at least auth-service) must be running to log in. Screens whose service is down show an error banner or a warning; the rest of the app keeps working. The dot at the bottom of the sidebar shows which services are offline.

| Command | What it does |
|---|---|
| `npm run dev` | Dev server with hot reload |
| `npm run typecheck` | TypeScript check only |
| `npm run build` | Type check, then production build into `dist/` |
| `npm run preview` | Serve the production build locally |

## Environment

| Variable | Default | Notes |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | Gateway URL. Read at build time by Vite. An empty value means "same origin as the page" (useful behind a reverse proxy). |

The gateway must allow the frontend's origin (`CORS_ALLOWED_ORIGINS`, default `http://localhost:5173`).

## Screens

| Route | Screen | Main endpoints |
|---|---|---|
| `/login` | Login | `POST /api/auth/login` |
| `/` | Dashboard: headline numbers and charts from every service | `GET /api/reports/dashboard` |
| `/animals` | Animal registry: search, register, sightings, health records, species | `/api/animals`, `/api/species` |
| `/zones` | Zones & map: areas, zones, habitats, water sources on a map | `/api/areas`, `/api/zones`, `/api/habitats`, `/api/water-resources` |
| `/patrols` | Patrol planner: schedule by day, start/complete, field logs, escalate to incident; teams and routes | `/api/patrols/**` |
| `/incidents` | Incident board: one column per status, workflow buttons, assign staff | `/api/incidents` |
| `/incidents/monthly` | **Monthly zone review** (the mandatory example) | `GET /api/incidents/zone/{zoneId}/current-month?status=` |
| `/rescues` | Rescue tracker: intake, treatments, rehab assessments, release/transfer | `/api/rescues` |
| `/resources` | Staff & resources: staff, vehicles, equipment, allocations and conflicts | `/api/staff`, `/api/vehicles`, `/api/equipment`, `/api/allocations` |
| `/programmes` | Conservation programmes: activities, targets, progress | `/api/programmes` |
| `/reports` | Reports: incidents by month, combined zone profile | `/api/reports/**` |
| `/users` | User management (ADMIN only) | `/api/users` |

Buttons are hidden when the logged-in role can't use them, but the backend is what actually enforces roles: a hidden button is a convenience, not security.

### The mandatory screen: Monthly zone review

`src/pages/ZoneMonthlyPage.tsx`. Pick a zone and it calls `GET /api/incidents/zone/{zoneId}/current-month`. The page shows:

- the status summary as a row of buttons with counts (All, Reported, Verified, In progress, Resolved, Closed). Clicking one re-calls the endpoint with `?status=` to filter the list, while the counts stay for the whole month;
- a map of where each incident happened;
- a table with each incident's status, severity, location and assigned personnel. Clicking a row opens the full incident with its workflow actions.

## Code layout

```
src/
├── main.tsx              entry: router, auth and toast providers
├── App.tsx               routes (logged-out users only see /login)
├── styles.css            all styling, responsive down to phone width
├── api/
│   ├── client.ts         fetch wrapper: adds the token, turns error bodies into ApiError, logs out on 401
│   ├── endpoints.ts      one function per backend endpoint, grouped by service
│   ├── types.ts          TypeScript copies of the DTOs in CONTRACTS.md
│   └── lookups.ts        cached zones/areas/species/staff for dropdowns
├── auth/AuthContext.tsx  login, logout, current user, hasRole()
├── components/           Layout (sidebar), DataTable, Modal/Drawer, FormModal, MapView, Bars, Toasts, small UI bits
└── pages/                one file per screen
```

The token is kept in `localStorage` (`wildguard.token`) so a page refresh keeps you logged in. Any 401 from the API logs you out.

**Adding a field:** update the DTO in `api/types.ts` (it must match CONTRACTS.md), then the page. If the backend DTO changes, `npm run typecheck` will point at every place that needs updating.

## Tests

The prompt asked for backend unit tests; there are no automated frontend tests. Check it by hand with `npm run typecheck` and by clicking through each screen with the backend running.
