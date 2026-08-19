package uy.com.fulbito.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uy.com.fulbito.dto.VenueDtos.*;
import uy.com.fulbito.security.CurrentUserService;
import uy.com.fulbito.service.VenueService;
import java.util.*;

@RestController
@RequestMapping("/api/owner/venues")
@PreAuthorize("hasRole('OWNER')")
public class VenueController {
    private final VenueService service;
    private final CurrentUserService current;

    public VenueController(VenueService service, CurrentUserService current) {
        this.service = service;
        this.current = current;
    }

    @GetMapping
    public List<VenueResponse> mine(Authentication auth) { return service.mine(current.require(auth)); }

    @PutMapping("/{id}")
    public VenueResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody VenueRequest request,
        Authentication auth
    ) {
        return service.updateOwned(id, current.require(auth), request);
    }
}
