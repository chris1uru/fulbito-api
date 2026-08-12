package uy.com.fulbito.dto;

import jakarta.validation.constraints.*;
import uy.com.fulbito.domain.enums.*;
import java.math.BigDecimal;
import java.util.UUID;

public final class CourtDtos {
    private CourtDtos() {}
    public record CourtRequest(
        @NotBlank @Size(max=80) String name,
        @NotNull FootballFormat footballFormat,
        @NotNull SurfaceType surface,
        boolean covered,
        @NotNull @DecimalMin("0.00") BigDecimal pricePerSlot,
        @Min(30) @Max(240) short slotMinutes,
        boolean active
    ) {}
    public record CourtResponse(UUID id, UUID venueId, String name, FootballFormat footballFormat,
                                SurfaceType surface, boolean covered, BigDecimal pricePerSlot,
                                String currency, short slotMinutes, boolean active) {}
}
