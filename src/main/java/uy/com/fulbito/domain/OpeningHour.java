package uy.com.fulbito.domain;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "opening_hours")
public class OpeningHour extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false) private Venue venue;
    @Column(name = "day_of_week", nullable = false) private short dayOfWeek;
    @Column(name = "opens_at", nullable = false) private LocalTime opensAt;
    @Column(name = "closes_at", nullable = false) private LocalTime closesAt;
    @Column(name = "open_minute", insertable = false, updatable = false) private Integer openMinute;
    @Column(name = "close_minute", insertable = false, updatable = false) private Integer closeMinute;

    public Venue getVenue() { return venue; } public void setVenue(Venue venue) { this.venue = venue; }
    public short getDayOfWeek() { return dayOfWeek; } public void setDayOfWeek(short dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getOpensAt() { return opensAt; } public void setOpensAt(LocalTime opensAt) { this.opensAt = opensAt; }
    public LocalTime getClosesAt() { return closesAt; } public void setClosesAt(LocalTime closesAt) { this.closesAt = closesAt; }
}
