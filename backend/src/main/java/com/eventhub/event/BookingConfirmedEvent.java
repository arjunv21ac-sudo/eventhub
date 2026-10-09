package com.eventhub.event;

// Published by BookingService when a booking is saved. Listeners react after the transaction commits.
public record BookingConfirmedEvent(Long bookingId) {
}
