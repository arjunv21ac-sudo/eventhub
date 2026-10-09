package com.eventhub.controller;

import com.eventhub.dto.BookingDtos.CheckInResponse;
import com.eventhub.dto.EventDtos.EventRequest;
import com.eventhub.dto.EventDtos.EventResponse;
import com.eventhub.dto.EventDtos.EventStatsResponse;
import com.eventhub.service.EventService;
import com.eventhub.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Every endpoint here requires ROLE_ORGANIZER (configured in SecurityConfig)
@RestController
@RequestMapping("/api/organizer")
public class OrganizerController {

    private final EventService eventService;
    private final TicketService ticketService;

    public OrganizerController(EventService eventService, TicketService ticketService) {
        this.eventService = eventService;
        this.ticketService = ticketService;
    }

    @GetMapping("/events")
    public List<EventResponse> myEvents(@AuthenticationPrincipal UserDetails user) {
        return eventService.getOrganizerEvents(user.getUsername());
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@Valid @RequestBody EventRequest request,
                                @AuthenticationPrincipal UserDetails user) {
        return eventService.create(request, user.getUsername());
    }

    @PutMapping("/events/{id}")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request,
                                @AuthenticationPrincipal UserDetails user) {
        return eventService.update(id, request, user.getUsername());
    }

    @DeleteMapping("/events/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
        eventService.delete(id, user.getUsername());
    }

    @GetMapping("/events/{id}/stats")
    public EventStatsResponse stats(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
        return eventService.getStats(id, user.getUsername());
    }

    // Called by the QR scanner page at the venue gate
    @PostMapping("/checkin/{ticketCode}")
    public CheckInResponse checkIn(@PathVariable String ticketCode, @AuthenticationPrincipal UserDetails user) {
        return ticketService.checkIn(ticketCode, user.getUsername());
    }
}
