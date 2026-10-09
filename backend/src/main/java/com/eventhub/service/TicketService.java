package com.eventhub.service;

import com.eventhub.dto.BookingDtos.CheckInResponse;
import com.eventhub.entity.Booking;
import com.eventhub.entity.BookingStatus;
import com.eventhub.entity.Event;
import com.eventhub.entity.Ticket;
import com.eventhub.exception.BadRequestException;
import com.eventhub.exception.ConflictException;
import com.eventhub.exception.ResourceNotFoundException;
import com.eventhub.repository.TicketRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TicketService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final TicketRepository ticketRepository;
    private final QrCodeService qrCodeService;

    public TicketService(TicketRepository ticketRepository, QrCodeService qrCodeService) {
        this.ticketRepository = ticketRepository;
        this.qrCodeService = qrCodeService;
    }

    /** Returns a PNG image of a QR code that contains the ticket code. */
    @Transactional(readOnly = true)
    public byte[] generateQrPng(String ticketCode) {
        return qrCodeService.generatePng(findTicket(ticketCode).getTicketCode());
    }

    /**
     * Called when an organizer scans a ticket at the venue gate.
     * Rejects fake, cancelled, already-used and other-organizer tickets.
     */
    @Transactional
    public CheckInResponse checkIn(String ticketCode, String organizerEmail) {
        Ticket ticket = findTicket(ticketCode);
        Booking booking = ticket.getBooking();
        Event event = booking.getEvent();

        if (!event.getOrganizer().getEmail().equals(organizerEmail)) {
            throw new AccessDeniedException("This ticket is for an event you don't organize");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("This ticket was cancelled");
        }
        if (ticket.isCheckedIn()) {
            throw new ConflictException("Ticket already used. Checked in at " + ticket.getCheckedInAt().format(TIME_FORMAT));
        }

        ticket.setCheckedIn(true);
        ticket.setCheckedInAt(LocalDateTime.now());
        // Flush now so a @Version conflict (same ticket scanned twice at once) is detected here
        ticketRepository.saveAndFlush(ticket);

        return new CheckInResponse(ticket.getTicketCode(), event.getTitle(), booking.getUser().getName(),
                ticket.getCheckedInAt(), "Welcome, " + booking.getUser().getName() + "! Check-in successful.");
    }

    private Ticket findTicket(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid ticket: no ticket matches this code"));
    }
}
