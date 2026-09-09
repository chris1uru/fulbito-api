package uy.com.fulbito.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Api;
import com.cloudinary.Uploader;
import com.cloudinary.api.ApiResponse;
import org.junit.jupiter.api.Test;
import uy.com.fulbito.error.ApiException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CloudinaryImageStorageTest {
    @Test
    void doesNotAttemptToDeleteAResourceOutsideTheFulbitoNamespace() {
        Cloudinary cloudinary = mock(Cloudinary.class);
        CloudinaryImageStorage storage = new CloudinaryImageStorage(cloudinary, "cloud", "key", "secret");

        storage.delete("another-app/asset");

        verifyNoInteractions(cloudinary);
    }

    @Test
    void rejectsAConfirmationWhosePublicIdIsNotTheSignedFolderAndUuid() {
        Cloudinary cloudinary = mock(Cloudinary.class);
        CloudinaryImageStorage storage = new CloudinaryImageStorage(cloudinary, "cloud", "key", "secret");

        ApiException error = assertThrows(ApiException.class, () -> storage.confirm(
            "fulbito/venues/venue-id", "fulbito/venues/venue-id/not-a-uuid", 1L, "signature"
        ));

        assertEquals("La confirmacion de Cloudinary no es valida", error.getMessage());
        verifyNoInteractions(cloudinary);
    }

    @Test
    void removesInvalidCloudinaryAssetsBeforeReportingValidationError() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Api api = mock(Api.class);
        ApiResponse resource = mock(ApiResponse.class);
        Uploader uploader = mock(Uploader.class);
        String id = "fulbito/venues/venue-id/00000000-0000-0000-0000-000000000001";
        when(cloudinary.verifyApiResponseSignature(id, "1", "signature")).thenReturn(true);
        when(cloudinary.api()).thenReturn(api);
        when(api.resource(eq(id), anyMap())).thenReturn(resource);
        when(resource.get("bytes")).thenReturn(5_000_001L);
        when(resource.get("width")).thenReturn(1600);
        when(resource.get("height")).thenReturn(1600);
        when(resource.get("format")).thenReturn("jpg");
        when(resource.get("resource_type")).thenReturn("image");
        when(resource.get("secure_url")).thenReturn("https://res.cloudinary.com/cloud/image/upload/x.jpg");
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq(id), anyMap())).thenReturn(Map.of("result", "ok"));
        CloudinaryImageStorage storage = new CloudinaryImageStorage(cloudinary, "cloud", "key", "secret");

        ApiException error = assertThrows(ApiException.class, () -> storage.confirm(
            "fulbito/venues/venue-id", id, 1L, "signature"
        ));

        assertEquals("La imagen debe ser JPG, PNG o WebP, pesar hasta 5 MB y medir hasta 2000 px por lado", error.getMessage());
        verify(uploader).destroy(eq(id), anyMap());
    }
}
