package uy.com.fulbito.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "match_request_availabilities")
public class MatchRequestAvailability extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_request_id", nullable = false) private MatchRequest matchRequest;
    @Column(name = "starts_at", nullable = false) private OffsetDateTime startsAt;
    @Column(name = "ends_at", nullable = false) private OffsetDateTime endsAt;

    public MatchRequest getMatchRequest() { return matchRequest; }
    public void setMatchRequest(MatchRequest matchRequest) { this.matchRequest = matchRequest; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(OffsetDateTime startsAt) { this.startsAt = startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(OffsetDateTime endsAt) { this.endsAt = endsAt; }
}
