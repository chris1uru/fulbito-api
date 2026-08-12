package uy.com.fulbito.dto;

import jakarta.validation.constraints.*;
import uy.com.fulbito.domain.enums.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class ReservationDtos {
    private ReservationDtos() {}
    public record ReservationRequest(
        @NotNull UUID courtId,
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt,
        @Size(max=161) String playerName,
        @Pattern(regexp="^\\+[1-9][0-9]{7,14}$") String playerPhone,
        @Size(max=500) String notes
    ) {}
    public record ReservationResponse(UUID id, UUID courtId, UUID playerId, OffsetDateTime startsAt,
        OffsetDateTime endsAt, ReservationStatus status, BigDecimal priceAmount, String currency,
        String playerName, String playerPhone, String notes, PaymentStatus paymentStatus,
        OffsetDateTime paidAt, OffsetDateTime cancelledAt) {}
}
