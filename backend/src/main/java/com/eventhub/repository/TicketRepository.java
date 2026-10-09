package com.eventhub.repository;

import com.eventhub.entity.BookingStatus;
import com.eventhub.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.booking.event.id = :eventId AND t.booking.status = :status")
    long countByEventAndStatus(@Param("eventId") Long eventId, @Param("status") BookingStatus status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.booking.event.id = :eventId AND t.checkedIn = true")
    long countCheckedIn(@Param("eventId") Long eventId);
}
