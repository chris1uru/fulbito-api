package uy.com.fulbito.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uy.com.fulbito.domain.enums.*;
import java.math.BigDecimal;

@Entity
@Table(name = "courts")
public class Court extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false) private Venue venue;
    @Column(nullable = false, length = 80) private String name;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "football_format", nullable = false, columnDefinition = "football_format") private FootballFormat footballFormat;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "surface_type") private SurfaceType surface;
    @Column(nullable = false) private boolean covered;
    @Column(name = "price_per_slot", nullable = false, precision = 12, scale = 2) private BigDecimal pricePerSlot;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3, columnDefinition = "char(3)") private String currency;
    @Column(name = "slot_minutes", nullable = false) private short slotMinutes;
    @Column(nullable = false) private boolean active;

    public Venue getVenue() { return venue; } public void setVenue(Venue venue) { this.venue = venue; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public FootballFormat getFootballFormat() { return footballFormat; } public void setFootballFormat(FootballFormat footballFormat) { this.footballFormat = footballFormat; }
    public SurfaceType getSurface() { return surface; } public void setSurface(SurfaceType surface) { this.surface = surface; }
    public boolean isCovered() { return covered; } public void setCovered(boolean covered) { this.covered = covered; }
    public BigDecimal getPricePerSlot() { return pricePerSlot; } public void setPricePerSlot(BigDecimal pricePerSlot) { this.pricePerSlot = pricePerSlot; }
    public String getCurrency() { return currency; } public void setCurrency(String currency) { this.currency = currency; }
    public short getSlotMinutes() { return slotMinutes; } public void setSlotMinutes(short slotMinutes) { this.slotMinutes = slotMinutes; }
    public boolean isActive() { return active; } public void setActive(boolean active) { this.active = active; }
}
