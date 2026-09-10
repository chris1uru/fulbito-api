package uy.com.fulbito.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import uy.com.fulbito.domain.RevokedToken;
import java.time.Instant;
import java.util.UUID;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, UUID> {
    @Modifying
    @Query("delete from RevokedToken token where token.expiresAt < :now")
    int deleteExpired(Instant now);
}
