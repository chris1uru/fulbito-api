package uy.com.fulbito.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import uy.com.fulbito.domain.enums.VenueStatus;
import java.math.BigDecimal;
import java.util.UUID;

public final class VenueDtos {
    private VenueDtos() {}
    public record LocationRequest(
        @NotBlank @Size(min=3,max=3) String departmentCode,
        @NotBlank @Size(max=100) String city,
        @Size(max=100) String neighborhood,
        @NotBlank @Size(max=120) String street,
        @Size(max=20) String streetNumber,
        String reference,
        @NotNull @DecimalMin("-35.100000") @DecimalMax("-30.000000") BigDecimal latitude,
        @NotNull @DecimalMin("-58.600000") @DecimalMax("-53.000000") BigDecimal longitude
    ) {}
    public record VenueRequest(
        @NotBlank @Size(min=2,max=120) String name,
        String description,
        @Pattern(regexp="^\\+[1-9][0-9]{7,14}$") String phone,
        @Pattern(regexp="^\\+[1-9][0-9]{7,14}$") String whatsappPhone,
        @Min(0) @Max(168) Short cancellationNoticeHours,
        @NotNull VenueStatus status,
        @NotNull @Valid LocationRequest location
    ) {}
    public record AdminVenueRequest(
        @NotNull UUID ownerId,
        @NotBlank @Size(min=2,max=120) String name,
        String description,
        @Pattern(regexp="^\\+[1-9][0-9]{7,14}$") String phone,
        @Pattern(regexp="^\\+[1-9][0-9]{7,14}$") String whatsappPhone,
        @Min(0) @Max(168) Short cancellationNoticeHours,
        @NotNull VenueStatus status,
        @NotNull @Valid LocationRequest location
    ) {
        public VenueRequest venueRequest() {
            return new VenueRequest(name, description, phone, whatsappPhone, cancellationNoticeHours, status, location);
        }
    }
    public record AssignOwnerRequest(@NotNull UUID ownerId) {}
    public record UpdateVenueStatusRequest(@NotNull VenueStatus status) {}
    public record OwnerSummaryResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String nationalId
    ) {}
    public record LocationResponse(
        String departmentCode,
        String departmentName,
        String city,
        String neighborhood,
        String street,
        String streetNumber,
        String reference,
        BigDecimal latitude,
        BigDecimal longitude
    ) {}
    public record VenueResponse(
        UUID id,
        UUID ownerId,
        String name,
        String description,
        String phone,
        String whatsappPhone,
        short cancellationNoticeHours,
        VenueStatus status,
        LocationResponse location,
        String coverImageUrl,
        OwnerSummaryResponse owner
    ) {}
}
