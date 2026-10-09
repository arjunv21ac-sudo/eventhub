package com.eventhub;

import com.eventhub.dto.BookingDtos.BookingRequest;
import com.eventhub.entity.Event;
import com.eventhub.entity.Role;
import com.eventhub.entity.User;
import com.eventhub.repository.EventRepository;
import com.eventhub.repository.UserRepository;
import com.eventhub.service.BookingService;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Booking with email enabled sends one email to the buyer, with one inline QR image per ticket.
 * The real SMTP sender is replaced by a Mockito mock, so no email actually leaves the machine.
 */
@SpringBootTest(properties = {
        "app.mail.enabled=true",
        "app.mail.from=tickets@eventhub.test",
        // separate in-memory database, because this test starts its own Spring context
        "spring.datasource.url=jdbc:h2:mem:emailtest;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class BookingEmailTest {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void sendsConfirmationEmailWithQrCodesAfterBooking() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        User organizer = saveUser("Org", "mail-org@test.com", Role.ORGANIZER);
        saveUser("Kavya", "kavya@test.com", Role.USER);

        Event event = new Event();
        event.setTitle("Music <Night>");
        event.setVenue("Open Air Theatre");
        event.setCity("Chennai");
        event.setStartTime(LocalDateTime.now().plusDays(5));
        event.setPrice(new BigDecimal("250"));
        event.setTotalSeats(50);
        event.setAvailableSeats(50);
        event.setOrganizer(organizer);
        Long eventId = eventRepository.save(event).getId();

        bookingService.book(new BookingRequest(eventId, 3), "kavya@test.com");

        // Sent asynchronously, so wait up to 5 seconds for it
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, timeout(5000)).send(captor.capture());

        MimeMessage sent = captor.getValue();
        assertThat(sent.getAllRecipients()[0].toString()).isEqualTo("kavya@test.com");
        assertThat(sent.getSubject()).isEqualTo("Your tickets for Music <Night>");

        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        sent.writeTo(raw);
        String mime = raw.toString();
        assertThat(mime).contains("Content-ID: <ticket-1>", "Content-ID: <ticket-2>", "Content-ID: <ticket-3>");
        // Event title is HTML-escaped in the body
        assertThat(mime).contains("Music &lt;Night&gt;");
        assertThat(sent.getContent()).isInstanceOf(Multipart.class);
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
