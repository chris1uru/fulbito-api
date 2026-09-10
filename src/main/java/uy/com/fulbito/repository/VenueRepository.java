package uy.com.fulbito.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import uy.com.fulbito.domain.Venue;
import uy.com.fulbito.domain.enums.VenueStatus;
import java.util.*;
public interface VenueRepository extends JpaRepository<Venue, UUID> {
    List<Venue> findAllByOrderByNameAsc();
    List<Venue> findByOwnerIdOrderByName(UUID ownerId);
    List<Venue> findByStatusOrderByName(VenueStatus status);
    Optional<Venue> findByIdAndOwnerId(UUID id, UUID ownerId);
}
