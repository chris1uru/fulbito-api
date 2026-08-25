package uy.com.fulbito.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.fulbito.domain.*;
import uy.com.fulbito.dto.CourtDtos.*;
import uy.com.fulbito.error.ApiException;
import uy.com.fulbito.repository.CourtRepository;
import uy.com.fulbito.repository.CourtImageRepository;
import java.util.*;

@Service
public class CourtService {

    private final CourtRepository courts; private final CourtImageRepository images; private final VenueService venueService;

    public CourtService(CourtRepository courts, CourtImageRepository images, VenueService venueService) {
        this.courts = courts; this.images = images; this.venueService = venueService;
    }

    @Transactional public CourtResponse create(UUID venueId, AppUser owner, CourtRequest request) {
        Court court = new Court(); court.setVenue(venueService.owned(venueId, owner)); apply(court, request); return response(courts.save(court), null);
    }

    @Transactional public CourtResponse update(UUID id, AppUser owner, CourtRequest request) {
        Court court = owned(id, owner); apply(court, request); return response(court, coverUrl(id));
    }

    @Transactional(readOnly = true) public List<CourtResponse> list(UUID venueId) {
        return responses(courts.findByVenueIdAndActiveTrueAndVenueStatusOrderByName(venueId, uy.com.fulbito.domain.enums.VenueStatus.ACTIVE));
    }

    @Transactional(readOnly = true)
    public List<Court> publicActive() {
        return courts.findPublicActive(uy.com.fulbito.domain.enums.VenueStatus.ACTIVE);
    }

    @Transactional(readOnly = true) public List<CourtResponse> listManaged(UUID venueId, AppUser actor) {
        venueService.owned(venueId, actor);
        return responses(courts.findByVenueIdOrderByName(venueId));
    }
    
    public Court owned(UUID id, AppUser owner) {
        if (owner.getRole() == uy.com.fulbito.domain.enums.UserRole.ADMIN) return get(id);
        return courts.findByIdAndVenueOwnerId(id, owner.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cancha no encontrada o no te pertenece"));
    }
    public Court publicActive(UUID id) {
        Court court = get(id);
        if (!court.isActive() || court.getVenue().getStatus() != uy.com.fulbito.domain.enums.VenueStatus.ACTIVE)
            throw new ApiException(HttpStatus.NOT_FOUND, "Cancha no encontrada");
        return court;
    }
    public Court get(UUID id) { return courts.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cancha no encontrada")); }
    private void apply(Court c, CourtRequest r) {
        if (r.slotMinutes() % 15 != 0) throw new ApiException(HttpStatus.BAD_REQUEST, "La duracion debe ser multiplo de 15 minutos");
        c.setName(r.name().trim()); c.setFootballFormat(r.footballFormat()); c.setSurface(r.surface()); c.setCovered(r.covered());
        c.setPricePerSlot(r.pricePerSlot()); c.setCurrency("UYU"); c.setSlotMinutes(r.slotMinutes()); c.setActive(r.active());
    }
    private List<CourtResponse> responses(List<Court> values) {
        if (values.isEmpty()) return List.of();
        Map<UUID, String> covers = new HashMap<>();
        images.findByCourtIdInOrderByCourtIdAscSortOrderAsc(values.stream().map(Court::getId).toList())
            .forEach(image -> covers.putIfAbsent(image.getCourt().getId(), image.getUrl()));
        return values.stream().map(court -> response(court, covers.get(court.getId()))).toList();
    }
    private String coverUrl(UUID courtId) {
        return images.findByCourtIdOrderBySortOrder(courtId).stream().findFirst().map(CourtImage::getUrl).orElse(null);
    }
    public static CourtResponse response(Court c, String coverImageUrl) {
        return new CourtResponse(c.getId(), c.getVenue().getId(), c.getName(), c.getFootballFormat(), c.getSurface(), c.isCovered(), c.getPricePerSlot(), c.getCurrency(), c.getSlotMinutes(), c.isActive(), coverImageUrl);
    }
}
