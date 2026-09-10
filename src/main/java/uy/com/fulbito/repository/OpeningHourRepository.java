package uy.com.fulbito.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import uy.com.fulbito.domain.OpeningHour;
import java.util.*;
public interface OpeningHourRepository extends JpaRepository<OpeningHour, UUID> {
    List<OpeningHour> findByVenueIdOrderByDayOfWeekAscOpensAtAsc(UUID venueId);
    List<OpeningHour> findByVenueIdInAndDayOfWeekOrderByOpensAtAsc(
        Collection<UUID> venueIds,
        short dayOfWeek
    );
}
