package uy.com.fulbito.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.com.fulbito.domain.AppUser;
import uy.com.fulbito.domain.Venue;
import uy.com.fulbito.domain.VenueImage;
import uy.com.fulbito.dto.ImageDtos.UploadSignatureResponse;
import uy.com.fulbito.dto.ImageDtos.VenueImageRequest;
import uy.com.fulbito.error.ApiException;
import uy.com.fulbito.repository.CourtImageRepository;
import uy.com.fulbito.repository.VenueImageRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {
    @Mock private VenueImageRepository venueImages;
    @Mock private CourtImageRepository courtImages;
    @Mock private VenueService venues;
    @Mock private CourtService courts;
    @Mock private ImageStorage storage;

    private ImageService service;

    @BeforeEach
    void setUp() {
        service = new ImageService(venueImages, courtImages, venues, courts, storage);
    }

    @Test
    void venueUploadIsRejectedAtEightImagesBeforeSigning() {
        UUID venueId = UUID.randomUUID();
        AppUser actor = mock(AppUser.class);
        when(venueImages.countByVenueId(venueId)).thenReturn(8L);

        ApiException error = assertThrows(
            ApiException.class,
            () -> service.prepareVenue(venueId, actor)
        );

        assertEquals("El complejo ya alcanzo el maximo de 8 imagenes", error.getMessage());
        verify(venues).owned(venueId, actor);
        verifyNoInteractions(storage);
    }

    @Test
    void newCoverUnsetsPreviousCoverAndStoresVerifiedCloudinaryData() {
        UUID venueId = UUID.randomUUID();
        AppUser actor = mock(AppUser.class);
        Venue venue = mock(Venue.class);
        VenueImage previousCover = mock(VenueImage.class);
        String publicId = "fulbito/venues/" + venueId + "/" + UUID.randomUUID();
        String secureUrl = "https://res.cloudinary.com/demo/image/upload/v1/photo.jpg";

        when(venues.owned(venueId, actor)).thenReturn(venue);
        when(storage.confirm(any(), eq(publicId), eq(42L), eq("response-signature")))
            .thenReturn(new ImageStorage.StoredImage(publicId, secureUrl));
        when(venueImages.existsByStorageKey(publicId)).thenReturn(false);
        when(venueImages.countByVenueId(venueId)).thenReturn(1L);
        when(venueImages.findByVenueIdAndCoverTrue(venueId)).thenReturn(Optional.of(previousCover));
        when(venueImages.saveAndFlush(any(VenueImage.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.addVenue(
            venueId,
            actor,
            new VenueImageRequest(publicId, 42L, "response-signature", (short) 2, true)
        );

        verify(previousCover).setCover(false);
        assertEquals(secureUrl, result.url());
        assertEquals(2, result.sortOrder());
        assertTrue(result.cover());
    }

    @Test
    void deletingImageAlsoDeletesCloudinaryAsset() {
        UUID imageId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        AppUser actor = mock(AppUser.class);
        Venue venue = mock(Venue.class);
        VenueImage image = mock(VenueImage.class);
        String publicId = "fulbito/venues/" + venueId + "/" + UUID.randomUUID();

        when(image.getVenue()).thenReturn(venue);
        when(venue.getId()).thenReturn(venueId);
        when(image.getStorageKey()).thenReturn(publicId);
        when(venueImages.findById(imageId)).thenReturn(Optional.of(image));

        service.deleteVenue(imageId, actor);

        verify(venues).owned(venueId, actor);
        verify(storage).delete(publicId);
        verify(venueImages).delete(image);
    }

    @Test
    void movingVenueImageNormalizesEverySortOrderAtomically() {
        UUID venueId = UUID.randomUUID();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        UUID thirdId = UUID.randomUUID();
        AppUser actor = mock(AppUser.class);
        Venue venue = mock(Venue.class);
        VenueImage first = mock(VenueImage.class);
        VenueImage second = mock(VenueImage.class);
        VenueImage third = mock(VenueImage.class);

        when(first.getId()).thenReturn(firstId);
        when(second.getId()).thenReturn(secondId);
        when(third.getId()).thenReturn(thirdId);
        when(third.getVenue()).thenReturn(venue);
        when(venue.getId()).thenReturn(venueId);
        when(venueImages.findById(thirdId)).thenReturn(Optional.of(third));
        when(venueImages.findByVenueIdOrderBySortOrder(venueId))
            .thenReturn(List.of(first, second, third));
        when(venueImages.saveAllAndFlush(anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        service.reorderVenue(thirdId, (short) 0, actor);

        verify(venues).owned(venueId, actor);
        verify(third).setSortOrder((short) 0);
        verify(first).setSortOrder((short) 1);
        verify(second).setSortOrder((short) 2);
    }
}
