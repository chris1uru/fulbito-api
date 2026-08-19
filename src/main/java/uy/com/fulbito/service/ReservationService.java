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

@Service
public class ReservationService {
    private static final ZoneId URUGUAY=ZoneId.of("America/Montevideo");
    private final ReservationRepository reservations; private final CourtService courts; private final OpeningHourRepository hours;
    public ReservationService(ReservationRepository reservations,CourtService courts,OpeningHourRepository hours){this.reservations=reservations;this.courts=courts;this.hours=hours;}

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
        value.setPriceAmount(court.getPricePerSlot()); value.setCurrency("UYU"); value.setNotes(clean(r.notes())); value.setPaymentStatus(PaymentStatus.PENDING);
        // saveAndFlush fuerza el INSERT ahora: si hay solapamiento, el constraint se traduce a HTTP 409.
        return response(reservations.saveAndFlush(value));
    }

    @Transactional(readOnly=true) public List<ReservationResponse> mine(AppUser player){return reservations.findByPlayerIdOrderByStartsAtDesc(player.getId()).stream().map(ReservationService::response).toList();}

    @Transactional(readOnly=true) public List<ReservationResponse> ownerAgenda(AppUser owner,OffsetDateTime from,OffsetDateTime to){
        if(!to.isAfter(from))throw new ApiException(HttpStatus.BAD_REQUEST,"El rango de fechas es invalido");
        List<Reservation> values = owner.getRole() == UserRole.ADMIN
            ? reservations.findByStartsAtBetweenOrderByStartsAt(from, to)
            : reservations.findByCourtVenueOwnerIdAndStartsAtBetweenOrderByStartsAt(owner.getId(),from,to);
        return values.stream().map(ReservationService::response).toList();
    }

    @Transactional public ReservationResponse markPaid(UUID id,AppUser owner){
        Reservation r=owned(id,owner); if(r.getStatus()!=ReservationStatus.CONFIRMED)
            throw new ApiException(HttpStatus.CONFLICT,"Una reserva cancelada no puede cobrarse");

        if(r.getPaymentStatus()==PaymentStatus.PAID)return response(r);

        r.setPaymentStatus(PaymentStatus.PAID);r.setPaidAt(OffsetDateTime.now());r.setPaidConfirmedBy(owner);return response(r);
    }

    @Transactional public ReservationResponse cancelByOwner(UUID id,AppUser owner){Reservation r=owned(id,owner);cancel(r,ReservationStatus.CANCELLED_BY_OWNER);return response(r);}
    @Transactional public ReservationResponse cancelByPlayer(UUID id,AppUser player){Reservation r=reservations.findByIdAndPlayerId(id,player.getId()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Reserva no encontrada"));cancel(r,ReservationStatus.CANCELLED_BY_PLAYER);return response(r);}
    private Reservation owned(UUID id,AppUser owner){
        if (owner.getRole() == UserRole.ADMIN)
            return reservations.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Reserva no encontrada"));
        return reservations.findByIdAndCourtVenueOwnerId(id,owner.getId()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Reserva no encontrada o no pertenece a tus complejos"));
    }
    private void cancel(Reservation r,ReservationStatus status){if(r.getStatus()!=ReservationStatus.CONFIRMED)throw new ApiException(HttpStatus.CONFLICT,"La reserva ya esta cancelada");if(r.getPaymentStatus()==PaymentStatus.PAID)throw new ApiException(HttpStatus.CONFLICT,"No se puede cancelar una reserva marcada como paga");r.setStatus(status);r.setCancelledAt(OffsetDateTime.now());}
    private void validateSlot(Court court,OffsetDateTime start,OffsetDateTime end){
        if(!end.isAfter(start))throw new ApiException(HttpStatus.BAD_REQUEST,"El fin debe ser posterior al inicio");
        if(Duration.between(start,end).toMinutes()!=court.getSlotMinutes())throw new ApiException(HttpStatus.BAD_REQUEST,"La reserva debe durar exactamente "+court.getSlotMinutes()+" minutos");
        ZonedDateTime localStart=start.atZoneSameInstant(URUGUAY), localEnd=end.atZoneSameInstant(URUGUAY);
        if(!localStart.toLocalDate().equals(localEnd.toLocalDate()))throw new ApiException(HttpStatus.BAD_REQUEST,"La reserva debe comenzar y terminar el mismo dia local");
        short day=(short)localStart.getDayOfWeek().getValue();
        boolean inside=hours.findByVenueIdOrderByDayOfWeekAscOpensAtAsc(court.getVenue().getId()).stream().filter(h->h.getDayOfWeek()==day).anyMatch(h->{
            LocalTime s=localStart.toLocalTime(),e=localEnd.toLocalTime(); long offset=Duration.between(h.getOpensAt(),s).toMinutes();
            return !s.isBefore(h.getOpensAt())&&!e.isAfter(h.getClosesAt())&&offset>=0&&offset%court.getSlotMinutes()==0;
        });
        if(!inside)throw new ApiException(HttpStatus.CONFLICT,"El turno no coincide con el horario de apertura o con el inicio de un turno");
    }
    private static ReservationResponse response(Reservation r) {
        return new ReservationResponse(
            r.getId(),

            r.getCourt().getId(),
            r.getCourt().getName(),
            r.getCourt().getVenue().getId(),
            r.getCourt().getVenue().getName(),

            r.getPlayer() == null ? null : r.getPlayer().getId(),
            r.getStartsAt(),
            r.getEndsAt(),
            r.getStatus(),
            r.getPriceAmount(),
            r.getCurrency(),
            r.getPlayerNameSnapshot(),
            r.getPlayerPhoneSnapshot(),
            r.getNotes(),
            r.getPaymentStatus(),
            r.getPaidAt(),
            r.getCancelledAt()
        );
    }
    private String clean(String s){return s==null||s.isBlank()?null:s.trim();}
}
