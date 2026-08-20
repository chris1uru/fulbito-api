package uy.com.fulbito.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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

    public record AvailableCourtResponse(
        UUID courtId,
        String courtName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        BigDecimal priceAmount,
        String currency,
        short slotMinutes
    ) {}

    public record VenueAvailabilityResponse(
        UUID venueId,
        boolean available,
        int totalActiveCourts,
        List<AvailableCourtResponse> availableCourts
    ) {}

    public record VenueAvailabilitySearchResponse(
        LocalDate date,
        LocalTime time,
        String timezone,
        List<VenueAvailabilityResponse> venues
    ) {}
}
