package com.eventhub.service;

import com.eventhub.dto.BookingDtos.BookingRequest;
import com.eventhub.dto.BookingDtos.BookingResponse;
import com.eventhub.entity.*;
import com.eventhub.event.BookingConfirmedEvent;
import com.eventhub.exception.BadRequestException;
import com.eventhub.exception.ResourceNotFoundException;
import com.eventhub.repository.BookingRepository;
import com.eventhub.repository.EventRepository;
import com.eventhub.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BookingService(BookingRepository bookingRepository, EventRepository eventRepository,
                          UserRepository userRepository, ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Books seats and creates one QR ticket per seat.
     * The whole method is one transaction: if anything fails, nothing is saved.
     */
    @Transactional
    public BookingResponse book(BookingRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Pessimistic lock: other bookings for this event wait here until we commit
        Event event = eventRepository.findByIdForUpdate(request.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + request.eventId()));

        if (!event.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Bookings are closed for this event");
        }
        if (event.getAvailableSeats() < request.quantity()) {
            throw new BadRequestException(event.getAvailableSeats() == 0
                    ? "Sorry, this event is sold out"
                    : "Only " + event.getAvailableSeats() + " seat(s) left");
        }

        event.setAvailableSeats(event.getAvailableSeats() - request.quantity());

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setEvent(event);
        booking.setQuantity(request.quantity());
        booking.setTotalAmount(event.getPrice().multiply(BigDecimal.valueOf(request.quantity())));
        booking.setStatus(BookingStatus.CONFIRMED);

        for (int i = 0; i < request.quantity(); i++) {
            Ticket ticket = new Ticket();
            ticket.setTicketCode(UUID.randomUUID().toString());
            booking.addTicket(ticket);
        }

        bookingRepository.save(booking);

        // The email is sent only after this transaction commits (see BookingEmailService)
        eventPublisher.publishEvent(new BookingConfirmedEvent(booking.getId()));
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String userEmail) {
        return bookingRepository.findByUserEmailOrderByBookedAtDesc(userEmail).stream()
                .map(BookingResponse::from)
                .toList();
    }

    @Transactional
    public BookingResponse cancel(Long bookingId, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + bookingId));

        if (!booking.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("You can only cancel your own bookings");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled");
        }
        if (booking.getTickets().stream().anyMatch(Ticket::isCheckedIn)) {
            throw new BadRequestException("Cannot cancel: a ticket from this booking was already used");
        }

        Event event = eventRepository.findByIdForUpdate(booking.getEvent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        if (!event.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Cannot cancel after the event has started");
        }

        // Give the seats back
        event.setAvailableSeats(event.getAvailableSeats() + booking.getQuantity());
        booking.setStatus(BookingStatus.CANCELLED);
        return BookingResponse.from(booking);
    }
}
