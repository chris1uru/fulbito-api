package uy.com.fulbito.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uy.com.fulbito.domain.enums.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "reservations")
public class Reservation extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false) private Court court;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id") private AppUser player;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false) private AppUser createdBy;
    @Column(name = "starts_at", nullable = false) private OffsetDateTime startsAt;
    @Column(name = "ends_at", nullable = false) private OffsetDateTime endsAt;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "reservation_status") private ReservationStatus status;
    @Column(name = "price_amount", nullable = false, precision = 12, scale = 2) private BigDecimal priceAmount;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3, columnDefinition = "char(3)") private String currency;
    @Column(name = "player_name_snapshot", nullable = false, length = 161) private String playerNameSnapshot;
    @Column(name = "player_phone_snapshot", length = 20) private String playerPhoneSnapshot;
    @Column(length = 500) private String notes;
    @Column(name = "cancellation_notice_hours", nullable = false) private short cancellationNoticeHours;
    @Column(name = "late_cancellation", nullable = false) private boolean lateCancellation;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "payment_status", nullable = false, columnDefinition = "payment_status") private PaymentStatus paymentStatus;
    @Column(name = "paid_at") private OffsetDateTime paidAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "paid_confirmed_by_user_id") private AppUser paidConfirmedBy;
    @Column(name = "cancelled_at") private OffsetDateTime cancelledAt;

    public Court getCourt() { return court; } public void setCourt(Court court) { this.court = court; }
    public AppUser getPlayer() { return player; } public void setPlayer(AppUser player) { this.player = player; }
    public AppUser getCreatedBy() { return createdBy; } public void setCreatedBy(AppUser createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getStartsAt() { return startsAt; } public void setStartsAt(OffsetDateTime startsAt) { this.startsAt = startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; } public void setEndsAt(OffsetDateTime endsAt) { this.endsAt = endsAt; }
    public ReservationStatus getStatus() { return status; } public void setStatus(ReservationStatus status) { this.status = status; }
    public BigDecimal getPriceAmount() { return priceAmount; } public void setPriceAmount(BigDecimal priceAmount) { this.priceAmount = priceAmount; }
    public String getCurrency() { return currency; } public void setCurrency(String currency) { this.currency = currency; }
    public String getPlayerNameSnapshot() { return playerNameSnapshot; } public void setPlayerNameSnapshot(String playerNameSnapshot) { this.playerNameSnapshot = playerNameSnapshot; }
    public String getPlayerPhoneSnapshot() { return playerPhoneSnapshot; } public void setPlayerPhoneSnapshot(String playerPhoneSnapshot) { this.playerPhoneSnapshot = playerPhoneSnapshot; }
    public String getNotes() { return notes; } public void setNotes(String notes) { this.notes = notes; }
    public short getCancellationNoticeHours() { return cancellationNoticeHours; } public void setCancellationNoticeHours(short cancellationNoticeHours) { this.cancellationNoticeHours = cancellationNoticeHours; }
    public boolean isLateCancellation() { return lateCancellation; } public void setLateCancellation(boolean lateCancellation) { this.lateCancellation = lateCancellation; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; } public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public OffsetDateTime getPaidAt() { return paidAt; } public void setPaidAt(OffsetDateTime paidAt) { this.paidAt = paidAt; }
    public AppUser getPaidConfirmedBy() { return paidConfirmedBy; } public void setPaidConfirmedBy(AppUser paidConfirmedBy) { this.paidConfirmedBy = paidConfirmedBy; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; } public void setCancelledAt(OffsetDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
