package com.eventhub.service;

import com.eventhub.dto.EventDtos.EventRequest;
import com.eventhub.dto.EventDtos.EventResponse;
import com.eventhub.dto.EventDtos.EventStatsResponse;
import com.eventhub.dto.PageResponse;
import com.eventhub.entity.BookingStatus;
import com.eventhub.entity.Event;
import com.eventhub.entity.User;
import com.eventhub.exception.BadRequestException;
import com.eventhub.exception.ResourceNotFoundException;
import com.eventhub.repository.BookingRepository;
import com.eventhub.repository.EventRepository;
import com.eventhub.repository.TicketRepository;
import com.eventhub.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    private static final int MAX_PAGE_SIZE = 50;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;

    public EventService(EventRepository eventRepository, UserRepository userRepository,
                        BookingRepository bookingRepository, TicketRepository ticketRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
    }

    // ---------- Public ----------

    @Transactional(readOnly = true)
    public PageResponse<EventResponse> searchUpcoming(String query, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("startTime").ascending());
        String q = query == null ? "" : query.trim();
        return PageResponse.of(eventRepository.searchUpcoming(q, LocalDateTime.now(), pageable)
                .map(EventResponse::from));
    }

    @Transactional(readOnly = true)
    public EventResponse getById(Long id) {
        return EventResponse.from(findEvent(id));
    }

    // ---------- Organizer ----------

    @Transactional(readOnly = true)
    public List<EventResponse> getOrganizerEvents(String organizerEmail) {
        return eventRepository.findByOrganizerEmailOrderByStartTimeDesc(organizerEmail).stream()
                .map(EventResponse::from)
                .toList();
    }

    @Transactional
    public EventResponse create(EventRequest request, String organizerEmail) {
        User organizer = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Event event = new Event();
        applyRequest(event, request);
        event.setTotalSeats(request.totalSeats());
        event.setAvailableSeats(request.totalSeats());
        event.setOrganizer(organizer);
        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional
    public EventResponse update(Long id, EventRequest request, String organizerEmail) {
        // Lock the row so seat numbers can't change under us while bookings happen
        Event event = eventRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + id));
        checkOwner(event, organizerEmail);

        int bookedSeats = event.getTotalSeats() - event.getAvailableSeats();
        if (request.totalSeats() < bookedSeats) {
            throw new BadRequestException("Total seats cannot be less than already booked seats (" + bookedSeats + ")");
        }

        applyRequest(event, request);
        event.setTotalSeats(request.totalSeats());
        event.setAvailableSeats(request.totalSeats() - bookedSeats);
        return EventResponse.from(event);
    }

    @Transactional
    public void delete(Long id, String organizerEmail) {
        Event event = findEvent(id);
        checkOwner(event, organizerEmail);
        if (bookingRepository.existsByEventId(id)) {
            throw new BadRequestException("Cannot delete an event that already has bookings");
        }
        eventRepository.delete(event);
    }

    @Transactional(readOnly = true)
    public EventStatsResponse getStats(Long id, String organizerEmail) {
        Event event = findEvent(id);
        checkOwner(event, organizerEmail);
        return new EventStatsResponse(
                event.getId(),
                event.getTitle(),
                event.getTotalSeats(),
                ticketRepository.countByEventAndStatus(id, BookingStatus.CONFIRMED),
                ticketRepository.countCheckedIn(id),
                bookingRepository.sumAmountByEventAndStatus(id, BookingStatus.CONFIRMED));
    }

    // ---------- Helpers ----------

    private Event findEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + id));
    }

    private void checkOwner(Event event, String organizerEmail) {
        if (!event.getOrganizer().getEmail().equals(organizerEmail)) {
            throw new AccessDeniedException("You can only manage your own events");
        }
    }

    private void applyRequest(Event event, EventRequest request) {
        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setVenue(request.venue().trim());
        event.setCity(request.city().trim());
        event.setStartTime(request.startTime());
        event.setPrice(request.price());
    }
}
