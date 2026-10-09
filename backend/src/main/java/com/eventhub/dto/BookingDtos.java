package com.eventhub.dto;

import com.eventhub.entity.Booking;
import com.eventhub.entity.BookingStatus;
import com.eventhub.entity.Ticket;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class BookingDtos {

    private BookingDtos() {
    }

    public record BookingRequest(
            @NotNull(message = "Event id is required") Long eventId,
            @Min(value = 1, message = "Book at least 1 ticket") @Max(value = 10, message = "You can book at most 10 tickets at once") int quantity) {
    }

    public record TicketResponse(String ticketCode, boolean checkedIn, LocalDateTime checkedInAt) {

        public static TicketResponse from(Ticket t) {
            return new TicketResponse(t.getTicketCode(), t.isCheckedIn(), t.getCheckedInAt());
        }
    }

    public record BookingResponse(
            Long id,
            Long eventId,
            String eventTitle,
            String venue,
            String city,
            LocalDateTime eventStartTime,
            int quantity,
            BigDecimal totalAmount,
            BookingStatus status,
            LocalDateTime bookedAt,
            List<TicketResponse> tickets) {

        public static BookingResponse from(Booking b) {
            var e = b.getEvent();
            return new BookingResponse(b.getId(), e.getId(), e.getTitle(), e.getVenue(), e.getCity(),
                    e.getStartTime(), b.getQuantity(), b.getTotalAmount(), b.getStatus(), b.getBookedAt(),
                    b.getTickets().stream().map(TicketResponse::from).toList());
        }
    }

    public record CheckInResponse(
            String ticketCode,
            String eventTitle,
            String attendeeName,
            LocalDateTime checkedInAt,
            String message) {
    }
}
