package uy.com.fulbito.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uy.com.fulbito.dto.AvailabilityDtos.CourtAvailabilityResponse;
import uy.com.fulbito.dto.AvailabilityDtos.VenueAvailabilitySearchResponse;
import uy.com.fulbito.service.AvailabilityService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@RestController
public class AvailabilityController {
    private final AvailabilityService availability;

    public AvailabilityController(AvailabilityService availability) {
        this.availability = availability;
    }

    @GetMapping("/api/public/courts/{courtId}/availability")
    public CourtAvailabilityResponse availability(
        @PathVariable UUID courtId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return availability.availability(courtId, date);
    }

    @GetMapping("/api/public/venues/availability")
    public VenueAvailabilitySearchResponse venueAvailability(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time
    ) {
        return availability.venueAvailability(date, time);
    }
}
