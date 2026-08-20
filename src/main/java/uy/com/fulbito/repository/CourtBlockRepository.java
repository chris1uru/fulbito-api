package uy.com.fulbito.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import uy.com.fulbito.domain.CourtBlock;
import java.time.OffsetDateTime;
import java.util.*;
public interface CourtBlockRepository extends JpaRepository<CourtBlock, UUID> {
    List<CourtBlock> findByCourtIdAndEndsAtAfterAndStartsAtBeforeOrderByStartsAt(UUID courtId, OffsetDateTime from, OffsetDateTime to);
    List<CourtBlock> findByCourtIdInAndEndsAtAfterAndStartsAtBefore(
        Collection<UUID> courtIds,
        OffsetDateTime from,
        OffsetDateTime to
    );
    Optional<CourtBlock> findByIdAndCourtVenueOwnerId(UUID id, UUID ownerId);
}
