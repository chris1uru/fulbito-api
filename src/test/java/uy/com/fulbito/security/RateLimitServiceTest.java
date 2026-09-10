package uy.com.fulbito.security;

import org.junit.jupiter.api.Test;
import uy.com.fulbito.error.ApiException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimitServiceTest {
    @Test
    void blocksRequestsBeyondTheConfiguredLimit() {
        RateLimitService service = new RateLimitService();

        assertDoesNotThrow(() -> service.check("login", "client", 2, Duration.ofMinutes(1)));
        assertDoesNotThrow(() -> service.check("login", "client", 2, Duration.ofMinutes(1)));
        assertThrows(ApiException.class, () -> service.check("login", "client", 2, Duration.ofMinutes(1)));
    }

    @Test
    void clearAllowsAValidLoginAfterPreviousAttempts() {
        RateLimitService service = new RateLimitService();
        service.check("login", "client", 1, Duration.ofMinutes(1));
        service.clear("login", "client");
        assertDoesNotThrow(() -> service.check("login", "client", 1, Duration.ofMinutes(1)));
    }
}
