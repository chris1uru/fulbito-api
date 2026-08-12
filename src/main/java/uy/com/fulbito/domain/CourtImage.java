package uy.com.fulbito.domain;
import jakarta.persistence.*;

@Entity @Table(name = "court_images")
public class CourtImage {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private java.util.UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "court_id") private Court court;
    @Column(nullable = false, columnDefinition = "text") private String url;
    @Column(name = "storage_key", columnDefinition = "text") private String storageKey;
    @Column(name = "sort_order", nullable = false) private short sortOrder;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private java.time.OffsetDateTime createdAt;
    public java.util.UUID getId() { return id; } public Court getCourt() { return court; } public void setCourt(Court court) { this.court = court; }
    public String getUrl() { return url; } public void setUrl(String url) { this.url = url; } public String getStorageKey() { return storageKey; } public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public short getSortOrder() { return sortOrder; } public void setSortOrder(short sortOrder) { this.sortOrder = sortOrder; }
}
