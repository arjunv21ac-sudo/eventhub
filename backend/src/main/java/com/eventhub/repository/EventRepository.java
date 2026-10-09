package com.eventhub.repository;

import com.eventhub.entity.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    // Upcoming events whose title or city matches the search text (empty text matches all)
    @Query("""
            SELECT e FROM Event e
            WHERE e.startTime > :now
              AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(e.city) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Event> searchUpcoming(@Param("q") String q, @Param("now") LocalDateTime now, Pageable pageable);

    List<Event> findByOrganizerEmailOrderByStartTimeDesc(String email);

    // SELECT ... FOR UPDATE: locks the event row until the booking transaction ends,
    // so two users can never book the last seat at the same time.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);
}
