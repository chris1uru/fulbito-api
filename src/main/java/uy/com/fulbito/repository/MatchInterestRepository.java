package uy.com.fulbito.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.com.fulbito.domain.MatchInterest;
import java.util.*;

public interface MatchInterestRepository extends JpaRepository<MatchInterest, UUID> {
    boolean existsByMatchRequestIdAndPlayerId(UUID matchRequestId, UUID playerId);
    List<MatchInterest> findByMatchRequestIdOrderByCreatedAt(UUID matchRequestId);
    List<MatchInterest> findByPlayerIdAndMatchRequestIdIn(UUID playerId, Collection<UUID> matchRequestIds);

    @Query("""
        select i.matchRequest.id, count(i)
        from MatchInterest i
        where i.matchRequest.id in :requestIds
        group by i.matchRequest.id
        """)
    List<Object[]> countByRequestIds(@Param("requestIds") Collection<UUID> requestIds);
}
