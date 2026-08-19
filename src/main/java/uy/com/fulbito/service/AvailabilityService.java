package uy.com.fulbito.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.fulbito.domain.Court;
import uy.com.fulbito.domain.CourtBlock;
import uy.com.fulbito.domain.Reservation;
import uy.com.fulbito.domain.enums.ReservationStatus;
import uy.com.fulbito.dto.AvailabilityDtos.AvailabilitySlotResponse;
import uy.com.fulbito.dto.AvailabilityDtos.CourtAvailabilityResponse;
import uy.com.fulbito.repository.CourtBlockRepository;
import uy.com.fulbito.repository.OpeningHourRepository;
import uy.com.fulbito.repository.ReservationRepository;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
