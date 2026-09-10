package uy.com.fulbito.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.fulbito.domain.*;
import uy.com.fulbito.domain.enums.*;
import uy.com.fulbito.dto.ReservationDtos.*;
import uy.com.fulbito.error.ApiException;
import uy.com.fulbito.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.PageRequest;

@Service
public class ReservationService {
    private static final ZoneId URUGUAY=ZoneId.of("America/Montevideo");
    private static final Duration MAX_AGENDA_RANGE = Duration.ofDays(32);
    private final ReservationRepository reservations; private final CourtService courts; private final OpeningHourRepository hours; private final CourtImageRepository courtImages;
    public ReservationService(ReservationRepository reservations,CourtService courts,OpeningHourRepository hours,CourtImageRepository courtImages){this.reservations=reservations;this.courts=courts;this.hours=hours;this.courtImages=courtImages;}

    @Transactional
    public ReservationResponse create(AppUser actor,ReservationRequest r){

        Court court=courts.get(r.courtId());

        if (!court.isActive() || court.getVenue().getStatus()!=VenueStatus.ACTIVE) throw new ApiException(HttpStatus.CONFLICT,"La cancha no esta disponible");

        boolean owner=actor.getRole()==UserRole.OWNER || actor.getRole()==UserRole.ADMIN;

        if(owner && actor.getRole()!=UserRole.ADMIN && !court.getVenue().getOwner().getId().equals(actor.getId())) throw new ApiException(HttpStatus.FORBIDDEN,"La cancha no te pertenece");

        validateSlot(court,r.startsAt(),r.endsAt());
        Reservation value=new Reservation(); value.setCourt(court); value.setCreatedBy(actor);
        if(actor.getRole()==UserRole.PLAYER){value.setPlayer(actor); value.setPlayerNameSnapshot(actor.getFirstName()+" "+actor.getLastName()); value.setPlayerPhoneSnapshot(actor.getPhone());}
        else {
            if(r.playerName()==null || r.playerName().isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST,"El nombre del jugador es obligatorio para una reserva manual");
            value.setPlayerNameSnapshot(r.playerName().trim()); value.setPlayerPhoneSnapshot(r.playerPhone());
        }
        value.setStartsAt(r.startsAt()); value.setEndsAt(r.endsAt()); value.setStatus(ReservationStatus.CONFIRMED);
        value.setCancellationNoticeHours(court.getVenue().getCancellationNoticeHours()); value.setLateCancellation(false);
        value.setPriceAmount(court.getPricePerSlot()); value.setCurrency("UYU"); value.setNotes(clean(r.notes())); value.setPaymentStatus(PaymentStatus.PENDING);
        // saveAndFlush fuerza el INSERT ahora: si hay solapamiento, el constraint se traduce a HTTP 409.
        return response(reservations.saveAndFlush(value), null);
    }

    @Transactional(readOnly=true) public List<ReservationResponse> mine(AppUser player, int limit){return responses(reservations.findByPlayerIdOrderByStartsAtDesc(player.getId(), PageRequest.of(0, limit)));}

    @Transactional(readOnly=true)
    public ReservationResponse one(UUID id,AppUser actor){
        Reservation reservation = switch (actor.getRole()) {
            case PLAYER -> reservations.findByIdAndPlayerId(id,actor.getId())
                .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Reserva no encontrada"));
            case OWNER -> reservations.findByIdAndCourtVenueOwnerId(id,actor.getId())
                .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Reserva no encontrada"));
            case ADMIN -> reservations.findById(id)
                .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Reserva no encontrada"));
        };
        return response(reservation, coverUrls(List.of(reservation)).get(reservation.getCourt().getId()));
    }

    @Transactional(readOnly=true) public List<ReservationResponse> ownerAgenda(AppUser owner,OffsetDateTime from,OffsetDateTime to,UUID venueId){
        if(!to.isAfter(from))throw new ApiException(HttpStatus.BAD_REQUEST,"El rango de fechas es invalido");
        if(Duration.between(from,to).compareTo(MAX_AGENDA_RANGE)>0)throw new ApiException(HttpStatus.BAD_REQUEST,"La agenda permite consultar hasta 32 dias por vez");
        List<Reservation> values;
        if (venueId == null) {
            values = owner.getRole() == UserRole.ADMIN
                ? reservations.findByStartsAtBetweenOrderByStartsAt(from, to)
                : reservations.findByCourtVenueOwnerIdAndStartsAtBetweenOrderByStartsAt(owner.getId(),from,to);
        } else {
            values = owner.getRole() == UserRole.ADMIN
                ? reservations.findAgendaForAdminVenue(venueId, from, to)
                : reservations.findAgendaForOwnerVenue(venueId, owner.getId(), from, to);
        }
        return responses(values);
    }

    @Transactional public ReservationResponse markPaid(UUID id,AppUser owner){
        Reservation r=owned(id,owner); if(r.getStatus()!=ReservationStatus.CONFIRMED)
            throw new ApiException(HttpStatus.CONFLICT,"Una reserva cancelada no puede cobrarse");

        if(r.getPaymentStatus()==PaymentStatus.PAID)return response(r, coverUrl(r));

        r.setPaymentStatus(PaymentStatus.PAID);r.setPaidAt(OffsetDateTime.now());r.setPaidConfirmedBy(owner);return response(r, coverUrl(r));
    }

    @Transactional public ReservationResponse cancelByOwner(UUID id,AppUser owner){Reservation r=owned(id,owner);cancel(r,ReservationStatus.CANCELLED_BY_OWNER);return response(r, coverUrl(r));}
    @Transactional public ReservationResponse cancelByPlayer(UUID id,AppUser player){Reservation r=reservations.findByIdAndPlayerId(id,player.getId()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Reserva no encontrada"));cancel(r,ReservationStatus.CANCELLED_BY_PLAYER);return response(r, coverUrl(r));}
    private Reservation owned(UUID id,AppUser owner){
        if (owner.getRole() == UserRole.ADMIN)
            return reservations.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Reserva no encontrada"));
        return reservations.findByIdAndCourtVenueOwnerId(id,owner.getId()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Reserva no encontrada o no pertenece a tus complejos"));
    }
    private void cancel(Reservation r,ReservationStatus status){
        if(r.getStatus()!=ReservationStatus.CONFIRMED)throw new ApiException(HttpStatus.CONFLICT,"La reserva ya esta cancelada");
        if(r.getPaymentStatus()==PaymentStatus.PAID)throw new ApiException(HttpStatus.CONFLICT,"No se puede cancelar una reserva marcada como paga");
        OffsetDateTime cancelledAt=OffsetDateTime.now();
        r.setStatus(status);r.setCancelledAt(cancelledAt);
        r.setLateCancellation(status==ReservationStatus.CANCELLED_BY_PLAYER && cancelledAt.isAfter(r.getStartsAt().minusHours(r.getCancellationNoticeHours())));
    }
    private void validateSlot(Court court,OffsetDateTime start,OffsetDateTime end){
        if(!end.isAfter(start))throw new ApiException(HttpStatus.BAD_REQUEST,"El fin debe ser posterior al inicio");
        if(Duration.between(start,end).toMinutes()!=court.getSlotMinutes())throw new ApiException(HttpStatus.BAD_REQUEST,"La reserva debe durar exactamente "+court.getSlotMinutes()+" minutos");
        ZonedDateTime localStart=start.atZoneSameInstant(URUGUAY), localEnd=end.atZoneSameInstant(URUGUAY);
        if(!localStart.isAfter(ZonedDateTime.now(URUGUAY)))throw new ApiException(HttpStatus.CONFLICT,"No se puede reservar un turno pasado");
        if(!localStart.toLocalDate().equals(localEnd.toLocalDate()))throw new ApiException(HttpStatus.BAD_REQUEST,"La reserva debe comenzar y terminar el mismo dia local");
        short day=(short)localStart.getDayOfWeek().getValue();
        boolean inside=hours.findByVenueIdOrderByDayOfWeekAscOpensAtAsc(court.getVenue().getId()).stream().filter(h->h.getDayOfWeek()==day).anyMatch(h->{
            LocalTime s=localStart.toLocalTime(),e=localEnd.toLocalTime(); long offset=Duration.between(h.getOpensAt(),s).toMinutes();
            return !s.isBefore(h.getOpensAt())&&!e.isAfter(h.getClosesAt())&&offset>=0&&offset%court.getSlotMinutes()==0;
        });
        if(!inside)throw new ApiException(HttpStatus.CONFLICT,"El turno no coincide con el horario de apertura o con el inicio de un turno");
    }
    private List<ReservationResponse> responses(List<Reservation> values) {
        Map<UUID, String> covers = coverUrls(values);
        return values.stream().map(value -> response(value, covers.get(value.getCourt().getId()))).toList();
    }
    private Map<UUID, String> coverUrls(List<Reservation> values) {
        List<UUID> courtIds = values.stream().map(value -> value.getCourt().getId()).distinct().toList();
        if (courtIds.isEmpty()) return Map.of();
        Map<UUID, String> result = new HashMap<>();
        courtImages.findByCourtIdInOrderByCourtIdAscSortOrderAsc(courtIds)
            .forEach(image -> result.putIfAbsent(image.getCourt().getId(), image.getUrl()));
        return result;
    }
    private String coverUrl(Reservation value) {
        return courtImages.findByCourtIdOrderBySortOrder(value.getCourt().getId()).stream().findFirst().map(CourtImage::getUrl).orElse(null);
    }
    private static ReservationResponse response(Reservation r, String courtImageUrl) {
        return new ReservationResponse(
            r.getId(),

            r.getCourt().getId(),
            r.getCourt().getName(),
            r.getCourt().getFootballFormat(),
            r.getCourt().getVenue().getId(),
            r.getCourt().getVenue().getName(),
            courtImageUrl,

            r.getPlayer() == null ? null : r.getPlayer().getId(),
            r.getStartsAt(),
            r.getEndsAt(),
            r.getStatus(),
            r.getPriceAmount(),
            r.getCurrency(),
            r.getPlayerNameSnapshot(),
            r.getPlayerPhoneSnapshot(),
            r.getNotes(),
            r.getCancellationNoticeHours(),
            r.getStartsAt().minusHours(r.getCancellationNoticeHours()),
            r.isLateCancellation(),
            r.getPaymentStatus(),
            r.getPaidAt(),
            r.getCancelledAt()
        );
    }
    private String clean(String s){return s==null||s.isBlank()?null:s.trim();}
}
