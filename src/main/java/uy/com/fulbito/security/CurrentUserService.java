package uy.com.fulbito.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import uy.com.fulbito.domain.AppUser;
import uy.com.fulbito.domain.enums.UserStatus;
import uy.com.fulbito.error.ApiException;
import uy.com.fulbito.repository.UserRepository;
import uy.com.fulbito.repository.RevokedTokenRepository;
import java.util.UUID;

@Service
public class CurrentUserService {
    private final UserRepository users;
    private final RevokedTokenRepository revokedTokens;
    public CurrentUserService(UserRepository users, RevokedTokenRepository revokedTokens) {
        this.users = users; this.revokedTokens = revokedTokens;
    }
    public AppUser require(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Se requiere autenticacion");
        try {
            AppUser user = users.findById(UUID.fromString(authentication.getName()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario del token no encontrado"));
            if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication))
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Token invalido");
            String tokenId = jwtAuthentication.getToken().getId();
            Number tokenVersion = jwtAuthentication.getToken().getClaim("ver");
            if (tokenId == null || revokedTokens.existsById(UUID.fromString(tokenId))
                || tokenVersion == null || tokenVersion.intValue() != user.getAuthVersion())
                throw new ApiException(HttpStatus.UNAUTHORIZED, "La sesion ya no es valida");
            if (user.getStatus() != UserStatus.ACTIVE)
                throw new ApiException(HttpStatus.FORBIDDEN, "La cuenta no esta activa");
            return user;
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token invalido");
        }
    }
}
