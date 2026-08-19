package uy.com.fulbito.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import uy.com.fulbito.domain.AppUser;
import uy.com.fulbito.domain.enums.UserRole;
import java.util.*;
public interface UserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    Optional<AppUser> findByNationalId(String nationalId);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByNationalId(String nationalId);

    @Query("""
        select u from AppUser u
        where u.role = :role
          and (
            :query = ''
            or lower(u.email) like lower(concat('%', :query, '%'))
            or lower(concat(u.firstName, ' ', u.lastName)) like lower(concat('%', :query, '%'))
            or (:nationalId <> '' and u.nationalId like concat('%', :nationalId, '%'))
          )
        order by u.firstName, u.lastName
        """)
    List<AppUser> searchByRole(
        @Param("role") UserRole role,
        @Param("query") String query,
        @Param("nationalId") String nationalId,
        Pageable pageable
    );
}
