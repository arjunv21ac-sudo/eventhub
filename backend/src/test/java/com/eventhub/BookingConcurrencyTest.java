package com.eventhub;

import com.eventhub.dto.BookingDtos.BookingRequest;
import com.eventhub.entity.Event;
import com.eventhub.entity.Role;
import com.eventhub.entity.User;
import com.eventhub.exception.BadRequestException;
import com.eventhub.repository.EventRepository;
import com.eventhub.repository.UserRepository;
import com.eventhub.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 20 users try to grab the last 5 seats at exactly the same time.
 * Thanks to the pessimistic lock, exactly 5 succeed and the event is never overbooked.
 */
@SpringBootTest
@ActiveProfiles("test")
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void neverOverbooksUnderConcurrentRequests() throws Exception {
        User organizer = saveUser("Org", "conc-org@test.com", Role.ORGANIZER);
        saveUser("Buyer", "conc-user@test.com", Role.USER);

        Event event = new Event();
        event.setTitle("Limited Seats Show");
        event.setVenue("Hall A");
        event.setCity("Chennai");
        event.setStartTime(LocalDateTime.now().plusDays(3));
        event.setPrice(new BigDecimal("100"));
        event.setTotalSeats(5);
        event.setAvailableSeats(5);
        event.setOrganizer(organizer);
        Long eventId = eventRepository.save(event).getId();

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startSignal = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startSignal.await(); // all threads fire together
                    bookingService.book(new BookingRequest(eventId, 1), "conc-user@test.com");
                    succeeded.incrementAndGet();
                } catch (BadRequestException e) {
                    soldOut.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
        }

        startSignal.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(succeeded.get()).isEqualTo(5);
        assertThat(soldOut.get()).isEqualTo(15);
        assertThat(eventRepository.findById(eventId).orElseThrow().getAvailableSeats()).isZero();
    }

    private User saveUser(String name, String email, Role role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("not-used");
        user.setRole(role);
        return userRepository.save(user);
    }
}
