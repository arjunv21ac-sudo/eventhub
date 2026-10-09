package com.eventhub.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Random UUID encoded inside the QR code. Unguessable, so it is safe to show publicly.
    @Column(nullable = false, unique = true, length = 36)
    private String ticketCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    private boolean checkedIn;

    private LocalDateTime checkedInAt;

    // Optimistic locking: if two gates scan the same ticket at the same moment,
    // only the first update succeeds and the second fails with a version conflict.
    @Version
    private Long version;

    public Long getId() { return id; }

    public String getTicketCode() { return ticketCode; }
    public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public boolean isCheckedIn() { return checkedIn; }
    public void setCheckedIn(boolean checkedIn) { this.checkedIn = checkedIn; }

    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(LocalDateTime checkedInAt) { this.checkedInAt = checkedInAt; }

    public Long getVersion() { return version; }
}
