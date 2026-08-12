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
    private final VenueRepository venues; private final VenueLocationRepository locations; private final DepartmentRepository departments;
    public VenueService(VenueRepository venues, VenueLocationRepository locations, DepartmentRepository departments) {
        this.venues = venues; this.locations = locations; this.departments = departments;
    }

    @Transactional
    public VenueResponse create(AppUser owner, VenueRequest request) {
        requireOwner(owner);
        Venue venue = new Venue(); venue.setOwner(owner); apply(venue, request); venue = venues.save(venue);
        VenueLocation location = new VenueLocation(); location.setVenue(venue); apply(location, request.location()); locations.save(location);
        return response(venue, location);
    }

    @Transactional
    public VenueResponse update(UUID id, AppUser owner, VenueRequest request) {
        Venue venue = owned(id, owner); apply(venue, request);
        VenueLocation location = locations.findByVenueId(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ubicacion no encontrada"));
        apply(location, request.location()); return response(venue, location);
    }

    @Transactional(readOnly = true)
    public List<VenueResponse> mine(AppUser owner) {
        return venues.findByOwnerIdOrderByName(owner.getId()).stream().map(this::withLocation).toList();
    }

    @Transactional(readOnly = true)
    public List<VenueResponse> publicList() {
        return venues.findByStatusOrderByName(VenueStatus.ACTIVE).stream().map(this::withLocation).toList();
    }

    @Transactional(readOnly = true)
    public VenueResponse publicOne(UUID id) {
        Venue venue = venues.findById(id).filter(v -> v.getStatus() == VenueStatus.ACTIVE)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Complejo no encontrado"));
        return withLocation(venue);
    }

    public Venue owned(UUID id, AppUser owner) {
        return venues.findByIdAndOwnerId(id, owner.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Complejo no encontrado o no te pertenece"));
    }

    private void requireOwner(AppUser user) {
        if (user.getRole() != UserRole.OWNER && user.getRole() != UserRole.ADMIN)
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo un dueno puede administrar complejos");
    }
    private void apply(Venue v, VenueRequest r) {
        v.setName(r.name().trim()); v.setDescription(clean(r.description())); v.setPhone(r.phone());
        v.setWhatsappPhone(r.whatsappPhone()); v.setTimezone("America/Montevideo"); v.setStatus(r.status());
    }
    private void apply(VenueLocation l, LocationRequest r) {
        Department d = departments.findById(r.departmentCode().toUpperCase())
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Departamento invalido"));
        l.setDepartment(d); l.setCity(r.city().trim()); l.setNeighborhood(clean(r.neighborhood()));
        l.setStreet(r.street().trim()); l.setStreetNumber(clean(r.streetNumber())); l.setReference(clean(r.reference()));
        l.setLatitude(r.latitude()); l.setLongitude(r.longitude());
    }
    private VenueResponse withLocation(Venue venue) {
        VenueLocation l = locations.findByVenueId(venue.getId()).orElse(null); return response(venue, l);
    }
    private VenueResponse response(Venue v, VenueLocation l) {
        LocationResponse lr = l == null ? null : new LocationResponse(l.getDepartment().getCode(), l.getDepartment().getName(),
            l.getCity(), l.getNeighborhood(), l.getStreet(), l.getStreetNumber(), l.getReference(), l.getLatitude(), l.getLongitude());
        return new VenueResponse(v.getId(), v.getOwner().getId(), v.getName(), v.getDescription(), v.getPhone(), v.getWhatsappPhone(), v.getStatus(), lr);
    }
    private String clean(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
