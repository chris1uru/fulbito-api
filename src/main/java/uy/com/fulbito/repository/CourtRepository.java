package uy.com.fulbito.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import uy.com.fulbito.domain.Court;
import java.util.*;
public interface CourtRepository extends JpaRepository<Court, UUID> {
    List<Court> findByVenueIdOrderByName(UUID venueId);
    List<Court> findByVenueIdAndActiveTrueAndVenueStatusOrderByName(UUID venueId, uy.com.fulbito.domain.enums.VenueStatus status);
    Optional<Court> findByIdAndVenueOwnerId(UUID id, UUID ownerId);
}
