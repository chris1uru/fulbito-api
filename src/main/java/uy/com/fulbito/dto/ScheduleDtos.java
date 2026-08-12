package uy.com.fulbito.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;

public final class ScheduleDtos {
    private ScheduleDtos() {}
    public record OpeningHourRequest(@Min(1) @Max(7) short dayOfWeek, @NotNull LocalTime opensAt, @NotNull LocalTime closesAt) {}
    public record OpeningHourResponse(UUID id, UUID venueId, short dayOfWeek, LocalTime opensAt, LocalTime closesAt) {}
    public record CourtBlockRequest(@NotNull OffsetDateTime startsAt, @NotNull OffsetDateTime endsAt,
                                    @NotBlank @Size(min=2,max=300) String reason) {}
    public record CourtBlockResponse(UUID id, UUID courtId, OffsetDateTime startsAt, OffsetDateTime endsAt, String reason) {}
}
