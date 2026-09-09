package uy.com.fulbito.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uy.com.fulbito.dto.ImageDtos.CourtImageRequest;
import uy.com.fulbito.dto.ImageDtos.ImageResponse;
import uy.com.fulbito.dto.ImageDtos.ImageOrderRequest;
import uy.com.fulbito.dto.ImageDtos.UploadSignatureResponse;
import uy.com.fulbito.dto.ImageDtos.VenueImageRequest;
import uy.com.fulbito.security.CurrentUserService;
import uy.com.fulbito.service.ImageService;
import uy.com.fulbito.security.RateLimitService;
import uy.com.fulbito.domain.AppUser;
import java.time.Duration;

import java.util.List;
import java.util.UUID;

@RestController
public class ImageController {
    private final ImageService service;
    private final CurrentUserService current;
    private final RateLimitService rateLimits;

    public ImageController(ImageService service, CurrentUserService current, RateLimitService rateLimits) {
        this.service = service;
        this.current = current;
        this.rateLimits = rateLimits;
    }

    @GetMapping("/api/public/courts/{id}/images")
    public List<ImageResponse> courtList(@PathVariable UUID id) {
        return service.courtList(id);
    }

    @GetMapping("/api/public/venues/{id}/images")
    public List<ImageResponse> venueList(@PathVariable UUID id) {
        return service.venueList(id);
    }

    @PostMapping("/api/owner/venues/{id}/images/upload-signature")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public UploadSignatureResponse prepareVenue(@PathVariable UUID id, Authentication auth) {
        AppUser actor = current.require(auth);
        rateLimits.check("image-upload", actor.getId().toString(), 30, Duration.ofHours(1));
        return service.prepareVenue(id, actor);
    }

    @PostMapping("/api/owner/courts/{id}/images/upload-signature")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public UploadSignatureResponse prepareCourt(@PathVariable UUID id, Authentication auth) {
        AppUser actor = current.require(auth);
        rateLimits.check("image-upload", actor.getId().toString(), 30, Duration.ofHours(1));
        return service.prepareCourt(id, actor);
    }

    @PostMapping("/api/owner/venues/{id}/images")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ImageResponse addVenue(
        @PathVariable UUID id,
        @Valid @RequestBody VenueImageRequest request,
        Authentication auth
    ) {
        return service.addVenue(id, current.require(auth), request);
    }

    @PostMapping("/api/owner/courts/{id}/images")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ImageResponse addCourt(
        @PathVariable UUID id,
        @Valid @RequestBody CourtImageRequest request,
        Authentication auth
    ) {
        return service.addCourt(id, current.require(auth), request);
    }

    @PatchMapping("/api/owner/venue-images/{id}/cover")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ImageResponse setVenueCover(@PathVariable UUID id, Authentication auth) {
        return service.setVenueCover(id, current.require(auth));
    }

    @PatchMapping("/api/owner/venue-images/{id}/order")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<ImageResponse> reorderVenue(
        @PathVariable UUID id,
        @Valid @RequestBody ImageOrderRequest request,
        Authentication auth
    ) {
        return service.reorderVenue(id, request.sortOrder(), current.require(auth));
    }

    @PatchMapping("/api/owner/court-images/{id}/order")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<ImageResponse> reorderCourt(
        @PathVariable UUID id,
        @Valid @RequestBody ImageOrderRequest request,
        Authentication auth
    ) {
        return service.reorderCourt(id, request.sortOrder(), current.require(auth));
    }

    @DeleteMapping("/api/owner/venue-images/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public void deleteVenue(@PathVariable UUID id, Authentication auth) {
        service.deleteVenue(id, current.require(auth));
    }

    @DeleteMapping("/api/owner/court-images/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public void deleteCourt(@PathVariable UUID id, Authentication auth) {
        service.deleteCourt(id, current.require(auth));
    }
}
