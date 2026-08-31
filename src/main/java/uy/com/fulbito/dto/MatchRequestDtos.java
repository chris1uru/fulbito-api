package uy.com.fulbito.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import uy.com.fulbito.domain.enums.*;
import java.time.OffsetDateTime;
import java.util.*;

public final class MatchRequestDtos {
    private MatchRequestDtos() {}

    public record AvailabilityRequest(
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt
    ) {}

    public record CreateMatchRequestRequest(
        @NotNull FootballFormat footballFormat,
        @NotNull MatchStyle style,
        UUID reservationId,
        @Size(max = 500) String notes,
        @Size(max = 6) List<@Valid AvailabilityRequest> availabilities
    ) {}

    public record AvailabilityResponse(UUID id, OffsetDateTime startsAt, OffsetDateTime endsAt) {}

    public record MatchRequestResponse(
        UUID id,
        UUID creatorId,
        String creatorName,
        boolean mine,
        FootballFormat footballFormat,
        MatchStyle style,
        MatchRequestStatus status,
        String notes,
        UUID reservationId,
        UUID courtId,
        String courtName,
        UUID venueId,
        String venueName,
        OffsetDateTime reservedStartsAt,
        OffsetDateTime reservedEndsAt,
        List<AvailabilityResponse> availabilities,
        long interestCount,
        boolean interested,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt
    ) {}

    public record MatchInterestResponse(
        UUID id,
        UUID playerId,
        String playerName,
        String playerPhone,
        OffsetDateTime createdAt
    ) {}
}
