package uy.com.fulbito.controller;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import uy.com.fulbito.dto.CourtDtos;
import uy.com.fulbito.dto.CourtDtos.*;
import uy.com.fulbito.security.CurrentUserService;
import uy.com.fulbito.service.CourtService;
import java.util.*;
@RestController
public class CourtController {
    private final CourtService service; private final CurrentUserService current;

    public CourtController(CourtService service, CurrentUserService current) { this.service=service; this.current=current; }

    @GetMapping("/api/public/venues/{venueId}/courts") public List<CourtResponse> list(@PathVariable UUID venueId) { return service.list(venueId); }

    @GetMapping("/api/public/courts/{id}")
    public CourtDtos getCourt(@PathVariable Long id) {
        return 
    }

    @PostMapping("/api/owner/venues/{venueId}/courts") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('OWNER','ADMIN')")

    public CourtResponse create(@PathVariable UUID venueId, @Valid @RequestBody CourtRequest r, Authentication a) { return service.create(venueId,current.require(a),r); }

    @PutMapping("/api/owner/courts/{id}") @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public CourtResponse update(@PathVariable UUID id, @Valid @RequestBody CourtRequest r, Authentication a) { return service.update(id,current.require(a),r); }
}
