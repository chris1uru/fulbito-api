package uy.com.fulbito.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uy.com.fulbito.domain.enums.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "match_requests")
public class MatchRequest extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_user_id", nullable = false) private AppUser creator;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id") private Reservation reservation;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "football_format", nullable = false, columnDefinition = "football_format") private FootballFormat footballFormat;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "match_style") private MatchStyle style;
    @Column(length = 500) private String notes;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "match_request_status") private MatchRequestStatus status;
    @Column(name = "expires_at", nullable = false) private OffsetDateTime expiresAt;

    public AppUser getCreator() { return creator; }
    public void setCreator(AppUser creator) { this.creator = creator; }
    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }
    public FootballFormat getFootballFormat() { return footballFormat; }
    public void setFootballFormat(FootballFormat footballFormat) { this.footballFormat = footballFormat; }
    public MatchStyle getStyle() { return style; }
    public void setStyle(MatchStyle style) { this.style = style; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public MatchRequestStatus getStatus() { return status; }
    public void setStatus(MatchRequestStatus status) { this.status = status; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
}
