package uy.com.fulbito.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import uy.com.fulbito.domain.CourtImage;
import java.util.*;
public interface CourtImageRepository extends JpaRepository<CourtImage, UUID> {
    List<CourtImage> findByCourtIdOrderBySortOrder(UUID courtId);
}
