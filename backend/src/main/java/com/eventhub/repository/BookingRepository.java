package com.eventhub.repository;

import com.eventhub.entity.Booking;
import com.eventhub.entity.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // @EntityGraph loads event + tickets in the same query (avoids the N+1 query problem)
    @EntityGraph(attributePaths = {"event", "tickets"})
    List<Booking> findByUserEmailOrderByBookedAtDesc(String email);

    // Everything the confirmation email needs, in one query
    @EntityGraph(attributePaths = {"user", "event", "tickets"})
    Optional<Booking> findWithDetailsById(Long id);

    boolean existsByEventId(Long eventId);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.event.id = :eventId AND b.status = :status")
    BigDecimal sumAmountByEventAndStatus(@Param("eventId") Long eventId, @Param("status") BookingStatus status);
}
