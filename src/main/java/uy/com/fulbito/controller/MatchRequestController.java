package uy.com.fulbito.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uy.com.fulbito.domain.enums.*;
import uy.com.fulbito.dto.MatchRequestDtos.*;
import uy.com.fulbito.security.CurrentUserService;
import uy.com.fulbito.service.MatchRequestService;
import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/match-requests")
@PreAuthorize("hasRole('PLAYER')")
public class MatchRequestController {
    private final MatchRequestService service;
    private final CurrentUserService current;

    public MatchRequestController(MatchRequestService service, CurrentUserService current) {
        this.service = service;
        this.current = current;
    }

    @GetMapping
    public List<MatchRequestResponse> discover(
        @RequestParam(required = false) MatchStyle style,
        @RequestParam(required = false) FootballFormat format,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
        Authentication authentication
    ) {
        return service.discover(current.require(authentication), style, format, from, to);
    }

    @GetMapping("/mine")
    public List<MatchRequestResponse> mine(Authentication authentication) {
        return service.mine(current.require(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchRequestResponse create(
        @Valid @RequestBody CreateMatchRequestRequest request,
        Authentication authentication
    ) {
        return service.create(current.require(authentication), request);
    }

    @PatchMapping("/{id}/close")
    public MatchRequestResponse close(@PathVariable UUID id, Authentication authentication) {
        return service.close(id, current.require(authentication));
    }

    @PostMapping("/{id}/interests")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchInterestResponse expressInterest(@PathVariable UUID id, Authentication authentication) {
        return service.expressInterest(id, current.require(authentication));
    }

    @GetMapping("/{id}/interests")
    public List<MatchInterestResponse> interests(@PathVariable UUID id, Authentication authentication) {
        return service.interests(id, current.require(authentication));
    }
}
