package uy.com.fulbito.controller;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uy.com.fulbito.dto.ScheduleDtos.*;
import uy.com.fulbito.security.CurrentUserService;
import uy.com.fulbito.service.ScheduleService;
import java.time.OffsetDateTime;
import java.util.*;

@RestController
public class ScheduleController {
    private final ScheduleService service; private final CurrentUserService current;
    public ScheduleController(ScheduleService service,CurrentUserService current){this.service=service;this.current=current;}
    @GetMapping("/api/public/venues/{venueId}/opening-hours") public List<OpeningHourResponse> listHours(@PathVariable UUID venueId){return service.listHours(venueId);}
    @PostMapping("/api/owner/venues/{venueId}/opening-hours") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public OpeningHourResponse addHour(@PathVariable UUID venueId,@Valid @RequestBody OpeningHourRequest r,Authentication a){return service.addHour(venueId,current.require(a),r);}
    @DeleteMapping("/api/owner/opening-hours/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public void deleteHour(@PathVariable UUID id,Authentication a){service.deleteHour(id,current.require(a));}
    @PostMapping("/api/owner/courts/{courtId}/blocks") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public CourtBlockResponse addBlock(@PathVariable UUID courtId,@Valid @RequestBody CourtBlockRequest r,Authentication a){return service.addBlock(courtId,current.require(a),r);}
    @GetMapping("/api/owner/courts/{courtId}/blocks") @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<CourtBlockResponse> listBlocks(@PathVariable UUID courtId,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,Authentication a){return service.listBlocks(courtId,from,to,current.require(a));}
    @DeleteMapping("/api/owner/blocks/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public void deleteBlock(@PathVariable UUID id,Authentication a){service.deleteBlock(id,current.require(a));}
}
