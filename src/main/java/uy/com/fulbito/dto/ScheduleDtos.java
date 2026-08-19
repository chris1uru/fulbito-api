package uy.com.fulbito.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;

public final class ScheduleDtos {
    private ScheduleDtos() {}
    public record OpeningHourRequest(
        @Min(1) @Max(7) short dayOfWeek,
        @NotBlank @Pattern(regexp="^([01][0-9]|2[0-3]):[0-5][0-9]$") String opensAt,
        @NotBlank @Pattern(regexp="^([01][0-9]|2[0-3]):[0-5][0-9]$") String closesAt
    ) {}
    public record OpeningHourResponse(UUID id, UUID venueId, short dayOfWeek, LocalTime opensAt, LocalTime closesAt) {}
    public record CourtBlockRequest(@NotNull OffsetDateTime startsAt, @NotNull OffsetDateTime endsAt,
                                    @NotBlank @Size(min=2,max=300) String reason) {}
    public record CourtBlockResponse(UUID id, UUID courtId, OffsetDateTime startsAt, OffsetDateTime endsAt, String reason) {}
}
