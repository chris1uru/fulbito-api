package uy.com.fulbito.dto;
import jakarta.validation.constraints.*;
import java.util.UUID;
public final class ImageDtos {
    private ImageDtos() {}
    public record UploadSignatureResponse(
        String uploadUrl,
        String apiKey,
        long timestamp,
        String signature,
        String folder,
        String publicId,
        boolean overwrite
    ) {}
    public record VenueImageRequest(
        @NotBlank @Size(max=300) String publicId,
        @Positive long version,
        @NotBlank @Size(max=200) String signature,
        @Min(0) short sortOrder,
        boolean cover
    ) {}
    public record CourtImageRequest(
        @NotBlank @Size(max=300) String publicId,
        @Positive long version,
        @NotBlank @Size(max=200) String signature,
        @Min(0) short sortOrder
    ) {}
    public record ImageOrderRequest(@Min(0) short sortOrder) {}
    public record ImageResponse(UUID id,String url,String storageKey,short sortOrder,boolean cover) {}
}
