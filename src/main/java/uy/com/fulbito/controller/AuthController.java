package uy.com.fulbito.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import uy.com.fulbito.dto.AuthDtos.*;
import uy.com.fulbito.service.AuthService;
import uy.com.fulbito.security.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@RestController @RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;
    private final RateLimitService rateLimits;

    public AuthController(AuthService service, RateLimitService rateLimits) {
        this.service = service;
        this.rateLimits = rateLimits;
    }

    @PostMapping("/register-player") @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerPlayer(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        rateLimits.check("register", http.getRemoteAddr(), 5, Duration.ofHours(1));
        return service.registerPlayer(request);
    }

    @PostMapping("/login") public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        String key = http.getRemoteAddr() + ':' + request.email().trim().toLowerCase();
        rateLimits.check("login", key, 10, Duration.ofMinutes(15));
        AuthResponse response = service.login(request);
        rateLimits.clear("login", key);
        return response;
    }

    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(JwtAuthenticationToken authentication) {
        service.logout(authentication);
    }
}
