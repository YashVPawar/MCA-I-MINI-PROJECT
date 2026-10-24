# MedRoute

MedRoute is a Pune emergency-dispatch decision-support demo. Patient vitals, hospital capacity, traffic, and road costs are simulated; it does not diagnose or replace emergency services.

## Run locally

Requirements: Java 25+, Maven 3.9+, and Node.js 20+.

Start the API in one terminal:

```powershell
cd backend
mvn spring-boot:run
```

Start the dashboard in another terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open the Vite URL printed in the terminal. The dashboard proxies `/api` to `http://localhost:8081`. Port 8081 avoids conflicts with other local services on the usual Spring port 8080.

Optional PostGIS/pgRouting container:

```powershell
docker compose up -d
```

The database initializes the PostGIS and pgRouting extensions on first startup. The current demo API uses seeded in-memory hospitals and a synthetic road graph; it does not yet persist application data to this database or import Pune OSM roads.

## Checks

```powershell
cd backend
mvn test
cd ..\frontend
npm run build
npm run lint
```

## Current demo capabilities

- Pune Leaflet map with OpenStreetMap tiles, hospital markers, ambulance markers, and illustrative routes.
- Seeded hospital capacity and simulated traffic indicators.
- Critical-case hospital filtering and weighted resource/ETA ranking.
- In-memory A* and Dijkstra endpoints, plus a traffic multiplier and road-block control API.
- Emergency creation and the `ASSIGNED -> EN_ROUTE -> ARRIVED -> CLOSED` lifecycle.
- Ambulance, hospital, and operations views; hospital acknowledgement and an emergency history view.
- Health endpoint at `/api/health`.

## API examples

- `GET /api/health`
- `GET /api/hospitals`
- `GET /api/routes/compare?from=1&to=4`
- `POST /api/routes/traffic` with `{ "edgeId": 1, "blocked": true }`
- `POST /api/emergencies/rank-hospitals`
- `POST /api/emergencies`
- `POST /api/emergencies/{id}/advance`

## Scope still to implement

This workspace is an initial runnable vertical slice of the ten-phase plan, not the completed production system. Authentication/JWT and server-enforced roles, persistent PostgreSQL entities and CRUD, OSM/pgRouting import, graph loading from road data, live STOMP/WebSocket telemetry, background traffic/GPS/vitals simulators, route-driven ETA scoring, and the full role-specific CRUD/reporting workflows remain future phases. The role switch is demo-only, and the backend endpoints are unauthenticated; keep the API bound to a trusted local development environment until security is implemented.