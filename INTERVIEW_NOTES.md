# 🎤 EventHub: Interview Notes

Practice saying these answers out loud. Each one points to the real code, so you can open the file if they ask "show me".

## 1. "Tell me about your project" (60 seconds)

> EventHub is a full stack event ticketing platform built with **Spring Boot, React and MySQL**. Organizers create events, users book tickets, and every seat gets a **unique QR code**. At the venue, the organizer scans the QR with the phone camera, and the backend validates it and marks it as used, so a ticket can't be reused or faked.
> The two hardest parts were preventing **overbooking when many users book the last seats at the same time**, which I solved with a pessimistic database lock and proved with a 20-thread concurrency test, and preventing **double entry**, which I solved with optimistic locking on tickets. It uses **JWT authentication** with two roles, sends **confirmation emails asynchronously** with the QR codes embedded, and is deployed with **Docker on Render** and **Vercel**.

## 2. Architecture and request flow

**Q: Walk me through what happens when a user clicks "Book now".**
1. React calls `POST /api/bookings` with `{eventId, quantity}`. An Axios interceptor adds `Authorization: Bearer <jwt>` (`frontend/src/api/client.js`).
2. `JwtAuthFilter` validates the token signature and expiry, loads the user and puts them in the `SecurityContext`.
3. `SecurityConfig` checks the request is authenticated.
4. `BookingController` validates the body with `@Valid` (quantity 1–10).
5. `BookingService.book()` runs in **one transaction**: it locks the event row, checks seats, decrements `availableSeats`, and creates the booking with one `Ticket` (UUID) per seat.
6. After the commit, a `BookingConfirmedEvent` triggers the email on a background thread.
7. The response DTO goes back, and React redirects to My Tickets, which shows the QR images.

**Q: Why the Controller → Service → Repository layers?**
Separation of concerns. Controllers handle HTTP only, services hold business rules and transactions, and repositories handle data access. Each layer can be tested and changed independently.

**Q: Why DTOs instead of returning entities?**
1. Entities have lazy relations, and serializing them can throw `LazyInitializationException` or loop forever.
2. They would expose internal fields like the password hash.
3. The API contract stays stable even if the database changes.

## 3. Concurrency (most impressive topic)

**Q: Two users book the last seat at the same moment. What happens?**
`EventRepository.findByIdForUpdate()` uses `@Lock(PESSIMISTIC_WRITE)`, which runs `SELECT ... FOR UPDATE`. The second transaction **waits** until the first commits, then sees the updated `availableSeats` and gets "Sold out". `BookingConcurrencyTest` fires 20 threads at 5 seats, and exactly 5 succeed.

**Q: Why pessimistic for bookings but optimistic for check-in?**
- Booking: **high contention** on one row (everyone wants the same event), and failing would mean annoying retries. Waiting briefly on a lock is better.
- Check-in: the same ticket being scanned twice at once is **rare**. `@Version` on `Ticket` costs nothing normally, and the second update fails with `OptimisticLockingFailureException`, which returns 409.

**Q: Why not just check `availableSeats` in Java with `synchronized`?**
`synchronized` only works inside one JVM. With 2+ server instances behind a load balancer it does nothing. The database lock works across all instances.

## 4. Security

**Q: How does JWT authentication work here?**
On login, `AuthenticationManager` checks the BCrypt hash. `JwtService` then creates a token signed with HMAC-SHA256, containing the email, role and expiry. The client sends it on every request, and `JwtAuthFilter` verifies the signature. No session is stored on the server, so the API is **stateless**.

**Q: Why is CSRF disabled?**
CSRF attacks abuse cookies that the browser sends automatically. We send the JWT in a header that JavaScript has to add explicitly, so CSRF does not apply.

**Q: Where is the token stored in the frontend? Any risk?**
In `localStorage`. The risk is XSS: injected JavaScript could read it. Mitigations: React escapes output by default, we never use `dangerouslySetInnerHTML`, and the token expires in 24h. A stronger option is an `HttpOnly` cookie (plus CSRF protection).

**Q: How are passwords stored?**
BCrypt hash, which is salted and deliberately slow. Never plain text, never reversible.

**Q: The QR image endpoint is public. Isn't that insecure?**
Ticket codes are random UUIDs (122 bits), so they can't be guessed. Seeing a QR image doesn't let anyone check in: check-in requires an organizer's JWT, and it must be **their** event.

## 5. JPA / Database

**Q: What is the N+1 problem and did you face it?**
Loading 10 bookings, then lazily loading the tickets for each one, means 1 + 10 queries. `BookingRepository` uses `@EntityGraph(attributePaths = {"event", "tickets"})` to fetch everything in a single query.

**Q: Why `FetchType.LAZY` on `@ManyToOne`?**
The default for `@ManyToOne` is EAGER, which loads related rows even when they're not needed. LAZY loads them only when accessed, inside a transaction.

**Q: What does `@Transactional` do?**
Everything in the method commits together or rolls back together. If ticket creation fails, the seat count is not decremented. `readOnly = true` on read methods lets Hibernate skip dirty checking.

**Q: Why is `open-in-view` false?**
With it on, the database connection stays open during JSON rendering, so lazy loading can happen in the view layer: hidden queries and pool exhaustion under load. Turning it off forces all data loading to happen inside the service layer.

## 6. Email (async + events)

**Q: How do confirmation emails work?**
`BookingService` publishes a `BookingConfirmedEvent`. `BookingEmailService` listens with `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`:
- **AFTER_COMMIT**: no email for a booking that rolled back.
- **@Async**: the user doesn't wait 2–3 seconds for SMTP.
- If email fails, it's logged. The booking is still valid and the tickets are still in My Tickets.

## 7. Error handling and validation

**Q: How are errors handled?**
`GlobalExceptionHandler` (`@RestControllerAdvice`) maps exceptions to HTTP codes with one JSON shape: 400 validation, 401 bad login, 403 not your event, 404 not found, 409 already checked in or duplicate email. Unknown errors become 500 with a generic message, and the real error is only logged.

**Q: Why validate on both frontend and backend?**
Frontend validation is for user experience (instant feedback). Backend validation is for security, because anyone can call the API directly with Postman.

## 8. React

**Q: How do you protect routes?**
`ProtectedRoute` redirects to `/login` if not logged in, or to `/` if the role is wrong. This is only for user experience; the backend enforces the real rules.

**Q: How is the logged-in user shared across components?**
Context API (`AuthContext`), which avoids passing props through every level.

**Q: What is the `ignore` flag in `Home.jsx`?**
It prevents a race condition: if the user searches twice quickly, an older slow response could overwrite the newer one.

**Q: Any performance optimization?**
Code splitting with `React.lazy`. The QR scanner library (~340 kB) loads only when an organizer opens the Scan page, which cut the main bundle from 619 kB to 280 kB.

## 9. Deployment

**Q: How is it deployed?**
Backend: multi-stage **Docker** build (Maven build stage, then a small JRE runtime image running as a non-root user) on Render. Database: managed MySQL on Aiven over SSL. Frontend: static build on Vercel. All secrets are environment variables, never in Git.

## 10. "What was the hardest bug?"

> The frontend production build froze at "transforming…" and used 100% CPU. I bisected it: a one-line entry file built fine, and the React libraries froze. Then I narrowed it to `react-dom`. The cause was that npm had installed a much newer Rollup (Vite's bundler) than the Vite version I used. I pinned Rollup to a compatible version with npm `overrides`, and the build went from hanging forever to 3 seconds.

## 11. "What would you improve?"
- Payment gateway (Razorpay test mode) before confirming a booking
- Refresh tokens instead of one 24-hour token
- Redis cache for the event list
- Waitlist when an event is sold out
- CI pipeline (GitHub Actions) that runs the tests on every push
