package com.eventhub.service;

import com.eventhub.entity.Booking;
import com.eventhub.entity.Event;
import com.eventhub.entity.Ticket;
import com.eventhub.event.BookingConfirmedEvent;
import com.eventhub.repository.BookingRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static org.springframework.web.util.HtmlUtils.htmlEscape;

/**
 * Emails the QR tickets to the user after a successful booking.
 *
 * - AFTER_COMMIT: no email is sent if the booking transaction rolls back.
 * - @Async: the user gets the booking response immediately; the slow SMTP call runs in the background.
 * - If sending fails, it is logged. The booking itself is already saved and still valid.
 */
@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class BookingEmailService {

    private static final Logger log = LoggerFactory.getLogger(BookingEmailService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy 'at' hh:mm a");

    private final JavaMailSender mailSender;
    private final BookingRepository bookingRepository;
    private final QrCodeService qrCodeService;
    private final String fromAddress;
    private final String frontendUrl;

    public BookingEmailService(JavaMailSender mailSender, BookingRepository bookingRepository,
                               QrCodeService qrCodeService,
                               @Value("${app.mail.from}") String fromAddress,
                               @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.bookingRepository = bookingRepository;
        this.qrCodeService = qrCodeService;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        bookingRepository.findWithDetailsById(event.bookingId()).ifPresent(this::send);
    }

    private void send(Booking booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // multipart = true so we can embed the QR images inside the email
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(booking.getUser().getEmail());
            helper.setSubject("Your tickets for " + booking.getEvent().getTitle());
            helper.setText(buildHtml(booking), true);

            // Each QR is attached inline and referenced in the HTML as <img src="cid:ticket-N">
            List<Ticket> tickets = booking.getTickets();
            for (int i = 0; i < tickets.size(); i++) {
                byte[] png = qrCodeService.generatePng(tickets.get(i).getTicketCode());
                helper.addInline("ticket-" + (i + 1), new ByteArrayResource(png), "image/png");
            }

            mailSender.send(message);
            log.info("Booking confirmation email sent for booking {}", booking.getId());
        } catch (MessagingException | MailException e) {
            log.error("Could not send confirmation email for booking {}", booking.getId(), e);
        }
    }

    private String buildHtml(Booking booking) {
        Event event = booking.getEvent();
        String amount = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).format(booking.getTotalAmount());

        StringBuilder tickets = new StringBuilder();
        for (int i = 0; i < booking.getTickets().size(); i++) {
            String shortCode = booking.getTickets().get(i).getTicketCode().substring(0, 8).toUpperCase();
            tickets.append("""
                    <div style="display:inline-block;margin:8px;padding:12px;border:2px dashed #d0d3e0;border-radius:12px;text-align:center">
                      <img src="cid:ticket-%d" width="180" height="180" alt="Ticket %d QR code"><br>
                      <strong>Ticket %d</strong><br>
                      <code style="color:#6b6f80">%s</code>
                    </div>
                    """.formatted(i + 1, i + 1, i + 1, shortCode));
        }

        // htmlEscape: user-entered text must never be inserted into HTML as-is
        return """
                <div style="font-family:Segoe UI,Arial,sans-serif;max-width:640px;margin:auto;color:#1c1d29">
                  <h2 style="color:#5b4bdb">🎟️ Booking confirmed!</h2>
                  <p>Hi %s, your tickets are ready. Show these QR codes at the entrance.</p>
                  <div style="background:#f6f7fb;border-radius:12px;padding:16px;margin:16px 0">
                    <h3 style="margin:0 0 8px">%s</h3>
                    <p style="margin:4px 0">🕒 %s</p>
                    <p style="margin:4px 0">📍 %s, %s</p>
                    <p style="margin:4px 0">Booking #%d · %d ticket(s) · %s</p>
                  </div>
                  <div>%s</div>
                  <p style="color:#6b6f80;font-size:13px">You can also see your tickets anytime at
                    <a href="%s/my-bookings">%s/my-bookings</a></p>
                </div>
                """.formatted(
                htmlEscape(booking.getUser().getName()),
                htmlEscape(event.getTitle()),
                event.getStartTime().format(DATE_FORMAT),
                htmlEscape(event.getVenue()), htmlEscape(event.getCity()),
                booking.getId(), booking.getQuantity(), amount,
                tickets,
                frontendUrl, frontendUrl);
    }
}
