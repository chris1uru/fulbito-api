package uy.com.fulbito.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.com.fulbito.domain.MatchRequestAvailability;
import java.util.*;

public interface MatchRequestAvailabilityRepository extends JpaRepository<MatchRequestAvailability, UUID> {
    List<MatchRequestAvailability> findByMatchRequestIdOrderByStartsAt(UUID matchRequestId);
    List<MatchRequestAvailability> findByMatchRequestIdInOrderByStartsAt(Collection<UUID> matchRequestIds);
}
