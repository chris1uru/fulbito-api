package uy.com.fulbito.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.com.fulbito.domain.AppUser;
import uy.com.fulbito.domain.Court;
import uy.com.fulbito.domain.OpeningHour;
import uy.com.fulbito.domain.Reservation;
import uy.com.fulbito.domain.Venue;
import uy.com.fulbito.domain.enums.PaymentStatus;
import uy.com.fulbito.domain.enums.ReservationStatus;
import uy.com.fulbito.domain.enums.UserRole;
import uy.com.fulbito.domain.enums.VenueStatus;
import uy.com.fulbito.dto.ReservationDtos.ReservationRequest;
import uy.com.fulbito.error.ApiException;
import uy.com.fulbito.repository.OpeningHourRepository;
import uy.com.fulbito.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    private static final ZoneId URUGUAY = ZoneId.of("America/Montevideo");

    @Mock private ReservationRepository reservations;
    @Mock private CourtService courts;
    @Mock private OpeningHourRepository hours;

    private ReservationService service;

    @BeforeEach
    void setUp() {
        service = new ReservationService(reservations, courts, hours);
    }

    @Test
    void newReservationSnapshotsVenueCancellationNotice() {
        Venue venue = mock(Venue.class);
        when(venue.getId()).thenReturn(UUID.randomUUID());
        when(venue.getName()).thenReturn("Complejo test");
        when(venue.getStatus()).thenReturn(VenueStatus.ACTIVE);
        when(venue.getCancellationNoticeHours()).thenReturn((short) 12);

        Court court = mock(Court.class);
        when(court.getId()).thenReturn(UUID.randomUUID());
        when(court.getName()).thenReturn("Cancha test");
        when(court.getVenue()).thenReturn(venue);
        when(court.isActive()).thenReturn(true);
        when(court.getSlotMinutes()).thenReturn((short) 60);
        when(court.getPricePerSlot()).thenReturn(BigDecimal.valueOf(1800));

        AppUser admin = mock(AppUser.class);
        when(admin.getRole()).thenReturn(UserRole.ADMIN);

        OffsetDateTime start = OffsetDateTime.now(URUGUAY).plusDays(30)
            .withHour(19).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime end = start.plusHours(1);
        OpeningHour opening = mock(OpeningHour.class);
        when(opening.getDayOfWeek()).thenReturn((short) start.getDayOfWeek().getValue());
        when(opening.getOpensAt()).thenReturn(LocalTime.of(16, 0));
        when(opening.getClosesAt()).thenReturn(LocalTime.of(23, 0));

        when(courts.get(court.getId())).thenReturn(court);
        when(hours.findByVenueIdOrderByDayOfWeekAscOpensAtAsc(venue.getId()))
            .thenReturn(List.of(opening));
        when(reservations.saveAndFlush(any(Reservation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(admin, new ReservationRequest(
            court.getId(), start, end, "Reserva por WhatsApp", "+59899000000", "Test"
        ));

        assertEquals(12, result.cancellationNoticeHours());
        assertEquals(start.minusHours(12), result.cancellationDeadline());
        assertEquals(PaymentStatus.PENDING, result.paymentStatus());
        assertEquals("Reserva por WhatsApp", result.playerName());
    }

    @Test
    void paidReservationCannotBeCancelled() {
        UUID reservationId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        AppUser player = mock(AppUser.class);
        when(player.getId()).thenReturn(playerId);

        Reservation reservation = mock(Reservation.class);
        when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);
        when(reservation.getPaymentStatus()).thenReturn(PaymentStatus.PAID);
        when(reservations.findByIdAndPlayerId(reservationId, playerId))
            .thenReturn(Optional.of(reservation));

        ApiException error = assertThrows(
            ApiException.class,
            () -> service.cancelByPlayer(reservationId, player)
        );

        assertEquals("No se puede cancelar una reserva marcada como paga", error.getMessage());
        verify(reservation, never()).setStatus(any());
    }
}
