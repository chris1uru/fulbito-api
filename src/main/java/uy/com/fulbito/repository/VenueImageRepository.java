package uy.com.fulbito.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import uy.com.fulbito.domain.VenueImage;
import java.util.*;
public interface VenueImageRepository extends JpaRepository<VenueImage, UUID> {
    List<VenueImage> findByVenueIdOrderBySortOrder(UUID venueId);
    Optional<VenueImage> findByVenueIdAndCoverTrue(UUID venueId);
    List<VenueImage> findByVenueIdInAndCoverTrue(Collection<UUID> venueIds);
}
