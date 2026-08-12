package uy.com.fulbito.domain;
import jakarta.persistence.*;

@Entity @Table(name = "venue_images")
public class VenueImage {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private java.util.UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "venue_id") private Venue venue;
    @Column(nullable = false, columnDefinition = "text") private String url;
    @Column(name = "storage_key", columnDefinition = "text") private String storageKey;
    @Column(name = "sort_order", nullable = false) private short sortOrder;
    @Column(name = "is_cover", nullable = false) private boolean cover;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private java.time.OffsetDateTime createdAt;
    public java.util.UUID getId() { return id; } public Venue getVenue() { return venue; } public void setVenue(Venue venue) { this.venue = venue; }
    public String getUrl() { return url; } public void setUrl(String url) { this.url = url; } public String getStorageKey() { return storageKey; } public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public short getSortOrder() { return sortOrder; } public void setSortOrder(short sortOrder) { this.sortOrder = sortOrder; } public boolean isCover() { return cover; } public void setCover(boolean cover) { this.cover = cover; }
}
