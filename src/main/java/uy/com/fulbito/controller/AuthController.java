package uy.com.fulbito.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import uy.com.fulbito.dto.AuthDtos.*;
import uy.com.fulbito.service.AuthService;

@RestController @RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) { 
        this.service = service; 
    }

    @PostMapping("/register-player") @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerPlayer(@Valid @RequestBody RegisterRequest request) { 
        return 
        service.registerPlayer(request); 
    }

    @PostMapping("/register-owner") @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerOwner(@Valid @RequestBody RegisterRequest request) {
        return service.registerOwner(request);
    }

    @PostMapping("/login") public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return 
        service.login(request); 
    }
}
