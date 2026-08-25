package uy.com.fulbito.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import uy.com.fulbito.domain.Reservation;
import uy.com.fulbito.domain.enums.ReservationStatus;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    List<Reservation> findByPlayerIdOrderByStartsAtDesc(UUID playerId, Pageable pageable);
    List<Reservation> findByCourtVenueOwnerIdAndStartsAtBetweenOrderByStartsAt(UUID ownerId, OffsetDateTime from, OffsetDateTime to);
    List<Reservation> findByStartsAtBetweenOrderByStartsAt(OffsetDateTime from, OffsetDateTime to);
    @Query("""
        select r from Reservation r
        where r.court.venue.id = :venueId
          and r.startsAt between :from and :to
        order by r.startsAt
        """)
    List<Reservation> findAgendaForAdminVenue(
        @Param("venueId") UUID venueId,
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to
    );
    @Query("""
        select r from Reservation r
        where r.court.venue.id = :venueId
          and r.court.venue.owner.id = :ownerId
          and r.startsAt between :from and :to
        order by r.startsAt
        """)
    List<Reservation> findAgendaForOwnerVenue(
        @Param("venueId") UUID venueId,
        @Param("ownerId") UUID ownerId,
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to
    );
    List<Reservation> findByCourtIdAndStatusAndEndsAtAfterAndStartsAtBefore(
        UUID courtId, ReservationStatus status, OffsetDateTime from, OffsetDateTime to
    );
    List<Reservation> findByCourtIdInAndStatusAndEndsAtAfterAndStartsAtBefore(
        Collection<UUID> courtIds,
        ReservationStatus status,
        OffsetDateTime from,
        OffsetDateTime to
    );
    Optional<Reservation> findByIdAndCourtVenueOwnerId(UUID id, UUID ownerId);
    Optional<Reservation> findByIdAndPlayerId(UUID id, UUID playerId);
    @Modifying
    @Query("""
        update Reservation r
        set r.playerNameSnapshot = 'Usuario eliminado',
            r.playerPhoneSnapshot = null,
            r.notes = null
        where r.player.id = :playerId
        """)
    int anonymizePlayer(@Param("playerId") UUID playerId);
}
