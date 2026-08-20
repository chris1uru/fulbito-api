package uy.com.fulbito.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uy.com.fulbito.dto.ImageDtos.UploadSignatureResponse;
import uy.com.fulbito.error.ApiException;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CloudinaryImageStorage implements ImageStorage {
    private static final Logger log = LoggerFactory.getLogger(CloudinaryImageStorage.class);
    private static final long MAX_BYTES = 5_000_000;
    private static final int MAX_DIMENSION = 2_000;
    private static final Set<String> FORMATS = Set.of("jpg", "jpeg", "png", "webp");

    private final Cloudinary cloudinary;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public CloudinaryImageStorage(
        Cloudinary cloudinary,
        @Value("${app.cloudinary.cloud-name}") String cloudName,
        @Value("${app.cloudinary.api-key}") String apiKey,
        @Value("${app.cloudinary.api-secret}") String apiSecret
    ) {
        this.cloudinary = cloudinary;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    @Override
    public UploadSignatureResponse prepare(String folder) {
        long timestamp = Instant.now().getEpochSecond();
        String publicId = UUID.randomUUID().toString();
        Map<String, Object> parameters = ObjectUtils.asMap(
            "folder", folder,
            "overwrite", false,
            "public_id", publicId,
            "timestamp", timestamp
        );
        String signature = cloudinary.apiSignRequest(parameters, apiSecret, 2);
        return new UploadSignatureResponse(
            "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload",
            apiKey,
            timestamp,
            signature,
            folder,
            publicId,
            false
        );
    }

    @Override
    public StoredImage confirm(String folder, String publicId, long version, String signature) {
        if (!belongsToFolder(publicId, folder) || !validResponseSignature(publicId, version, signature)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La confirmacion de Cloudinary no es valida");
        }

        try {
            Map<?, ?> resource = cloudinary.api().resource(
                publicId,
                ObjectUtils.asMap("resource_type", "image", "type", "upload")
            );
            long bytes = number(resource.get("bytes"));
            int width = (int) number(resource.get("width"));
            int height = (int) number(resource.get("height"));
            String format = text(resource.get("format"));
            String resourceType = text(resource.get("resource_type"));
            String secureUrl = text(resource.get("secure_url"));

            if (!"image".equals(resourceType)
                || !FORMATS.contains(format)
                || bytes <= 0
                || bytes > MAX_BYTES
                || width <= 0
                || height <= 0
                || width > MAX_DIMENSION
                || height > MAX_DIMENSION
                || !secureUrl.startsWith("https://res.cloudinary.com/")) {
                deleteQuietly(publicId);
                throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "La imagen debe ser JPG, PNG o WebP, pesar hasta 5 MB y medir hasta 2000 px por lado"
                );
            }
            return new StoredImage(publicId, secureUrl);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("No se pudo verificar el recurso de Cloudinary {}", publicId, ex);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "No pudimos verificar la imagen en Cloudinary");
        }
    }

    @Override
    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank() || !publicId.startsWith("fulbito/")) return;
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.asMap("resource_type", "image", "invalidate", true)
            );
            String status = text(result.get("result"));
            if (!"ok".equals(status) && !"not found".equals(status)) {
                throw new IllegalStateException("Resultado inesperado: " + status);
            }
        } catch (Exception ex) {
            log.error("No se pudo eliminar el recurso de Cloudinary {}", publicId, ex);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "No pudimos eliminar la imagen de Cloudinary");
        }
    }

    private boolean validResponseSignature(String publicId, long version, String signature) {
        return cloudinary.verifyApiResponseSignature(publicId, Long.toString(version), signature);
    }

    private static boolean belongsToFolder(String publicId, String folder) {
        if (publicId == null || !publicId.startsWith(folder + "/")) return false;
        String suffix = publicId.substring(folder.length() + 1);
        try {
            UUID.fromString(suffix);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private void deleteQuietly(String publicId) {
        try {
            delete(publicId);
        } catch (RuntimeException ex) {
            log.warn("No se pudo limpiar el recurso rechazado de Cloudinary {}", publicId, ex);
        }
    }

    private static long number(Object value) {
        return value instanceof Number number ? number.longValue() : -1;
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }
}
