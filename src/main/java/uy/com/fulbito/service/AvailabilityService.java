package uy.com.fulbito.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.fulbito.domain.Court;
import uy.com.fulbito.domain.CourtBlock;
import uy.com.fulbito.domain.OpeningHour;
import uy.com.fulbito.domain.Reservation;
import uy.com.fulbito.domain.enums.ReservationStatus;
import uy.com.fulbito.dto.AvailabilityDtos.AvailabilitySlotResponse;
import uy.com.fulbito.dto.AvailabilityDtos.AvailableCourtResponse;
import uy.com.fulbito.dto.AvailabilityDtos.CourtAvailabilityResponse;
import uy.com.fulbito.dto.AvailabilityDtos.VenueAvailabilityResponse;
import uy.com.fulbito.dto.AvailabilityDtos.VenueAvailabilitySearchResponse;
import uy.com.fulbito.repository.CourtBlockRepository;
import uy.com.fulbito.repository.OpeningHourRepository;
import uy.com.fulbito.repository.ReservationRepository;

import java.time.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {
    private static final ZoneId URUGUAY = ZoneId.of("America/Montevideo");

    private final CourtService courts;
    private final OpeningHourRepository hours;
    private final CourtBlockRepository blocks;
    private final ReservationRepository reservations;

    public AvailabilityService(
        CourtService courts,
        OpeningHourRepository hours,
        CourtBlockRepository blocks,
        ReservationRepository reservations
    ) {
        this.courts = courts;
        this.hours = hours;
        this.blocks = blocks;
        this.reservations = reservations;
    }

    @Transactional(readOnly = true)
    public CourtAvailabilityResponse availability(UUID courtId, LocalDate date) {
        Court court = courts.publicActive(courtId);
        OffsetDateTime dayStart = date.atStartOfDay(URUGUAY).toOffsetDateTime();
        OffsetDateTime dayEnd = date.plusDays(1).atStartOfDay(URUGUAY).toOffsetDateTime();

        List<CourtBlock> dayBlocks = blocks
            .findByCourtIdAndEndsAtAfterAndStartsAtBeforeOrderByStartsAt(courtId, dayStart, dayEnd);
        List<Reservation> dayReservations = reservations
            .findByCourtIdAndStatusAndEndsAtAfterAndStartsAtBefore(
                courtId, ReservationStatus.CONFIRMED, dayStart, dayEnd
            );

        List<AvailabilitySlotResponse> slots = new ArrayList<>();
        short dayOfWeek = (short) date.getDayOfWeek().getValue();
        hours.findByVenueIdOrderByDayOfWeekAscOpensAtAsc(court.getVenue().getId()).stream()
            .filter(hour -> hour.getDayOfWeek() == dayOfWeek)
            .forEach(hour -> addSlots(
                court, date, hour.getOpensAt(), hour.getClosesAt(), dayBlocks, dayReservations, slots
            ));

        return new CourtAvailabilityResponse(
            courtId, date, URUGUAY.getId(), court.getSlotMinutes(), slots
        );
    }

    @Transactional(readOnly = true)
    public VenueAvailabilitySearchResponse venueAvailability(LocalDate date, LocalTime time) {
        LocalTime requestedTime = time.withSecond(0).withNano(0);
        ZonedDateTime requestedStart = ZonedDateTime.of(date, requestedTime, URUGUAY);
        ZonedDateTime now = ZonedDateTime.now(URUGUAY);
        List<Court> activeCourts = courts.publicActive();

        if (activeCourts.isEmpty()) {
            return new VenueAvailabilitySearchResponse(
                date, requestedTime, URUGUAY.getId(), List.of()
            );
        }

        List<UUID> courtIds = activeCourts.stream().map(Court::getId).toList();
        List<UUID> venueIds = activeCourts.stream()
            .map(court -> court.getVenue().getId())
            .distinct()
            .toList();
        short dayOfWeek = (short) date.getDayOfWeek().getValue();
        OffsetDateTime dayStart = date.atStartOfDay(URUGUAY).toOffsetDateTime();
        OffsetDateTime dayEnd = date.plusDays(1).atStartOfDay(URUGUAY).toOffsetDateTime();

        Map<UUID, List<OpeningHour>> hoursByVenue = hours
            .findByVenueIdInAndDayOfWeekOrderByOpensAtAsc(venueIds, dayOfWeek)
            .stream()
            .collect(Collectors.groupingBy(hour -> hour.getVenue().getId()));
        Map<UUID, List<CourtBlock>> blocksByCourt = blocks
            .findByCourtIdInAndEndsAtAfterAndStartsAtBefore(courtIds, dayStart, dayEnd)
            .stream()
            .collect(Collectors.groupingBy(block -> block.getCourt().getId()));
        Map<UUID, List<Reservation>> reservationsByCourt = reservations
            .findByCourtIdInAndStatusAndEndsAtAfterAndStartsAtBefore(
                courtIds, ReservationStatus.CONFIRMED, dayStart, dayEnd
            )
            .stream()
            .collect(Collectors.groupingBy(reservation -> reservation.getCourt().getId()));

        Map<UUID, List<Court>> courtsByVenue = activeCourts.stream().collect(
            Collectors.groupingBy(
                court -> court.getVenue().getId(),
                LinkedHashMap::new,
                Collectors.toList()
            )
        );

        List<VenueAvailabilityResponse> venueResults = courtsByVenue.entrySet().stream()
            .map(entry -> venueAvailability(
                entry.getKey(),
                entry.getValue(),
                requestedStart,
                now,
                hoursByVenue.getOrDefault(entry.getKey(), List.of()),
                blocksByCourt,
                reservationsByCourt
            ))
            .toList();

        return new VenueAvailabilitySearchResponse(
            date, requestedTime, URUGUAY.getId(), venueResults
        );
    }

    private VenueAvailabilityResponse venueAvailability(
        UUID venueId,
        List<Court> venueCourts,
        ZonedDateTime requestedStart,
        ZonedDateTime now,
        List<OpeningHour> venueHours,
        Map<UUID, List<CourtBlock>> blocksByCourt,
        Map<UUID, List<Reservation>> reservationsByCourt
    ) {
        List<AvailableCourtResponse> availableCourts = venueCourts.stream()
            .filter(court -> isAvailable(
                court,
                requestedStart,
                now,
                venueHours,
                blocksByCourt.getOrDefault(court.getId(), List.of()),
                reservationsByCourt.getOrDefault(court.getId(), List.of())
            ))
            .map(court -> {
                OffsetDateTime start = requestedStart.toOffsetDateTime();
                OffsetDateTime end = requestedStart
                    .plusMinutes(court.getSlotMinutes())
                    .toOffsetDateTime();
                return new AvailableCourtResponse(
                    court.getId(),
                    court.getName(),
                    start,
                    end,
                    court.getPricePerSlot(),
                    court.getCurrency(),
                    court.getSlotMinutes()
                );
            })
            .toList();

        return new VenueAvailabilityResponse(
            venueId,
            !availableCourts.isEmpty(),
            venueCourts.size(),
            availableCourts
        );
    }

    private static boolean isAvailable(
        Court court,
        ZonedDateTime requestedStart,
        ZonedDateTime now,
        List<OpeningHour> venueHours,
        List<CourtBlock> courtBlocks,
        List<Reservation> courtReservations
    ) {
        if (!requestedStart.isAfter(now)) return false;

        int requestedMinute = requestedStart.getHour() * 60 + requestedStart.getMinute();
        int requestedEndMinute = requestedMinute + court.getSlotMinutes();
        boolean validSlot = venueHours.stream().anyMatch(hour -> {
            int openMinute = hour.getOpensAt().getHour() * 60 + hour.getOpensAt().getMinute();
            int closeMinute = hour.getClosesAt().getHour() * 60 + hour.getClosesAt().getMinute();
            return requestedMinute >= openMinute
                && requestedEndMinute <= closeMinute
                && (requestedMinute - openMinute) % court.getSlotMinutes() == 0;
        });
        if (!validSlot) return false;

        OffsetDateTime start = requestedStart.toOffsetDateTime();
        OffsetDateTime end = requestedStart
            .plusMinutes(court.getSlotMinutes())
            .toOffsetDateTime();
        return !overlapsBlocks(courtBlocks, start, end)
            && !overlapsReservations(courtReservations, start, end);
    }

    private void addSlots(
        Court court,
        LocalDate date,
        LocalTime opensAt,
        LocalTime closesAt,
        List<CourtBlock> blocks,
        List<Reservation> reservations,
        List<AvailabilitySlotResponse> result
    ) {
        ZonedDateTime cursor = ZonedDateTime.of(date, opensAt, URUGUAY);
        ZonedDateTime close = ZonedDateTime.of(date, closesAt, URUGUAY);
        ZonedDateTime now = ZonedDateTime.now(URUGUAY);

        while (!cursor.plusMinutes(court.getSlotMinutes()).isAfter(close)) {
            OffsetDateTime start = cursor.toOffsetDateTime();
            OffsetDateTime end = cursor.plusMinutes(court.getSlotMinutes()).toOffsetDateTime();
            boolean occupied = overlapsBlocks(blocks, start, end)
                || overlapsReservations(reservations, start, end);
            result.add(new AvailabilitySlotResponse(start, end, cursor.isAfter(now) && !occupied));
            cursor = cursor.plusMinutes(court.getSlotMinutes());
        }
    }

    private static boolean overlapsBlocks(
        List<CourtBlock> values,
        OffsetDateTime start,
        OffsetDateTime end
    ) {
        return values.stream().anyMatch(
            value -> value.getEndsAt().isAfter(start) && value.getStartsAt().isBefore(end)
        );
    }

    private static boolean overlapsReservations(
        List<Reservation> values,
        OffsetDateTime start,
        OffsetDateTime end
    ) {
        return values.stream().anyMatch(
            value -> value.getEndsAt().isAfter(start) && value.getStartsAt().isBefore(end)
        );
    }
}
