package com.eventhub.config;

import com.eventhub.entity.Event;
import com.eventhub.entity.Role;
import com.eventhub.entity.User;
import com.eventhub.repository.EventRepository;
import com.eventhub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Inserts demo accounts and events the first time the app starts with an empty database.
 * Demo logins (local development only):
 *   organizer@eventhub.com / Organizer@123
 *   user@eventhub.com      / User@123
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, EventRepository eventRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User organizer = createUser("Demo Organizer", "organizer@eventhub.com", "Organizer@123", Role.ORGANIZER);
        createUser("Demo User", "user@eventhub.com", "User@123", Role.USER);

        createEvent(organizer, "Sunburn Arena ft. Local DJs", "An evening of electronic music with the city's best DJs.",
                "Jawaharlal Nehru Stadium", "Chennai", 10, "18:00", "1499", 500);
        createEvent(organizer, "Spring Boot Developer Meetup", "Talks on microservices, Spring Security and real-world deployments.",
                "Tidel Park Auditorium", "Chennai", 14, "10:00", "0", 120);
        createEvent(organizer, "Stand-up Comedy Night", "Two hours of non-stop laughs with three headline comedians.",
                "Phoenix Marketcity", "Bengaluru", 21, "19:30", "599", 200);
        createEvent(organizer, "Startup Pitch Day 2026", "Watch 12 early-stage startups pitch to top investors.",
                "HICC Novotel", "Hyderabad", 30, "09:30", "299", 300);

        log.info("Demo data created: 2 users and 4 events");
    }

    private User createUser(String name, String email, String rawPassword, Role role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        return userRepository.save(user);
    }

    private void createEvent(User organizer, String title, String description, String venue, String city,
                             int daysFromNow, String time, String price, int seats) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setVenue(venue);
        event.setCity(city);
        event.setStartTime(LocalDateTime.of(LocalDate.now().plusDays(daysFromNow), LocalTime.parse(time)));
        event.setPrice(new BigDecimal(price));
        event.setTotalSeats(seats);
        event.setAvailableSeats(seats);
        event.setOrganizer(organizer);
        eventRepository.save(event);
    }
}
