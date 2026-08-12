package uy.com.fulbito.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "venue_locations")
public class VenueLocation extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false, unique = true) private Venue venue;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_code", nullable = false) private Department department;
    @Column(nullable = false, length = 100) private String city;
    @Column(length = 100) private String neighborhood;
    @Column(nullable = false, length = 120) private String street;
    @Column(name = "street_number", length = 20) private String streetNumber;
    @Column(columnDefinition = "text") private String reference;
    @Column(nullable = false, precision = 9, scale = 6) private BigDecimal latitude;
    @Column(nullable = false, precision = 9, scale = 6) private BigDecimal longitude;

    public Venue getVenue() { return venue; } public void setVenue(Venue venue) { this.venue = venue; }
    public Department getDepartment() { return department; } public void setDepartment(Department department) { this.department = department; }
    public String getCity() { return city; } public void setCity(String city) { this.city = city; }
    public String getNeighborhood() { return neighborhood; } public void setNeighborhood(String neighborhood) { this.neighborhood = neighborhood; }
    public String getStreet() { return street; } public void setStreet(String street) { this.street = street; }
    public String getStreetNumber() { return streetNumber; } public void setStreetNumber(String streetNumber) { this.streetNumber = streetNumber; }
    public String getReference() { return reference; } public void setReference(String reference) { this.reference = reference; }
    public BigDecimal getLatitude() { return latitude; } public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; } public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
}
