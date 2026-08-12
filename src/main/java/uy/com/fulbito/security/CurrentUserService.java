package uy.com.fulbito.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import uy.com.fulbito.domain.AppUser;
import uy.com.fulbito.error.ApiException;
import uy.com.fulbito.repository.UserRepository;
import java.util.UUID;

@Service
public class CurrentUserService {
    private final UserRepository users;
    public CurrentUserService(UserRepository users) { this.users = users; }
    public AppUser require(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Se requiere autenticacion");
        try {
            return users.findById(UUID.fromString(authentication.getName()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario del token no encontrado"));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token invalido");
        }
    }
}
