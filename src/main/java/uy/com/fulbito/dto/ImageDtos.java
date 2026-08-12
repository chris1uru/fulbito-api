package uy.com.fulbito.dto;
import jakarta.validation.constraints.*;
import java.util.UUID;
public final class ImageDtos {
    private ImageDtos() {}
    public record VenueImageRequest(@NotBlank String url,String storageKey,@Min(0) short sortOrder,boolean cover) {}
    public record CourtImageRequest(@NotBlank String url,String storageKey,@Min(0) short sortOrder) {}
    public record ImageResponse(UUID id,String url,String storageKey,short sortOrder,boolean cover) {}
}
