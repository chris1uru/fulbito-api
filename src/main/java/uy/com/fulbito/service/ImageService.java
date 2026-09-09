package uy.com.fulbito.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.fulbito.domain.AppUser;
import uy.com.fulbito.domain.Court;
import uy.com.fulbito.domain.CourtImage;
import uy.com.fulbito.domain.Venue;
import uy.com.fulbito.domain.VenueImage;
import uy.com.fulbito.dto.ImageDtos.CourtImageRequest;
import uy.com.fulbito.dto.ImageDtos.ImageResponse;
import uy.com.fulbito.dto.ImageDtos.UploadSignatureResponse;
import uy.com.fulbito.dto.ImageDtos.VenueImageRequest;
import uy.com.fulbito.error.ApiException;
import uy.com.fulbito.repository.CourtImageRepository;
import uy.com.fulbito.repository.VenueImageRepository;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@Service
public class ImageService {
    private static final long MAX_VENUE_IMAGES = 8;
    private static final long MAX_COURT_IMAGES = 5;

    private final VenueImageRepository venueImages;
    private final CourtImageRepository courtImages;
    private final VenueService venues;
    private final CourtService courts;
    private final ImageStorage storage;

    public ImageService(
        VenueImageRepository venueImages,
        CourtImageRepository courtImages,
        VenueService venues,
        CourtService courts,
        ImageStorage storage
    ) {
        this.venueImages = venueImages;
        this.courtImages = courtImages;
        this.venues = venues;
        this.courts = courts;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public UploadSignatureResponse prepareVenue(UUID venueId, AppUser actor) {
        venues.owned(venueId, actor);
        requireRoom(venueImages.countByVenueId(venueId), MAX_VENUE_IMAGES, "complejo");
        return storage.prepare(venueFolder(venueId));
    }

    @Transactional(readOnly = true)
    public UploadSignatureResponse prepareCourt(UUID courtId, AppUser actor) {
        courts.owned(courtId, actor);
        requireRoom(courtImages.countByCourtId(courtId), MAX_COURT_IMAGES, "cancha");
        return storage.prepare(courtFolder(courtId));
    }

    @Transactional
    public ImageResponse addVenue(UUID venueId, AppUser actor, VenueImageRequest request) {
        Venue venue = venues.owned(venueId, actor);
        ImageStorage.StoredImage uploaded = storage.confirm(
            venueFolder(venueId), request.publicId(), request.version(), request.signature()
        );
        if (venueImages.existsByStorageKey(uploaded.publicId())) {
            throw new ApiException(HttpStatus.CONFLICT, "La imagen ya fue registrada");
        }
        if (venueImages.countByVenueId(venueId) >= MAX_VENUE_IMAGES) {
            storage.delete(uploaded.publicId());
            throw imageLimit("complejo", MAX_VENUE_IMAGES);
        }

        try {
            if (request.cover()) clearCover(venueId);
            VenueImage image = new VenueImage();
            image.setVenue(venue);
            image.setUrl(uploaded.secureUrl());
            image.setStorageKey(uploaded.publicId());
            image.setSortOrder(request.sortOrder());
            image.setCover(request.cover());
            return response(venueImages.saveAndFlush(image));
        } catch (RuntimeException ex) {
            storage.delete(uploaded.publicId());
            throw ex;
        }
    }

    @Transactional
    public ImageResponse addCourt(UUID courtId, AppUser actor, CourtImageRequest request) {
        Court court = courts.owned(courtId, actor);
        ImageStorage.StoredImage uploaded = storage.confirm(
            courtFolder(courtId), request.publicId(), request.version(), request.signature()
        );
        if (courtImages.existsByStorageKey(uploaded.publicId())) {
            throw new ApiException(HttpStatus.CONFLICT, "La imagen ya fue registrada");
        }
        if (courtImages.countByCourtId(courtId) >= MAX_COURT_IMAGES) {
            storage.delete(uploaded.publicId());
            throw imageLimit("cancha", MAX_COURT_IMAGES);
        }

        try {
            CourtImage image = new CourtImage();
            image.setCourt(court);
            image.setUrl(uploaded.secureUrl());
            image.setStorageKey(uploaded.publicId());
            image.setSortOrder(request.sortOrder());
            return response(courtImages.saveAndFlush(image));
        } catch (RuntimeException ex) {
            storage.delete(uploaded.publicId());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> venueList(UUID id) {
        return venueImages.findByVenueIdOrderBySortOrder(id).stream().map(ImageService::response).toList();
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> courtList(UUID id) {
        return courtImages.findByCourtIdOrderBySortOrder(id).stream().map(ImageService::response).toList();
    }

    @Transactional
    public ImageResponse setVenueCover(UUID id, AppUser actor) {
        VenueImage image = venueImages.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Imagen no encontrada"));
        venues.owned(image.getVenue().getId(), actor);
        if (image.isCover()) return response(image);
        clearCover(image.getVenue().getId());
        image.setCover(true);
        return response(venueImages.saveAndFlush(image));
    }

    @Transactional
    public List<ImageResponse> reorderVenue(UUID id, short requestedOrder, AppUser actor) {
        VenueImage image = venueImages.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Imagen no encontrada"));
        UUID venueId = image.getVenue().getId();
        venues.owned(venueId, actor);
        List<VenueImage> ordered = new ArrayList<>(venueImages.findByVenueIdOrderBySortOrder(venueId));
        ordered.removeIf(value -> value.getId().equals(id));
        ordered.add(Math.min(requestedOrder, (short) ordered.size()), image);
        for (short index = 0; index < ordered.size(); index++) ordered.get(index).setSortOrder(index);
        return venueImages.saveAllAndFlush(ordered).stream().map(ImageService::response).toList();
    }

    @Transactional
    public List<ImageResponse> reorderCourt(UUID id, short requestedOrder, AppUser actor) {
        CourtImage image = courtImages.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Imagen no encontrada"));
        UUID courtId = image.getCourt().getId();
        courts.owned(courtId, actor);
        List<CourtImage> ordered = new ArrayList<>(courtImages.findByCourtIdOrderBySortOrder(courtId));
        ordered.removeIf(value -> value.getId().equals(id));
        ordered.add(Math.min(requestedOrder, (short) ordered.size()), image);
        for (short index = 0; index < ordered.size(); index++) ordered.get(index).setSortOrder(index);
        return courtImages.saveAllAndFlush(ordered).stream().map(ImageService::response).toList();
    }

    @Transactional
    public void deleteVenue(UUID id, AppUser actor) {
        VenueImage image = venueImages.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Imagen no encontrada"));
        UUID venueId = image.getVenue().getId();
        venues.owned(venueId, actor);
        storage.delete(image.getStorageKey());
        venueImages.delete(image);
        venueImages.flush();
        if (image.isCover()) {
            venueImages.findFirstByVenueIdOrderBySortOrderAsc(venueId).ifPresent(next -> {
                next.setCover(true);
                venueImages.saveAndFlush(next);
            });
        }
    }

    @Transactional
    public void deleteCourt(UUID id, AppUser actor) {
        CourtImage image = courtImages.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Imagen no encontrada"));
        courts.owned(image.getCourt().getId(), actor);
        storage.delete(image.getStorageKey());
        courtImages.delete(image);
    }

    private void clearCover(UUID venueId) {
        venueImages.findByVenueIdAndCoverTrue(venueId).ifPresent(current -> {
            current.setCover(false);
            venueImages.saveAndFlush(current);
        });
    }

    private static void requireRoom(long current, long maximum, String target) {
        if (current >= maximum) throw imageLimit(target, maximum);
    }

    private static ApiException imageLimit(String target, long maximum) {
        String article = "complejo".equals(target) ? "El" : "La";
        return new ApiException(
            HttpStatus.CONFLICT,
            article + " " + target + " ya alcanzo el maximo de " + maximum + " imagenes"
        );
    }

    private static String venueFolder(UUID venueId) {
        return "fulbito/venues/" + venueId;
    }

    private static String courtFolder(UUID courtId) {
        return "fulbito/courts/" + courtId;
    }

    private static ImageResponse response(VenueImage image) {
        return new ImageResponse(
            image.getId(), image.getUrl(), image.getStorageKey(), image.getSortOrder(), image.isCover()
        );
    }

    private static ImageResponse response(CourtImage image) {
        return new ImageResponse(
            image.getId(), image.getUrl(), image.getStorageKey(), image.getSortOrder(), false
        );
    }
}
