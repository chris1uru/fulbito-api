package uy.com.fulbito.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import uy.com.fulbito.domain.Reservation;
import uy.com.fulbito.domain.enums.ReservationStatus;
import java.time.OffsetDateTime;
import java.util.*;
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    List<Reservation> findByPlayerIdOrderByStartsAtDesc(UUID playerId);
    List<Reservation> findByCourtVenueOwnerIdAndStartsAtBetweenOrderByStartsAt(UUID ownerId, OffsetDateTime from, OffsetDateTime to);
    List<Reservation> findByStartsAtBetweenOrderByStartsAt(OffsetDateTime from, OffsetDateTime to);
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
}
