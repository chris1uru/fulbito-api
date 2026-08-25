package uy.com.fulbito.controller;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uy.com.fulbito.dto.ReservationDtos.*;
import uy.com.fulbito.security.CurrentUserService;
import uy.com.fulbito.service.ReservationService;
import java.time.OffsetDateTime;
import java.util.*;

@RestController @RequestMapping("/api/reservations")
public class ReservationController {
    private final ReservationService service;private final CurrentUserService current;

    public ReservationController(ReservationService service,CurrentUserService current){this.service=service;this.current=current;}

    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ReservationResponse create(@Valid @RequestBody ReservationRequest r,Authentication a){return service.create(current.require(a),r);}

    @GetMapping("/mine") @PreAuthorize("hasRole('PLAYER')") public List<ReservationResponse> mine(
        @RequestParam(defaultValue = "100") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int limit,
        Authentication a
    ){return service.mine(current.require(a), limit);}

    @GetMapping("/{id}") public ReservationResponse one(@PathVariable UUID id,Authentication a){return service.one(id,current.require(a));}

    @GetMapping("/owner-agenda") @PreAuthorize("hasAnyRole('OWNER','ADMIN')") public List<ReservationResponse> agenda(
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
        @RequestParam(required=false) UUID venueId,
        Authentication a
    ){return service.ownerAgenda(current.require(a),from,to,venueId);}

    @PatchMapping("/{id}/mark-paid") @PreAuthorize("hasAnyRole('OWNER','ADMIN')") public ReservationResponse paid(@PathVariable UUID id,Authentication a){return service.markPaid(id,current.require(a));}

    @PatchMapping("/{id}/cancel-owner") @PreAuthorize("hasAnyRole('OWNER','ADMIN')") public ReservationResponse cancelOwner(@PathVariable UUID id,Authentication a){return service.cancelByOwner(id,current.require(a));}
    
    @PatchMapping("/{id}/cancel-player") @PreAuthorize("hasRole('PLAYER')") public ReservationResponse cancelPlayer(@PathVariable UUID id,Authentication a){return service.cancelByPlayer(id,current.require(a));}
}
