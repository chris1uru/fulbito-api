package uy.com.fulbito.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.com.fulbito.domain.Court;
import uy.com.fulbito.domain.CourtBlock;
import uy.com.fulbito.domain.OpeningHour;
import uy.com.fulbito.domain.Reservation;
import uy.com.fulbito.domain.Venue;
import uy.com.fulbito.domain.enums.ReservationStatus;
import uy.com.fulbito.repository.CourtBlockRepository;
import uy.com.fulbito.repository.OpeningHourRepository;
import uy.com.fulbito.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {
    private static final ZoneId URUGUAY = ZoneId.of("America/Montevideo");
    private static final LocalDate DATE = LocalDate.of(2099, 8, 20);

    @Mock private CourtService courts;
    @Mock private OpeningHourRepository hours;
    @Mock private CourtBlockRepository blocks;
    @Mock private ReservationRepository reservations;

    private AvailabilityService service;
    private Court courtFive;
    private Court courtSeven;

    @BeforeEach
    void setUp() {
        service = new AvailabilityService(courts, hours, blocks, reservations);

        UUID venueId = UUID.randomUUID();
        Venue venue = mock(Venue.class);
        when(venue.getId()).thenReturn(venueId);

        courtFive = court(venue, "Campus 5", (short) 60);
        courtSeven = court(venue, "Campus 7", (short) 90);
        when(courts.publicActive()).thenReturn(List.of(courtFive, courtSeven));

        OpeningHour opening = mock(OpeningHour.class);
        when(opening.getVenue()).thenReturn(venue);
        when(opening.getOpensAt()).thenReturn(LocalTime.of(16, 0));
        when(opening.getClosesAt()).thenReturn(LocalTime.of(23, 30));
        when(hours.findByVenueIdInAndDayOfWeekOrderByOpensAtAsc(any(), anyShort()))
            .thenReturn(List.of(opening));
    }

    @Test
    void venueIsUnavailableWhenEveryCourtIsOccupied() {
        List<Reservation> occupiedCourts = List.of(
            reservation(courtFive, at(19, 0), at(20, 0)),
            reservation(courtSeven, at(19, 0), at(20, 30))
        );
        when(blocks.findByCourtIdInAndEndsAtAfterAndStartsAtBefore(any(), any(), any()))
            .thenReturn(List.of());
        when(reservations.findByCourtIdInAndStatusAndEndsAtAfterAndStartsAtBefore(
            any(), eq(ReservationStatus.CONFIRMED), any(), any()
        )).thenReturn(occupiedCourts);

        var result = service.venueAvailability(DATE, LocalTime.of(19, 0));

        assertEquals(1, result.venues().size());
        assertFalse(result.venues().getFirst().available());
        assertTrue(result.venues().getFirst().availableCourts().isEmpty());
    }

    @Test
    void venueIsAvailableWhenAtLeastOneCourtIsFree() {
        List<Reservation> occupiedCourts = List.of(
            reservation(courtFive, at(19, 0), at(20, 0)),
            reservation(courtSeven, at(19, 0), at(20, 30))
        );
        when(blocks.findByCourtIdInAndEndsAtAfterAndStartsAtBefore(any(), any(), any()))
            .thenReturn(List.of());
        when(reservations.findByCourtIdInAndStatusAndEndsAtAfterAndStartsAtBefore(
            any(), eq(ReservationStatus.CONFIRMED), any(), any()
        )).thenReturn(occupiedCourts);

        var result = service.venueAvailability(DATE, LocalTime.of(20, 0));

        var venue = result.venues().getFirst();
        assertTrue(venue.available());
        assertEquals(1, venue.availableCourts().size());
        assertEquals(courtFive.getId(), venue.availableCourts().getFirst().courtId());
    }

    @Test
    void blockAndInvalidSlotAlignmentMakeVenueUnavailable() {
        CourtBlock courtBlock = block(courtFive, at(21, 0), at(22, 0));
        when(blocks.findByCourtIdInAndEndsAtAfterAndStartsAtBefore(any(), any(), any()))
            .thenReturn(List.of(courtBlock));
        when(reservations.findByCourtIdInAndStatusAndEndsAtAfterAndStartsAtBefore(
            any(), eq(ReservationStatus.CONFIRMED), any(), any()
        )).thenReturn(List.of());

        var result = service.venueAvailability(DATE, LocalTime.of(21, 0));

        assertFalse(result.venues().getFirst().available());
    }

    private static Court court(Venue venue, String name, short slotMinutes) {
        Court value = mock(Court.class);
        when(value.getId()).thenReturn(UUID.randomUUID());
        when(value.getVenue()).thenReturn(venue);
        lenient().when(value.getName()).thenReturn(name);
        when(value.getSlotMinutes()).thenReturn(slotMinutes);
        lenient().when(value.getPricePerSlot()).thenReturn(BigDecimal.valueOf(1600));
        lenient().when(value.getCurrency()).thenReturn("UYU");
        return value;
    }

    private static Reservation reservation(Court court, OffsetDateTime start, OffsetDateTime end) {
        Reservation value = new Reservation();
        value.setCourt(court);
        value.setStartsAt(start);
        value.setEndsAt(end);
        return value;
    }

    private static CourtBlock block(Court court, OffsetDateTime start, OffsetDateTime end) {
        CourtBlock value = new CourtBlock();
        value.setCourt(court);
        value.setStartsAt(start);
        value.setEndsAt(end);
        return value;
    }

    private static OffsetDateTime at(int hour, int minute) {
        return DATE.atTime(hour, minute).atZone(URUGUAY).toOffsetDateTime();
    }
}
