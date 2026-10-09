package com.eventhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end API test: register -> create event -> book -> QR -> check-in,
 * plus the main error cases (401, 403, 400 validation, 409 double check-in).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventHubFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullTicketingFlow() throws Exception {
        String organizerToken = register("Priya Organizer", "priya@test.com", "ORGANIZER");
        String userToken = register("Rahul User", "rahul@test.com", "USER");

        Map<String, Object> event = Map.of(
                "title", "Java Conference",
                "description", "All about Spring Boot",
                "venue", "Tech Park",
                "city", "Chennai",
                "startTime", LocalDateTime.now().plusDays(7).withNano(0).toString(),
                "price", 500,
                "totalSeats", 3);

        // A normal user cannot create events
        mockMvc.perform(post("/api/organizer/events").header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json(event)))
                .andExpect(status().isForbidden());

        // Organizer creates the event
        JsonNode created = readJson(mockMvc.perform(post("/api/organizer/events")
                        .header("Authorization", bearer(organizerToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json(event)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.availableSeats").value(3))
                .andReturn().getResponse().getContentAsString());
        long eventId = created.get("id").asLong();

        // Anyone can browse and search events
        mockMvc.perform(get("/api/events").param("q", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Java Conference"));

        // Booking without login is rejected
        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventId", eventId, "quantity", 1))))
                .andExpect(status().isUnauthorized());

        // User books 2 tickets
        JsonNode booking = readJson(mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventId", eventId, "quantity", 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(1000))
                .andExpect(jsonPath("$.tickets.length()").value(2))
                .andReturn().getResponse().getContentAsString());
        String ticketCode = booking.get("tickets").get(0).get("ticketCode").asText();

        // Only 1 seat left, so booking 2 more fails
        mockMvc.perform(post("/api/bookings").header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventId", eventId, "quantity", 2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only 1 seat(s) left"));

        // QR image is generated
        mockMvc.perform(get("/api/tickets/" + ticketCode + "/qr"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));

        // Organizer scans the ticket at the gate
        mockMvc.perform(post("/api/organizer/checkin/" + ticketCode).header("Authorization", bearer(organizerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendeeName").value("Rahul User"));

        // Same ticket scanned again is rejected
        mockMvc.perform(post("/api/organizer/checkin/" + ticketCode).header("Authorization", bearer(organizerToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already used")));

        // A fake ticket code is rejected
        mockMvc.perform(post("/api/organizer/checkin/not-a-real-ticket").header("Authorization", bearer(organizerToken)))
                .andExpect(status().isNotFound());

        // Stats reflect the sale and the check-in
        mockMvc.perform(get("/api/organizer/events/" + eventId + "/stats").header("Authorization", bearer(organizerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketsSold").value(2))
                .andExpect(jsonPath("$.checkedIn").value(1))
                .andExpect(jsonPath("$.revenue").value(1000));

        // Booking with a used ticket can't be cancelled
        mockMvc.perform(post("/api/bookings/" + booking.get("id").asLong() + "/cancel")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validationAndLoginErrors() throws Exception {
        // Invalid register body returns every field error
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "", "email", "not-an-email", "password", "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").value("Enter a valid email"))
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        register("Asha", "asha@test.com", "USER");

        // Duplicate email
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Asha", "email", "asha@test.com", "password", "secret123"))))
                .andExpect(status().isConflict());

        // Wrong password
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "asha@test.com", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        // Correct login returns a token
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "asha@test.com", "password", "secret123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        // Tampered token is treated as not logged in
        mockMvc.perform(get("/api/bookings/my").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    private String register(String name, String email, String role) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", name, "email", email, "password", "secret123", "role", role))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return readJson(body).get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode readJson(String body) throws Exception {
        return objectMapper.readTree(body);
    }
}
