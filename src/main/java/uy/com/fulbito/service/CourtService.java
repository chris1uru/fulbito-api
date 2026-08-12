package uy.com.fulbito.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.fulbito.domain.*;
import uy.com.fulbito.dto.CourtDtos.*;
import uy.com.fulbito.error.ApiException;
import uy.com.fulbito.repository.CourtRepository;
import java.util.*;

@Service
public class CourtService {
    private final CourtRepository courts; private final VenueService venueService;
    public CourtService(CourtRepository courts, VenueService venueService) { this.courts = courts; this.venueService = venueService; }
    @Transactional public CourtResponse create(UUID venueId, AppUser owner, CourtRequest request) {
        Court court = new Court(); court.setVenue(venueService.owned(venueId, owner)); apply(court, request); return response(courts.save(court));
    }
    @Transactional public CourtResponse update(UUID id, AppUser owner, CourtRequest request) {
        Court court = owned(id, owner); apply(court, request); return response(court);
    }
    @Transactional(readOnly = true) public List<CourtResponse> list(UUID venueId) {
        return courts.findByVenueIdAndActiveTrueAndVenueStatusOrderByName(venueId, uy.com.fulbito.domain.enums.VenueStatus.ACTIVE).stream().map(CourtService::response).toList();
    }
    public Court owned(UUID id, AppUser owner) {
        return courts.findByIdAndVenueOwnerId(id, owner.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cancha no encontrada o no te pertenece"));
    }
    public Court get(UUID id) { return courts.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cancha no encontrada")); }
    private void apply(Court c, CourtRequest r) {
        if (r.slotMinutes() % 15 != 0) throw new ApiException(HttpStatus.BAD_REQUEST, "La duracion debe ser multiplo de 15 minutos");
        c.setName(r.name().trim()); c.setFootballFormat(r.footballFormat()); c.setSurface(r.surface()); c.setCovered(r.covered());
        c.setPricePerSlot(r.pricePerSlot()); c.setCurrency("UYU"); c.setSlotMinutes(r.slotMinutes()); c.setActive(r.active());
    }
    public static CourtResponse response(Court c) { return new CourtResponse(c.getId(), c.getVenue().getId(), c.getName(), c.getFootballFormat(), c.getSurface(), c.isCovered(), c.getPricePerSlot(), c.getCurrency(), c.getSlotMinutes(), c.isActive()); }
}
