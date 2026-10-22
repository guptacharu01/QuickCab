# 🚖 QuickCab — Cab Booking Application (Full-Stack Java)

A complete, working cab-booking platform: **React** frontend, **Spring Boot** backend,
**PostgreSQL** database, real-time WebSocket updates, and Stripe test-mode payments.

## Project structure

```
cab-booking-application/
├── frontend/                  # React 18 + Vite + Tailwind CSS + Leaflet maps
│   ├── src/
│   │   ├── components/        # Navbar, MapPicker, ProtectedRoute, StatusBadge, ui
│   │   ├── pages/             # Landing, Login, Register, Rider/Driver/Admin dashboards,
│   │   │                      # RideHistory, Support
│   │   ├── services/api.js    # Axios client (JWT attached automatically)
│   │   ├── context/           # Auth state (login/logout/role)
│   │   └── hooks/             # useRideSocket — live ride updates
│   ├── .env.example
│   └── package.json
├── backend/                   # Spring Boot 3 + Java 17 + Maven
│   └── src/main/java/com/cabbooking/
│       ├── controller/        # Auth, Ride, Driver, Payment, Ticket, Admin (+Promo)
│       ├── service/           # Auth, Ride (booking state machine), Fare, Payment (Stripe),
│       │                      # Rating, Ticket, Admin, RideEventPublisher
│       ├── entity/            # User, Driver, Ride, Payment, Rating, PromoCode, SupportTicket
│       ├── repository/        # Spring Data JPA repositories
│       ├── security/          # JWT util + auth filter
│       ├── config/            # Security, WebSocket, admin bootstrap
│       ├── dto/               # Request/response shapes (no entities leak to clients)
│       └── exception/         # Centralized JSON error handling
├── database/
│   ├── schema.sql             # Canonical PostgreSQL schema
│   └── seed.sql               # Safe sample data (promo codes only)
├── docker-compose.yml         # One-command Postgres + backend
├── .env.example               # All environment variables (placeholders only)
└── README.md
```

## 1. Required software

| Software | Version | Check with |
|---|---|---|
| Java (JDK) | 17+ | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Node.js | 18+ | `node -version` |
| PostgreSQL | 14+ | `psql --version` |
| Docker (optional) | any recent | `docker --version` |

## 2. Install the tools

**Windows (PowerShell)**
- Java: download the Temurin 17 installer from [adoptium.net](https://adoptium.net), run it.
- Maven: download the binary zip from [maven.apache.org](https://maven.apache.org/download.cgi), extract to `C:\maven`, add `C:\maven\bin` to PATH.
- Node.js: installer from [nodejs.org](https://nodejs.org).
- PostgreSQL: installer from [postgresql.org/download/windows](https://www.postgresql.org/download/windows/) (remember the `postgres` password you set).

**macOS**
```bash
brew install openjdk@17 maven node postgresql@16
```

**Linux (Debian/Ubuntu)**
```bash
sudo apt update && sudo apt install -y openjdk-17-jdk maven nodejs npm postgresql
```

## 3. Create the database

**PowerShell**
```powershell
psql -U postgres -c "CREATE USER cabuser WITH PASSWORD 'cabpass';"
psql -U postgres -c "CREATE DATABASE cabbooking OWNER cabuser;"
psql -U cabuser -d cabbooking -f database/schema.sql
psql -U cabuser -d cabbooking -f database/seed.sql
```

**macOS / Linux**
```bash
sudo -u postgres psql -c "CREATE USER cabuser WITH PASSWORD 'cabpass';"
sudo -u postgres psql -c "CREATE DATABASE cabbooking OWNER cabuser;"
psql -U cabuser -d cabbooking -f database/schema.sql
psql -U cabuser -d cabbooking -f database/seed.sql
```

> The backend can also create the tables itself (`DDL_AUTO=update`, the default),
> so the `schema.sql` step is optional for local development — but the file is the
> canonical schema and is what Docker uses.

## 4. Configure environment variables

```powershell
# PowerShell — copy the example and edit it
Copy-Item .env.example .env
# macOS/Linux:
# cp .env.example .env
```

Then either export the variables in your shell or create the `.env` file —
the backend reads `application.properties`, which picks up real environment
variables first. The simplest local setup needs **nothing**: defaults point at
`localhost:5432/cabbooking` with user `cabuser` / `cabpass`.

**Mandatory for production:** set a long random `JWT_SECRET` (min 32 chars).
**Optional:** `STRIPE_*` keys (payments), `ADMIN_EMAIL`/`ADMIN_PASSWORD` (first admin).

## 5. Start the backend

**VS Code:** open the `backend/` folder, use the *Spring Boot Dashboard* extension
(or the `Spring Boot` extension pack) and run `CabBookingApplication`.

**Terminal (PowerShell / bash):**
```powershell
cd backend
mvn spring-boot:run
```
The API is at `http://localhost:8080`. First start also seeds promo codes
`WELCOME50` (50% off, max ₹100) and `FLAT30` (₹30 off).

## 6. Start the frontend

```powershell
cd frontend
npm install
npm run dev
```
Open `http://localhost:5173`. To point at a different backend, copy
`frontend/.env.example` to `frontend/.env` and set `VITE_API_URL`.

## 7. Create the first admin account

There is **no public admin registration** (by design). Set these before starting
the backend once:

```powershell
# PowerShell
$env:ADMIN_EMAIL="admin@example.com"; $env:ADMIN_PASSWORD="choose-a-strong-password"
# macOS/Linux
# export ADMIN_EMAIL="admin@example.com" ADMIN_PASSWORD="choose-a-strong-password"
```

On startup the backend creates the admin if it doesn't exist yet.
Log in with those credentials → you land on the **Admin console**.

## 8. Test the rider → driver workflow

1. Register a **rider** (`/register` → Rider) and a **driver** (→ Driver, with vehicle details).
2. Log in as the **driver** → **Go online** → optionally **Share location** (browser will ask permission).
3. In another window (or incognito), log in as the **rider** → click **📍 Pickup**, click the map → **🏁 Drop**, click the map → **Book ride**.
4. Back as driver: the request appears live → **Accept** → **Mark arrived** → **Start trip** → **Complete trip**.
5. Back as rider: the status updates live on the page → **Pay** (see §9) → **rate the driver** ⭐.

## 9. Stripe test payments (optional)

1. Create a free account at [stripe.com](https://stripe.com) and switch to **Test mode**.
2. Copy the **Secret key** (`sk_test_…`), **Publishable key** (`pk_test_…`), and create a
   webhook endpoint pointing at `http://localhost:8080/api/payments/webhook` for the
   `payment_intent.succeeded` event to get the **Webhook secret** (`whsec_…`).
3. Set `STRIPE_SECRET_KEY`, `STRIPE_PUBLISHABLE_KEY`, `STRIPE_WEBHOOK_SECRET` and restart the backend.
4. For local webhooks, use the Stripe CLI: `stripe listen --forward-to localhost:8080/api/payments/webhook`.
5. Pay with test card `4242 4242 4242 4242`, any future expiry, any CVC.

Without Stripe keys the app still runs end-to-end; the Pay button honestly reports
that online payment isn't configured and **never** marks an unpaid ride as paid.

## 10. Maps & location

- Maps use **Leaflet + OpenStreetMap** — no API key needed.
- Routes use the public **OSRM demo server** (free for light use; if it is slow or
  rate-limited the app falls back to a straight line — the fare math never depends on it).
- Driver live location uses the **browser geolocation API**; if permission is denied,
  sharing stays off and the app keeps working.
- Fare = `max(₹80, ₹60 + ₹15 × km)` (configurable via `FARE_*` variables).

## 11. Run the tests

```powershell
cd backend
mvn test
```
Covers fare math (`FareServiceTest`) and API security rules (`SecurityTest`: anonymous
access blocked, webhook stays public, login is public).

## 12. Docker (alternative setup)

```powershell
# optional: create .env with JWT_SECRET / ADMIN_* / STRIPE_* first
docker compose up --build
```
This starts PostgreSQL (with `schema.sql` + `seed.sql` applied) and the backend on
`:8080`. Run the frontend separately with `npm run dev`.

## 13. Common errors

| Error | Fix |
|---|---|
| `Connection refused` / backend can't reach Postgres | Start Postgres; check `DB_URL`, `DB_USER`, `DB_PASSWORD`. |
| `FATAL: password authentication failed` | The DB password in env doesn't match the one you created the user with. |
| `JWT secret` / token errors | Set a `JWT_SECRET` of at least 32 characters. |
| Frontend `Network Error` / CORS | Backend must run on `:8080`; `CORS_ORIGINS` must include the frontend URL (`http://localhost:5173`). |
| Map tiles don't load | Check internet access; tiles come from openstreetmap.org. |
| `4242…` card declined | You're not in Stripe test mode, or keys are wrong — check backend logs. |
| Port 8080/5173 already in use | Stop the other process or change `server.port` / Vite `server.port`. |
| `mvn: command not found` | Install Maven and add it to PATH (see §2). |

## API overview

| Method & path | Role | Purpose |
|---|---|---|
| `POST /api/auth/register` | public | Register rider/driver |
| `POST /api/auth/login` | public | Login → JWT |
| `GET /api/auth/me` | any | Current user |
| `POST /api/rides/estimate` | rider | Fare estimate |
| `POST /api/rides` | rider | Book a ride |
| `GET /api/rides/mine`, `GET /api/rides/{id}` | rider | Ride history / details |
| `POST /api/rides/{id}/cancel`, `/sos`, `/{id}/rate` | rider | Cancel, SOS, rate |
| `GET /api/promos/validate?code=` | rider | Validate promo |
| `GET/PUT /api/driver/profile`, `PUT /api/driver/availability` | driver | Profile, online toggle |
| `GET /api/driver/requests` | driver | Pending requests |
| `POST /api/driver/rides/{id}/accept`, `/reject`, `/{id}/status` | driver | Trip control |
| `POST /api/driver/location` | driver | Live location |
| `GET /api/driver/ratings`, `/rides/history` | driver | Reviews, history |
| `POST /api/payments/rides/{id}/intent`, `/confirm` | rider | Stripe payment |
| `POST /api/payments/webhook` | public (signed) | Stripe webhook |
| `GET/POST /api/tickets` | rider/driver | Support tickets |
| `GET /api/admin/stats|users|rides|payments|tickets`, `PUT …` | admin | Admin console |
| `WS /ws/rides?rideId=&token=` | per-ride auth | Live status + location push |

## Security notes

- Passwords are BCrypt-hashed; JWTs are HMAC-signed with expiry; endpoints enforce
  roles server-side (the UI hiding buttons is not the protection).
- A ride can only be accepted once (pessimistic DB lock); status transitions are
  validated; drivers can only touch their own rides; amounts are computed server-side.
- No secrets are committed — everything sensitive comes from environment variables.
