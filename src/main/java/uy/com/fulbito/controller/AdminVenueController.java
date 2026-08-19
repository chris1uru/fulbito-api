package uy.com.fulbito.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uy.com.fulbito.dto.VenueDtos.*;
import uy.com.fulbito.service.VenueService;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/venues")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVenueController {
    private final VenueService service;

    public AdminVenueController(VenueService service) { this.service = service; }

    @GetMapping
    public List<VenueResponse> list() { return service.adminList(); }

    @GetMapping("/{id}")
    public VenueResponse one(@PathVariable UUID id) { return service.adminOne(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VenueResponse create(@Valid @RequestBody AdminVenueRequest request) {
        return service.adminCreate(request);
    }

    @PutMapping("/{id}")
    public VenueResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody AdminVenueRequest request
    ) {
        return service.adminUpdate(id, request);
    }

    @PatchMapping("/{id}/owner")
    public VenueResponse assignOwner(
        @PathVariable UUID id,
        @Valid @RequestBody AssignOwnerRequest request
    ) {
        return service.adminAssignOwner(id, request);
    }

    @PatchMapping("/{id}/status")
    public VenueResponse updateStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateVenueStatusRequest request
    ) {
        return service.adminUpdateStatus(id, request);
    }
}
