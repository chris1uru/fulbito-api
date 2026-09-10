package uy.com.fulbito.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.fulbito.domain.*;
import uy.com.fulbito.domain.enums.*;
import uy.com.fulbito.dto.MatchRequestDtos.*;
import uy.com.fulbito.error.ApiException;
import uy.com.fulbito.repository.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchRequestService {
    private static final ZoneId URUGUAY = ZoneId.of("America/Montevideo");
    private static final int MAX_RESULTS = 100;
    private static final int MAX_AVAILABILITIES = 6;
    private static final Duration MIN_WINDOW = Duration.ofMinutes(30);
    private static final Duration MAX_WINDOW = Duration.ofHours(8);
    private static final Duration MAX_FUTURE = Duration.ofDays(90);

    private final MatchRequestRepository requests;
    private final MatchRequestAvailabilityRepository availabilities;
    private final MatchInterestRepository interests;
    private final ReservationRepository reservations;

    public MatchRequestService(
        MatchRequestRepository requests,
        MatchRequestAvailabilityRepository availabilities,
        MatchInterestRepository interests,
        ReservationRepository reservations
    ) {
        this.requests = requests;
        this.availabilities = availabilities;
        this.interests = interests;
        this.reservations = reservations;
    }

    @Transactional
    public List<MatchRequestResponse> discover(
        AppUser player,
        MatchStyle style,
        FootballFormat format,
        OffsetDateTime from,
        OffsetDateTime to
    ) {
        validateRange(from, to);
        OffsetDateTime now = OffsetDateTime.now();
        expireOld(now);
        List<MatchRequest> values = requests.findByStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            MatchRequestStatus.OPEN, now, PageRequest.of(0, MAX_RESULTS)
        ).stream()
            .filter(value -> !value.getCreator().getId().equals(player.getId()))
            .filter(value -> style == null || value.getStyle() == style)
            .filter(value -> format == null || value.getFootballFormat() == format)
            .toList();
        ResponseContext context = context(values, player.getId());
        return values.stream()
            .filter(value -> overlapsRange(value, context.availabilities(), from, to))
            .map(value -> response(value, player.getId(), context))
            .toList();
    }

    @Transactional
    public List<MatchRequestResponse> mine(AppUser player) {
        expireOld(OffsetDateTime.now());
        List<MatchRequest> values = requests.findByCreatorIdOrderByCreatedAtDesc(
            player.getId(), PageRequest.of(0, MAX_RESULTS)
        );
        ResponseContext context = context(values, player.getId());
        return values.stream().map(value -> response(value, player.getId(), context)).toList();
    }

    @Transactional
    public MatchRequestResponse create(AppUser player, CreateMatchRequestRequest body) {
        requirePhone(player);
        OffsetDateTime now = OffsetDateTime.now();
        MatchRequest value = new MatchRequest();
        value.setCreator(player);
        value.setStyle(body.style());
        value.setNotes(clean(body.notes()));
        value.setStatus(MatchRequestStatus.OPEN);

        List<AvailabilityRequest> requestedWindows = body.availabilities() == null
            ? List.of()
            : body.availabilities();
        if (body.reservationId() != null) {
            if (!requestedWindows.isEmpty())
                throw new ApiException(HttpStatus.BAD_REQUEST, "Elegí una reserva o franjas disponibles, no ambas opciones");
            Reservation reservation = reservations.findByIdAndPlayerId(body.reservationId(), player.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La reserva seleccionada no existe"));
            if (reservation.getStatus() != ReservationStatus.CONFIRMED || !reservation.getStartsAt().isAfter(now))
                throw new ApiException(HttpStatus.CONFLICT, "La reserva seleccionada debe estar confirmada y ser futura");
            if (requests.existsByReservationIdAndStatus(reservation.getId(), MatchRequestStatus.OPEN))
                throw new ApiException(HttpStatus.CONFLICT, "Ya existe una búsqueda abierta para esa reserva");
            value.setReservation(reservation);
            value.setFootballFormat(reservation.getCourt().getFootballFormat());
            value.setExpiresAt(reservation.getStartsAt());
            requests.saveAndFlush(value);
        } else {
            value.setFootballFormat(body.footballFormat());
            List<AvailabilityRequest> windows = validateWindows(requestedWindows, now);
            value.setExpiresAt(windows.stream().map(AvailabilityRequest::endsAt).max(Comparator.naturalOrder()).orElseThrow());
            requests.saveAndFlush(value);
            availabilities.saveAllAndFlush(windows.stream().map(window -> {
                MatchRequestAvailability item = new MatchRequestAvailability();
                item.setMatchRequest(value);
                item.setStartsAt(window.startsAt());
                item.setEndsAt(window.endsAt());
                return item;
            }).toList());
        }

        return response(value, player.getId(), context(List.of(value), player.getId()));
    }

    @Transactional
    public MatchRequestResponse close(UUID id, AppUser player) {
        MatchRequest value = requests.findByIdAndCreatorId(id, player.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Búsqueda no encontrada"));
        if (value.getStatus() != MatchRequestStatus.OPEN)
            throw new ApiException(HttpStatus.CONFLICT, "La búsqueda ya no está abierta");
        value.setStatus(MatchRequestStatus.CLOSED);
        requests.saveAndFlush(value);
        return response(value, player.getId(), context(List.of(value), player.getId()));
    }

    @Transactional
    public MatchInterestResponse expressInterest(UUID id, AppUser player) {
        requirePhone(player);
        MatchRequest request = requests.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Búsqueda no encontrada"));
        if (request.getStatus() != MatchRequestStatus.OPEN || !request.getExpiresAt().isAfter(OffsetDateTime.now()))
            throw new ApiException(HttpStatus.CONFLICT, "La búsqueda ya no está disponible");
        if (request.getCreator().getId().equals(player.getId()))
            throw new ApiException(HttpStatus.CONFLICT, "No podés postularte a tu propia búsqueda");
        if (interests.existsByMatchRequestIdAndPlayerId(id, player.getId()))
            throw new ApiException(HttpStatus.CONFLICT, "Ya indicaste que querés jugar este partido");

        MatchInterest value = new MatchInterest();
        value.setMatchRequest(request);
        value.setPlayer(player);
        value.setPlayerNameSnapshot(fullName(player));
        value.setPlayerPhoneSnapshot(player.getPhone());
        return interestResponse(interests.saveAndFlush(value));
    }

    @Transactional(readOnly = true)
    public List<MatchInterestResponse> interests(UUID id, AppUser player) {
        requests.findByIdAndCreatorId(id, player.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Búsqueda no encontrada"));
        return interests.findByMatchRequestIdOrderByCreatedAt(id).stream()
            .map(MatchRequestService::interestResponse)
            .toList();
    }

    private void expireOld(OffsetDateTime now) {
        requests.expireOld(now, MatchRequestStatus.OPEN, MatchRequestStatus.EXPIRED);
    }

    private List<AvailabilityRequest> validateWindows(List<AvailabilityRequest> values, OffsetDateTime now) {
        if (values.isEmpty())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Agregá al menos una franja disponible");
        if (values.size() > MAX_AVAILABILITIES)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Podés agregar hasta seis franjas disponibles");

        List<AvailabilityRequest> sorted = values.stream()
            .sorted(Comparator.comparing(AvailabilityRequest::startsAt))
            .toList();
        for (int index = 0; index < sorted.size(); index++) {
            AvailabilityRequest window = sorted.get(index);
            Duration duration = Duration.between(window.startsAt(), window.endsAt());
            if (!window.startsAt().isAfter(now))
                throw new ApiException(HttpStatus.BAD_REQUEST, "Todas las franjas deben comenzar en el futuro");
            if (Duration.between(now, window.startsAt()).compareTo(MAX_FUTURE) > 0)
                throw new ApiException(HttpStatus.BAD_REQUEST, "Las franjas pueden publicarse con hasta 90 días de anticipación");
            if (duration.compareTo(MIN_WINDOW) < 0 || duration.compareTo(MAX_WINDOW) > 0)
                throw new ApiException(HttpStatus.BAD_REQUEST, "Cada franja debe durar entre 30 minutos y 8 horas");
            ZonedDateTime localStart = window.startsAt().atZoneSameInstant(URUGUAY);
            ZonedDateTime localEnd = window.endsAt().atZoneSameInstant(URUGUAY);
            if (!localStart.toLocalDate().equals(localEnd.toLocalDate()))
                throw new ApiException(HttpStatus.BAD_REQUEST, "Cada franja debe comenzar y terminar el mismo día en Uruguay");
            if (index > 0 && window.startsAt().isBefore(sorted.get(index - 1).endsAt()))
                throw new ApiException(HttpStatus.BAD_REQUEST, "Las franjas disponibles no pueden superponerse");
        }
        return sorted;
    }

    private static void validateRange(OffsetDateTime from, OffsetDateTime to) {
        if ((from == null) != (to == null))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debés indicar el inicio y el fin del filtro de fecha");
        if (from != null && !to.isAfter(from))
            throw new ApiException(HttpStatus.BAD_REQUEST, "El rango de fechas es inválido");
    }

    private static boolean overlapsRange(
        MatchRequest value,
        Map<UUID, List<MatchRequestAvailability>> byRequest,
        OffsetDateTime from,
        OffsetDateTime to
    ) {
        if (from == null) return true;
        if (value.getReservation() != null)
            return overlaps(value.getReservation().getStartsAt(), value.getReservation().getEndsAt(), from, to);
        return byRequest.getOrDefault(value.getId(), List.of()).stream()
            .anyMatch(window -> overlaps(window.getStartsAt(), window.getEndsAt(), from, to));
    }

    private static boolean overlaps(OffsetDateTime start, OffsetDateTime end, OffsetDateTime from, OffsetDateTime to) {
        return end.isAfter(from) && start.isBefore(to);
    }

    private ResponseContext context(List<MatchRequest> values, UUID playerId) {
        List<UUID> ids = values.stream().map(MatchRequest::getId).toList();
        if (ids.isEmpty()) return new ResponseContext(Map.of(), Map.of(), Set.of());
        Map<UUID, List<MatchRequestAvailability>> byRequest = availabilities
            .findByMatchRequestIdInOrderByStartsAt(ids).stream()
            .collect(Collectors.groupingBy(item -> item.getMatchRequest().getId(), LinkedHashMap::new, Collectors.toList()));
        Map<UUID, Long> counts = interests.countByRequestIds(ids).stream()
            .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
        Set<UUID> interestedIds = interests.findByPlayerIdAndMatchRequestIdIn(playerId, ids).stream()
            .map(item -> item.getMatchRequest().getId())
            .collect(Collectors.toSet());
        return new ResponseContext(byRequest, counts, interestedIds);
    }

    private static MatchRequestResponse response(MatchRequest value, UUID actorId, ResponseContext context) {
        Reservation reservation = value.getReservation();
        return new MatchRequestResponse(
            value.getId(),
            value.getCreator().getId(),
            fullName(value.getCreator()),
            value.getCreator().getId().equals(actorId),
            value.getFootballFormat(),
            value.getStyle(),
            value.getStatus(),
            value.getNotes(),
            reservation == null ? null : reservation.getId(),
            reservation == null ? null : reservation.getCourt().getId(),
            reservation == null ? null : reservation.getCourt().getName(),
            reservation == null ? null : reservation.getCourt().getVenue().getId(),
            reservation == null ? null : reservation.getCourt().getVenue().getName(),
            reservation == null ? null : reservation.getStartsAt(),
            reservation == null ? null : reservation.getEndsAt(),
            context.availabilities().getOrDefault(value.getId(), List.of()).stream()
                .map(item -> new AvailabilityResponse(item.getId(), item.getStartsAt(), item.getEndsAt()))
                .toList(),
            context.interestCounts().getOrDefault(value.getId(), 0L),
            context.interestedRequestIds().contains(value.getId()),
            value.getCreatedAt(),
            value.getExpiresAt()
        );
    }

    private static MatchInterestResponse interestResponse(MatchInterest value) {
        return new MatchInterestResponse(
            value.getId(), value.getPlayer().getId(), value.getPlayerNameSnapshot(),
            value.getPlayerPhoneSnapshot(), value.getCreatedAt()
        );
    }

    private static void requirePhone(AppUser player) {
        if (player.getPhone() == null || player.getPhone().isBlank())
            throw new ApiException(HttpStatus.CONFLICT, "Agregá un teléfono a tu perfil para usar Buscar rival");
    }

    private static String fullName(AppUser user) { return user.getFirstName() + " " + user.getLastName(); }
    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private record ResponseContext(
        Map<UUID, List<MatchRequestAvailability>> availabilities,
        Map<UUID, Long> interestCounts,
        Set<UUID> interestedRequestIds
    ) {}
}
