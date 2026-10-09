# 🎟️ EventHub: Event Ticketing with QR Check-in

A full stack event booking platform. Organizers create events, users book tickets and get a **unique QR code per seat**, and organizers **scan the QR at the venue gate** to check attendees in. Fake, cancelled and already-used tickets are rejected.

**🔗 Live demo:** https://eventhub-six-umber.vercel.app · **API docs:** https://eventhub-api-qxo7.onrender.com/swagger-ui.html
<sub>(Free hosting: the first request may take ~1 minute while the backend wakes up.)</sub>

📄 [Deployment guide](DEPLOYMENT.md) · 🎤 [Interview notes](INTERVIEW_NOTES.md)

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.5, Spring Security, JWT, Spring Data JPA (Hibernate) |
| Database | MySQL 8 (H2 in-memory for tests) |
| QR codes | Google ZXing |
| API docs | Swagger / OpenAPI (springdoc) |
| Email | Spring Mail (Gmail SMTP), async + transactional events |
| Testing | JUnit 5, MockMvc, Mockito, concurrency test |
| Frontend | React 19, React Router 7, Axios, Vite, html5-qrcode (camera scanner) |
| Deployment | Docker, Render (backend), Vercel (frontend), Aiven (MySQL) |

## Key features

- **JWT authentication** with two roles: `USER` and `ORGANIZER`
- **Event management**: organizers create, edit and delete their own events and see live stats (tickets sold, checked in, revenue)
- **Search + pagination** for upcoming events by title or city
- **Booking with seat limits**: one QR ticket generated per seat
- **No overbooking under load**: pessimistic row lock (`SELECT ... FOR UPDATE`) on the event while booking
- **QR check-in**: validates ticket, organizer ownership, cancellation and double entry
- **Double-scan protection**: `@Version` optimistic locking on tickets
- **Cancellation**: seats are returned to the event; used tickets can't be cancelled
- **Email tickets**: QR codes emailed after booking, sent in the background only after the booking commits
- **Global exception handling**: every error returns the same JSON shape
- **Validation**: `@Valid` request DTOs with field-level error messages
- **Camera QR scanner** in the browser for gate check-in (with manual code entry fallback)
- **Role-based UI**: protected routes, organizer dashboard, printable tickets
- **Code splitting**: the scanner page and its QR library load only when opened

## Screens

| Page | Who | What it does |
|---|---|---|
| Events (`/`) | Everyone | Search + paginated event cards with seats-left badges |
| Event details | Everyone | Info, ticket quantity, total price, Book now |
| My Tickets | Logged in | Bookings with one QR per seat, cancel, print |
| Dashboard | Organizer | Totals (sold, checked in, revenue) + per-event table |
| Create / Edit event | Organizer | One form used for both |
| Scan | Organizer | Camera scanner, green ✅ / red ⛔ result |

## Architecture

```
React UI ──HTTP + JWT──▶ Controller ──▶ Service ──▶ Repository ──▶ MySQL
                         (REST, @Valid)  (business    (Spring Data
                                          rules,       JPA)
                                          @Transactional)
           JwtAuthFilter checks the token before every request
```

## Database design

```
users ──1:N──▶ events      (organizer_id)
users ──1:N──▶ bookings    (user_id)
events ──1:N──▶ bookings   (event_id)
bookings ──1:N──▶ tickets  (booking_id)  ← one ticket (QR) per seat
```

| Table | Important columns |
|---|---|
| users | id, name, email (unique), password (BCrypt), role |
| events | id, title, venue, city, start_time, price, total_seats, available_seats, organizer_id |
| bookings | id, user_id, event_id, quantity, total_amount, status (CONFIRMED / CANCELLED) |
| tickets | id, ticket_code (UUID, unique), booking_id, checked_in, checked_in_at, version |

## API endpoints

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register (role: USER or ORGANIZER) |
| POST | `/api/auth/login` | Public | Login, returns JWT |
| GET | `/api/events?q=&page=&size=` | Public | Search upcoming events |
| GET | `/api/events/{id}` | Public | Event details |
| POST | `/api/bookings` | Logged in | Book tickets `{eventId, quantity}` |
| GET | `/api/bookings/my` | Logged in | My bookings with tickets |
| POST | `/api/bookings/{id}/cancel` | Owner | Cancel booking |
| GET | `/api/tickets/{code}/qr` | Public | QR code PNG image |
| GET | `/api/organizer/events` | Organizer | My events |
| POST | `/api/organizer/events` | Organizer | Create event |
| PUT | `/api/organizer/events/{id}` | Organizer | Update event |
| DELETE | `/api/organizer/events/{id}` | Organizer | Delete event (only if no bookings) |
| GET | `/api/organizer/events/{id}/stats` | Organizer | Sold / checked-in / revenue |
| POST | `/api/organizer/checkin/{code}` | Organizer | Check in a scanned ticket |

## How to run the backend

**Requirements:** JDK 17+ and MySQL 8 running on port 3306.

1. Set your MySQL password (or edit `backend/src/main/resources/application.properties`):
   ```bash
   set DB_PASSWORD=your_mysql_password
   ```
2. Start the app (the `eventhub_db` database is created automatically):
   ```bash
   cd backend
   mvnw spring-boot:run
   ```
3. Open Swagger UI at http://localhost:8080/swagger-ui.html

Or in Eclipse/STS: **File → Import → Existing Maven Project → select `backend`**, then run `EventHubApplication`.

### Demo accounts (created on first run)

| Role | Email | Password |
|---|---|---|
| Organizer | organizer@eventhub.com | Organizer@123 |
| User | user@eventhub.com | User@123 |

## How to run the frontend

**Requirements:** Node.js 20+. Keep the backend running.

```bash
cd frontend
npm install
npm run dev
```
Open http://localhost:5173. Vite forwards `/api` calls to the backend on port 8080.

> The camera scanner needs `localhost` or HTTPS. On a phone, use the deployed HTTPS site, or type the ticket code in the manual box.

### Run tests (no MySQL needed)
```bash
cd backend
mvnw test
```
| Test | What it proves |
|---|---|
| `EventHubFlowTest` | Full API journey: register → create event → book → QR → check-in, plus 400/401/403/404/409 cases |
| `BookingConcurrencyTest` | 20 simultaneous bookings for 5 seats: exactly 5 succeed, never overbooked |
| `BookingEmailTest` | Booking sends one email with an inline QR image per ticket; user text is HTML-escaped |

To enable emails locally, see [Step 6 of the deployment guide](DEPLOYMENT.md#step-6-optional-turn-on-booking-emails-with-gmail).
