package com.eventhub.dto;

import com.eventhub.entity.Event;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class EventDtos {

    private EventDtos() {
    }

    public record EventRequest(
            @NotBlank(message = "Title is required") @Size(max = 150) String title,
            @Size(max = 2000) String description,
            @NotBlank(message = "Venue is required") String venue,
            @NotBlank(message = "City is required") @Size(max = 100) String city,
            @NotNull(message = "Start time is required") @Future(message = "Start time must be in the future") LocalDateTime startTime,
            @NotNull(message = "Price is required") @DecimalMin(value = "0", message = "Price cannot be negative") BigDecimal price,
            @Min(value = 1, message = "At least 1 seat is required") @Max(value = 100000) int totalSeats) {
    }

    public record EventResponse(
            Long id,
            String title,
            String description,
            String venue,
            String city,
            LocalDateTime startTime,
            BigDecimal price,
            int totalSeats,
            int availableSeats,
            String organizerName) {

        public static EventResponse from(Event e) {
            return new EventResponse(e.getId(), e.getTitle(), e.getDescription(), e.getVenue(), e.getCity(),
                    e.getStartTime(), e.getPrice(), e.getTotalSeats(), e.getAvailableSeats(),
                    e.getOrganizer().getName());
        }
    }

    public record EventStatsResponse(
            Long eventId,
            String title,
            int totalSeats,
            long ticketsSold,
            long checkedIn,
            BigDecimal revenue) {
    }
}
