package uy.com.fulbito.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "court_blocks")
public class CourtBlock extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false) private Court court;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false) private AppUser createdBy;
    @Column(name = "starts_at", nullable = false) private OffsetDateTime startsAt;
    @Column(name = "ends_at", nullable = false) private OffsetDateTime endsAt;
    @Column(nullable = false, length = 300) private String reason;

    public Court getCourt() { return court; } public void setCourt(Court court) { this.court = court; }
    public AppUser getCreatedBy() { return createdBy; } public void setCreatedBy(AppUser createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getStartsAt() { return startsAt; } public void setStartsAt(OffsetDateTime startsAt) { this.startsAt = startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; } public void setEndsAt(OffsetDateTime endsAt) { this.endsAt = endsAt; }
    public String getReason() { return reason; } public void setReason(String reason) { this.reason = reason; }
}
