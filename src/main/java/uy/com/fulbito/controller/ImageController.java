package uy.com.fulbito.controller;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uy.com.fulbito.dto.ImageDtos.*;
import uy.com.fulbito.security.CurrentUserService;
import uy.com.fulbito.service.ImageService;
import java.util.*;
@RestController
public class ImageController {
    private final ImageService service;private final CurrentUserService current;
    public ImageController(ImageService s,CurrentUserService c){service=s;current=c;}
    @GetMapping("/api/public/venues/{id}/images")public List<ImageResponse> venueList(@PathVariable UUID id){return service.venueList(id);}
    @GetMapping("/api/public/courts/{id}/images")public List<ImageResponse> courtList(@PathVariable UUID id){return service.courtList(id);}
    @PostMapping("/api/owner/venues/{id}/images")@ResponseStatus(HttpStatus.CREATED)@PreAuthorize("hasAnyRole('OWNER','ADMIN')")public ImageResponse addVenue(@PathVariable UUID id,@Valid @RequestBody VenueImageRequest r,Authentication a){return service.addVenue(id,current.require(a),r);}
    @PostMapping("/api/owner/courts/{id}/images")@ResponseStatus(HttpStatus.CREATED)@PreAuthorize("hasAnyRole('OWNER','ADMIN')")public ImageResponse addCourt(@PathVariable UUID id,@Valid @RequestBody CourtImageRequest r,Authentication a){return service.addCourt(id,current.require(a),r);}
    @DeleteMapping("/api/owner/venue-images/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)@PreAuthorize("hasAnyRole('OWNER','ADMIN')")public void deleteVenue(@PathVariable UUID id,Authentication a){service.deleteVenue(id,current.require(a));}
    @DeleteMapping("/api/owner/court-images/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)@PreAuthorize("hasAnyRole('OWNER','ADMIN')")public void deleteCourt(@PathVariable UUID id,Authentication a){service.deleteCourt(id,current.require(a));}
}
