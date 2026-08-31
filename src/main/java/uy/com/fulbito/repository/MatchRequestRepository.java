package uy.com.fulbito.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import uy.com.fulbito.domain.MatchRequest;
import uy.com.fulbito.domain.enums.MatchRequestStatus;
import java.time.OffsetDateTime;
import java.util.*;

public interface MatchRequestRepository extends JpaRepository<MatchRequest, UUID> {
    List<MatchRequest> findByStatusAndExpiresAtAfterOrderByCreatedAtDesc(MatchRequestStatus status, OffsetDateTime now, Pageable pageable);
    List<MatchRequest> findByCreatorIdOrderByCreatedAtDesc(UUID creatorId, Pageable pageable);
    Optional<MatchRequest> findByIdAndCreatorId(UUID id, UUID creatorId);
    boolean existsByReservationIdAndStatus(UUID reservationId, MatchRequestStatus status);

    @Modifying
    @Query("update MatchRequest m set m.status = :expired where m.status = :open and m.expiresAt <= :now")
    int expireOld(@Param("now") OffsetDateTime now, @Param("open") MatchRequestStatus open, @Param("expired") MatchRequestStatus expired);
}
