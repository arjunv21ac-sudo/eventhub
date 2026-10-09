package com.eventhub.controller;

import com.eventhub.dto.EventDtos.EventResponse;
import com.eventhub.dto.PageResponse;
import com.eventhub.service.EventService;
import org.springframework.web.bind.annotation.*;

// Public endpoints: anyone can browse events without logging in
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // e.g. GET /api/events?q=music&page=0&size=9
    @GetMapping
    public PageResponse<EventResponse> search(@RequestParam(required = false) String q,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "9") int size) {
        return eventService.searchUpcoming(q, page, size);
    }

    @GetMapping("/{id}")
    public EventResponse getById(@PathVariable Long id) {
        return eventService.getById(id);
    }
}
