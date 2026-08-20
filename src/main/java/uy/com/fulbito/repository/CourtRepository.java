package uy.com.fulbito.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.com.fulbito.domain.Court;
import uy.com.fulbito.domain.enums.VenueStatus;
import java.util.*;
public interface CourtRepository extends JpaRepository<Court, UUID> {
    List<Court> findByVenueIdOrderByName(UUID venueId);
    List<Court> findByVenueIdAndActiveTrueAndVenueStatusOrderByName(UUID venueId, uy.com.fulbito.domain.enums.VenueStatus status);
    Optional<Court> findByIdAndVenueOwnerId(UUID id, UUID ownerId);

    @Query("""
        select c from Court c
        join fetch c.venue v
        where c.active = true and v.status = :status
        order by v.name asc, c.name asc
        """)
    List<Court> findPublicActive(@Param("status") VenueStatus status);
}
