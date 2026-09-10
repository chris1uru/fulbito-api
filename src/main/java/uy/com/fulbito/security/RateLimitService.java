package uy.com.fulbito.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uy.com.fulbito.error.ApiException;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    private static final int MAX_TRACKED_KEYS = 10_000;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public void check(String scope, String key, int maximum, Duration duration) {
        if (windows.size() > MAX_TRACKED_KEYS) removeExpired();
        Instant now = Instant.now();
        String bucketKey = scope + ':' + key;
        Window result = windows.compute(bucketKey, (ignored, current) -> {
            if (current == null || !now.isBefore(current.expiresAt())) {
                return new Window(1, now.plus(duration));
            }
            return new Window(current.count() + 1, current.expiresAt());
        });
        if (result.count() > maximum) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Espera unos minutos e intenta nuevamente");
        }
    }

    public void clear(String scope, String key) {
        windows.remove(scope + ':' + key);
    }

    private void removeExpired() {
        Instant now = Instant.now();
        windows.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
    }

    private record Window(int count, Instant expiresAt) {}
}
