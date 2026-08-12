package uy.com.fulbito.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.fulbito.domain.*;
import uy.com.fulbito.dto.ScheduleDtos.*;
import uy.com.fulbito.error.ApiException;
import uy.com.fulbito.repository.*;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ScheduleService {
    private final OpeningHourRepository hours; private final CourtBlockRepository blocks;
    private final VenueService venues; private final CourtService courts;
    public ScheduleService(OpeningHourRepository hours, CourtBlockRepository blocks, VenueService venues, CourtService courts) {
        this.hours=hours; this.blocks=blocks; this.venues=venues; this.courts=courts;
    }
    @Transactional public OpeningHourResponse addHour(UUID venueId, AppUser owner, OpeningHourRequest r) {
        if (!r.closesAt().isAfter(r.opensAt())) throw new ApiException(HttpStatus.BAD_REQUEST,"La hora de cierre debe ser posterior a la apertura");
        OpeningHour h=new OpeningHour(); h.setVenue(venues.owned(venueId,owner)); h.setDayOfWeek(r.dayOfWeek()); h.setOpensAt(r.opensAt()); h.setClosesAt(r.closesAt());
        return hour(hours.save(h));
    }
    @Transactional(readOnly=true) public List<OpeningHourResponse> listHours(UUID venueId) { return hours.findByVenueIdOrderByDayOfWeekAscOpensAtAsc(venueId).stream().map(ScheduleService::hour).toList(); }
    @Transactional public void deleteHour(UUID id, AppUser owner) {
        OpeningHour h=hours.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Horario no encontrado"));
        venues.owned(h.getVenue().getId(),owner); hours.delete(h);
    }
    @Transactional public CourtBlockResponse addBlock(UUID courtId, AppUser owner, CourtBlockRequest r) {
        if (!r.endsAt().isAfter(r.startsAt())) throw new ApiException(HttpStatus.BAD_REQUEST,"El fin debe ser posterior al inicio");
        CourtBlock b=new CourtBlock(); b.setCourt(courts.owned(courtId,owner)); b.setCreatedBy(owner); b.setStartsAt(r.startsAt()); b.setEndsAt(r.endsAt()); b.setReason(r.reason().trim());
        return block(blocks.saveAndFlush(b));
    }
    @Transactional(readOnly=true) public List<CourtBlockResponse> listBlocks(UUID courtId, OffsetDateTime from, OffsetDateTime to, AppUser owner) {
        courts.owned(courtId,owner); return blocks.findByCourtIdAndEndsAtAfterAndStartsAtBeforeOrderByStartsAt(courtId,from,to).stream().map(ScheduleService::block).toList();
    }
    @Transactional public void deleteBlock(UUID id,AppUser owner){ CourtBlock b=blocks.findByIdAndCourtVenueOwnerId(id,owner.getId()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Bloqueo no encontrado")); blocks.delete(b); }
    private static OpeningHourResponse hour(OpeningHour h){return new OpeningHourResponse(h.getId(),h.getVenue().getId(),h.getDayOfWeek(),h.getOpensAt(),h.getClosesAt());}
    private static CourtBlockResponse block(CourtBlock b){return new CourtBlockResponse(b.getId(),b.getCourt().getId(),b.getStartsAt(),b.getEndsAt(),b.getReason());}
}
