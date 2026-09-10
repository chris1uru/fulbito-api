package uy.com.fulbito.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.com.fulbito.domain.AppUser;
import uy.com.fulbito.domain.MatchRequest;
import uy.com.fulbito.domain.enums.*;
import uy.com.fulbito.dto.MatchRequestDtos.AvailabilityRequest;
import uy.com.fulbito.dto.MatchRequestDtos.CreateMatchRequestRequest;
import uy.com.fulbito.error.ApiException;
import uy.com.fulbito.repository.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchRequestServiceTest {
    private static final ZoneId URUGUAY = ZoneId.of("America/Montevideo");

    @Mock private MatchRequestRepository requests;
    @Mock private MatchRequestAvailabilityRepository availabilities;
    @Mock private MatchInterestRepository interests;
    @Mock private ReservationRepository reservations;

    private MatchRequestService service;

    @BeforeEach
    void setUp() {
        service = new MatchRequestService(requests, availabilities, interests, reservations);
    }

    @Test
    void rejectsOverlappingAvailabilityWindows() {
        AppUser player = player("+59899000000");
        OffsetDateTime start = OffsetDateTime.now(URUGUAY).plusDays(2)
            .withHour(18).withMinute(0).withSecond(0).withNano(0);
        var body = new CreateMatchRequestRequest(
            FootballFormat.FIVE,
            MatchStyle.RECREATIONAL,
            null,
            null,
            List.of(
                new AvailabilityRequest(start, start.plusHours(3)),
                new AvailabilityRequest(start.plusHours(2), start.plusHours(4))
            )
        );

        ApiException error = assertThrows(ApiException.class, () -> service.create(player, body));

        assertEquals("Las franjas disponibles no pueden superponerse", error.getMessage());
        verify(requests, never()).saveAndFlush(any());
    }

    @Test
    void rejectsInterestInOwnRequest() {
        UUID playerId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        AppUser player = player("+59899000000");
        when(player.getId()).thenReturn(playerId);
        MatchRequest request = mock(MatchRequest.class);
        when(request.getStatus()).thenReturn(MatchRequestStatus.OPEN);
        when(request.getExpiresAt()).thenReturn(OffsetDateTime.now().plusDays(1));
        when(request.getCreator()).thenReturn(player);
        when(requests.findById(requestId)).thenReturn(Optional.of(request));

        ApiException error = assertThrows(
            ApiException.class,
            () -> service.expressInterest(requestId, player)
        );

        assertEquals("No podés postularte a tu propia búsqueda", error.getMessage());
        verify(interests, never()).saveAndFlush(any());
    }

    @Test
    void requiresPhoneBeforePublishing() {
        AppUser player = player(null);
        var body = new CreateMatchRequestRequest(
            FootballFormat.FIVE,
            MatchStyle.RECREATIONAL,
            null,
            null,
            List.of()
        );

        ApiException error = assertThrows(ApiException.class, () -> service.create(player, body));

        assertEquals("Agregá un teléfono a tu perfil para usar Buscar rival", error.getMessage());
        verifyNoInteractions(requests, availabilities, interests, reservations);
    }

    private static AppUser player(String phone) {
        AppUser player = mock(AppUser.class);
        when(player.getPhone()).thenReturn(phone);
        return player;
    }
}
