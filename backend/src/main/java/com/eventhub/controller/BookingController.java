package com.eventhub.controller;

import com.eventhub.dto.BookingDtos.BookingRequest;
import com.eventhub.dto.BookingDtos.BookingResponse;
import com.eventhub.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse book(@Valid @RequestBody BookingRequest request,
                                @AuthenticationPrincipal UserDetails user) {
        return bookingService.book(request, user.getUsername());
    }

    @GetMapping("/my")
    public List<BookingResponse> myBookings(@AuthenticationPrincipal UserDetails user) {
        return bookingService.getMyBookings(user.getUsername());
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
        return bookingService.cancel(id, user.getUsername());
    }
}
