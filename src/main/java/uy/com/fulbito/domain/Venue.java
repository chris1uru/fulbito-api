package uy.com.fulbito.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uy.com.fulbito.domain.enums.VenueStatus;

@Entity
@Table(name = "venues")
public class Venue extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false) private AppUser owner;
    @Column(nullable = false, length = 120) private String name;
    @Column(columnDefinition = "text") private String description;
    @Column(length = 20) private String phone;
    @Column(name = "whatsapp_phone", length = 20) private String whatsappPhone;
    @Column(nullable = false, length = 64) private String timezone;
    @Column(name = "cancellation_notice_hours", nullable = false) private short cancellationNoticeHours = 4;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "venue_status") private VenueStatus status;

    public AppUser getOwner() { return owner; }
    public void setOwner(AppUser owner) { this.owner = owner; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getWhatsappPhone() { return whatsappPhone; }
    public void setWhatsappPhone(String whatsappPhone) { this.whatsappPhone = whatsappPhone; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public short getCancellationNoticeHours() { return cancellationNoticeHours; }
    public void setCancellationNoticeHours(short cancellationNoticeHours) { this.cancellationNoticeHours = cancellationNoticeHours; }
    public VenueStatus getStatus() { return status; }
    public void setStatus(VenueStatus status) { this.status = status; }
}
