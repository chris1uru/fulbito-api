package uy.com.fulbito.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import uy.com.fulbito.domain.VenueLocation;
import java.util.*;
public interface VenueLocationRepository extends JpaRepository<VenueLocation, UUID> {
    Optional<VenueLocation> findByVenueId(UUID venueId);
}
