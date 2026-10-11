# frontend

The WildGuard web app, written in plain **HTML, CSS and JavaScript** (ES modules). There is no framework and no build step: the browser loads the files in `src/` directly. The app talks only to the API gateway.

| | |
|---|---|
| Owner | Team Lead |
| Port | 5173 (`server.js`) |
| Talks to | api-gateway; `server.js` forwards every `/api/...` request to `API_TARGET` (default `http://localhost:8080`) |
| Libraries | None installed. Leaflet 1.9.4 (maps with OpenStreetMap tiles) is loaded from a CDN in `index.html`. All styling is in `src/styles.css` |

## Run it

Requirements: Node.js 20+ and npm.

```bash
cd frontend
npm install                 # nothing to download; kept so the usual commands still work
npm run dev                 # http://localhost:5173
```

Log in with one of the dev users from the root README (for example `officer` / `Officer@123`). The login page also has demo buttons that fill the form for you.

The gateway (and at least auth-service) must be running to log in. Screens whose service is down show an error banner or a warning; the rest of the app keeps working. The dot at the bottom of the sidebar shows which services are offline.

| Command | What it does |
|---|---|
| `npm run dev` (or `npm start`) | Starts `server.js`: serves the app and forwards `/api` to the gateway |
| `npm run build` | Checks every `.js` file for syntax errors and broken imports (`scripts/check.js`). Used by CI |

## Settings

| Variable | Default | Notes |
|---|---|---|
| `API_TARGET` | `http://localhost:8080` | Where `/api/...` requests are forwarded (the gateway) |
| `PORT` | `5173` | Port of the dev server |

Example: `API_TARGET=http://localhost:8080 PORT=5173 npm run dev`.

Because the browser only ever talks to port 5173 and the server forwards `/api` itself, there are no CORS problems and the same setup works on a laptop and in GitHub Codespaces.

## Screens

Pages use hash routes (`#/incidents`), so a refresh or a bookmark always works with any static server.

| Route | Screen | Main endpoints |
|---|---|---|
| (logged out) | Login | `POST /api/auth/login` |
| `#/` | Dashboard: headline numbers and charts from every service | `GET /api/reports/dashboard` |
| `#/animals` | Animal registry: search, register, health records, species | `/api/animals`, `/api/species` |
| `#/zones` | Zones & map: areas, zones, habitats, water sources on a map | `/api/areas`, `/api/zones`, `/api/habitats`, `/api/water-resources` |
| `#/patrols` | Patrol planner: schedule by day, start/complete, field logs, escalate to incident; teams and routes | `/api/patrols/**` |
| `#/incidents` | Incident board: one column per status, workflow buttons, assign staff | `/api/incidents` |
| `#/incidents/monthly` | **Monthly zone review** (the mandatory example) | `GET /api/incidents/zone/{zoneId}/current-month?status=` |
| `#/rescues` | Rescue tracker: intake, treatments, rehab assessments, release/transfer | `/api/rescues` |
| `#/resources` | Staff & resources: staff, vehicles, equipment, allocations and conflicts | `/api/staff`, `/api/vehicles`, `/api/equipment`, `/api/allocations` |
| `#/programmes` | Conservation programmes: activities, targets, progress | `/api/programmes` |
| `#/reports` | Reports: incidents by month, combined zone profile | `/api/reports/**` |
| `#/users` | User management (ADMIN only) | `/api/users` |

Buttons are hidden when the logged-in role can't use them, but the backend is what actually enforces roles: a hidden button is a convenience, not security.

### The mandatory screen: Monthly zone review

`src/pages/ZoneMonthlyPage.js`. Pick a zone and it calls `GET /api/incidents/zone/{zoneId}/current-month`. The page shows:

- the status summary as a row of buttons with counts (All, Reported, Verified, In progress, Resolved, Closed). Clicking one re-calls the endpoint with `?status=` to filter the list, while the counts stay for the whole month;
- a map of where each incident happened;
- a table with each incident's status, severity, location and assigned personnel. Clicking a row opens the full incident with its workflow actions.

## Code layout

```
frontend/
├── index.html            the one HTML page: loads the font, Leaflet, styles.css and src/main.js
├── server.js             dev server (Node built-in modules only) + /api forwarding
├── scripts/check.js      "npm run build": syntax and import check
├── public/favicon.svg
└── src/
    ├── main.js           entry: restores the login, then starts the app
    ├── App.js            hash routes; logged-out users only see the login page
    ├── styles.css        all styling, responsive down to phone width
    ├── api/
    │   ├── client.js     fetch wrapper: adds the token, turns error bodies into ApiError, logs out on 401
    │   ├── endpoints.js  one function per backend endpoint, grouped by service
    │   └── lookups.js    cached zones/areas/species/staff for dropdowns
    ├── auth/auth.js      login, logout, current user, hasRole()
    ├── components/       dom.js (h() and mount()), layout (sidebar), table, modal/drawer, form, map, bars, toasts, ui helpers
    └── pages/            one file per screen
```

How a screen is built:

- `h('button', { class: 'btn', onClick: save }, 'Save')` creates an element (`components/dom.js`); `mount(el, ...children)` replaces an element's contents.
- `region(loader, render)` (`components/ui.js`) shows "Loading…", calls the API, then renders the result or an error banner. `reload()` fetches again after a change.
- Forms are declared as a list of fields and opened with `openFormModal(...)`; record details open in a side panel with `openDrawer()`.

The token is kept in `localStorage` (`wildguard.token`) so a page refresh keeps you logged in. Any 401 from the API logs you out.

**Adding a field:** check the DTO in CONTRACTS.md, add it to the form's `fields` list and to the table `columns` or drawer `fact(...)` lines of the page.

## Tests

The prompt asked for backend unit tests; there are no automated frontend tests. `npm run build` catches syntax errors and broken imports; check the screens by hand by clicking through each one with the backend running.
