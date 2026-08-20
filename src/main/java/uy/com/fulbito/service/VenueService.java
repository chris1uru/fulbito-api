package uy.com.fulbito.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.fulbito.domain.*;
import uy.com.fulbito.domain.enums.*;
import uy.com.fulbito.dto.VenueDtos.*;
import uy.com.fulbito.error.ApiException;
import uy.com.fulbito.repository.*;
import java.util.*;

@Service
public class VenueService {
    private final VenueRepository venues; private final VenueLocationRepository locations; private final DepartmentRepository departments; private final VenueImageRepository venueImages; private final AdminUserService adminUsers;
    public VenueService(VenueRepository venues, VenueLocationRepository locations, DepartmentRepository departments, VenueImageRepository venueImages, AdminUserService adminUsers) {
        this.venues = venues; this.locations = locations; this.departments = departments; this.venueImages = venueImages; this.adminUsers = adminUsers;
    }

    @Transactional
    public VenueResponse adminCreate(AdminVenueRequest request) {
        AppUser owner = adminUsers.requireActiveOwner(request.ownerId());
        Venue venue = new Venue(); venue.setOwner(owner); apply(venue, request.venueRequest(), true); venue = venues.save(venue);
        VenueLocation location = new VenueLocation(); location.setVenue(venue); apply(location, request.location()); locations.save(location);
        return response(venue, location, null);
    }

    @Transactional
    public VenueResponse updateOwned(UUID id, AppUser owner, VenueRequest request) {
        requireOwner(owner);
        Venue venue = owned(id, owner); apply(venue, request, false);
        VenueLocation location = locations.findByVenueId(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ubicacion no encontrada"));
        apply(location, request.location()); return response(venue, location, coverUrl(venue.getId()));
    }

    @Transactional
    public VenueResponse adminUpdate(UUID id, AdminVenueRequest request) {
        Venue venue = requireVenue(id);
        venue.setOwner(adminUsers.requireActiveOwner(request.ownerId()));
        apply(venue, request.venueRequest(), true);
        VenueLocation location = locations.findByVenueId(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ubicacion no encontrada"));
        apply(location, request.location());
        return response(venue, location, coverUrl(id));
    }

    @Transactional
    public VenueResponse adminAssignOwner(UUID id, AssignOwnerRequest request) {
        Venue venue = requireVenue(id);
        venue.setOwner(adminUsers.requireActiveOwner(request.ownerId()));
        return withLocation(venue, coverUrl(id));
    }

    @Transactional
    public VenueResponse adminUpdateStatus(UUID id, UpdateVenueStatusRequest request) {
        Venue venue = requireVenue(id);
        venue.setStatus(request.status());
        return withLocation(venue, coverUrl(id));
    }

    @Transactional(readOnly = true)
    public List<VenueResponse> mine(AppUser owner) {
        requireOwner(owner);
        return withCoverImages(venues.findByOwnerIdOrderByName(owner.getId()));
    }

    @Transactional(readOnly = true)
    public List<VenueResponse> adminList() {
        return withCoverImages(venues.findAllByOrderByNameAsc());
    }

    @Transactional(readOnly = true)
    public VenueResponse adminOne(UUID id) {
        Venue venue = requireVenue(id);
        return withLocation(venue, coverUrl(id));
    }

    @Transactional(readOnly = true)
    public List<VenueResponse> publicList() {
        return withCoverImages(venues.findByStatusOrderByName(VenueStatus.ACTIVE));
    }

    @Transactional(readOnly = true)
    public VenueResponse publicOne(UUID id) {
        Venue venue = venues.findById(id).filter(v -> v.getStatus() == VenueStatus.ACTIVE)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Complejo no encontrado"));
        return withLocation(venue, coverUrl(venue.getId()));
    }

    public Venue owned(UUID id, AppUser owner) {
        if (owner.getRole() == UserRole.ADMIN) return requireVenue(id);
        return venues.findByIdAndOwnerId(id, owner.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Complejo no encontrado o no te pertenece"));
    }

    private void requireOwner(AppUser user) {
        if (user.getRole() != UserRole.OWNER)
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo un dueno puede administrar complejos");
    }
    private Venue requireVenue(UUID id) {
        return venues.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Complejo no encontrado"));
    }
    private void apply(Venue v, VenueRequest r, boolean includeStatus) {
        v.setName(r.name().trim()); v.setDescription(clean(r.description())); v.setPhone(r.phone());
        v.setWhatsappPhone(r.whatsappPhone()); v.setTimezone("America/Montevideo");
        if (r.cancellationNoticeHours() != null) v.setCancellationNoticeHours(r.cancellationNoticeHours());
        if (includeStatus) v.setStatus(r.status());
    }
    private void apply(VenueLocation l, LocationRequest r) {
        Department d = departments.findById(r.departmentCode().toUpperCase())
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Departamento invalido"));
        l.setDepartment(d); l.setCity(r.city().trim()); l.setNeighborhood(clean(r.neighborhood()));
        l.setStreet(r.street().trim()); l.setStreetNumber(clean(r.streetNumber())); l.setReference(clean(r.reference()));
        l.setLatitude(r.latitude()); l.setLongitude(r.longitude());
    }
    private List<VenueResponse> withCoverImages(List<Venue> venues) {
        Map<UUID, String> coverUrls = new HashMap<>();
        if (!venues.isEmpty()) {
            venueImages.findByVenueIdInAndCoverTrue(venues.stream().map(Venue::getId).toList())
                .forEach(image -> coverUrls.put(image.getVenue().getId(), image.getUrl()));
        }
        return venues.stream().map(venue -> withLocation(venue, coverUrls.get(venue.getId()))).toList();
    }
    private String coverUrl(UUID venueId) {
        return venueImages.findByVenueIdAndCoverTrue(venueId).map(VenueImage::getUrl).orElse(null);
    }
    private VenueResponse withLocation(Venue venue, String coverImageUrl) {
        VenueLocation l = locations.findByVenueId(venue.getId()).orElse(null); return response(venue, l, coverImageUrl);
    }
    private VenueResponse response(Venue v, VenueLocation l, String coverImageUrl) {
        LocationResponse lr = l == null ? null : new LocationResponse(l.getDepartment().getCode(), l.getDepartment().getName(),
            l.getCity(), l.getNeighborhood(), l.getStreet(), l.getStreetNumber(), l.getReference(), l.getLatitude(), l.getLongitude());
        AppUser owner = v.getOwner();
        OwnerSummaryResponse ownerResponse = new OwnerSummaryResponse(
            owner.getId(), owner.getEmail(), owner.getFirstName(), owner.getLastName(), owner.getNationalId()
        );
        return new VenueResponse(v.getId(), owner.getId(), v.getName(), v.getDescription(), v.getPhone(), v.getWhatsappPhone(), v.getCancellationNoticeHours(), v.getStatus(), lr, coverImageUrl, ownerResponse);
    }
    private String clean(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
