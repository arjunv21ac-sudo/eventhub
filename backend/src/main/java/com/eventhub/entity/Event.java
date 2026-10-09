package com.eventhub.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String venue;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    private int totalSeats;

    // Decremented on booking, incremented on cancellation.
    // Updated under a pessimistic row lock (see EventRepository.findByIdForUpdate).
    private int availableSeats;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id")
    private User organizer;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public User getOrganizer() { return organizer; }
    public void setOrganizer(User organizer) { this.organizer = organizer; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
