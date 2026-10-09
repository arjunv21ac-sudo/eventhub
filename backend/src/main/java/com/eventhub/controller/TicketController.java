package com.eventhub.controller;

import com.eventhub.service.TicketService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // Public so it works directly in <img src="..."> tags and emails.
    // Safe because ticket codes are random UUIDs that can't be guessed.
    @GetMapping(value = "/{ticketCode}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrCode(@PathVariable String ticketCode) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .body(ticketService.generateQrPng(ticketCode));
    }
}
