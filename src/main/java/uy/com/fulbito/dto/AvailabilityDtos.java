package uy.com.fulbito.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class AvailabilityDtos {
    private AvailabilityDtos() {}

    public record AvailabilitySlotResponse(
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        boolean available
    ) {}

    public record CourtAvailabilityResponse(
        UUID courtId,
        LocalDate date,
        String timezone,
        short slotMinutes,
        List<AvailabilitySlotResponse> slots
    ) {}
}
