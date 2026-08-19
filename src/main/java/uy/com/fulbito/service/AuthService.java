package uy.com.fulbito.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.fulbito.domain.AppUser;
import uy.com.fulbito.domain.enums.*;
import uy.com.fulbito.dto.AuthDtos.*;
import uy.com.fulbito.error.ApiException;
import uy.com.fulbito.repository.UserRepository;
import java.time.*;
import java.util.List;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder jwtEncoder;
    private final Duration expiration;

    public AuthService(UserRepository users, PasswordEncoder passwords, JwtEncoder jwtEncoder,
                       @Value("${app.jwt.expiration}") Duration expiration) {
        this.users = users; this.passwords = passwords; this.jwtEncoder = jwtEncoder; this.expiration = expiration;
    }

    @Transactional
    public AuthResponse registerPlayer(RegisterRequest request) { return register(request, UserRole.PLAYER); }

    @Transactional
    private AuthResponse register(RegisterRequest request, UserRole role) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw new ApiException(HttpStatus.CONFLICT, "El email ya esta registrado");
        AppUser user = new AppUser();
        user.setEmail(email); user.setPasswordHash(passwords.encode(request.password()));
        user.setFirstName(request.firstName().trim()); user.setLastName(request.lastName().trim());
        user.setPhone(request.phone()); user.setRole(role); user.setStatus(UserStatus.ACTIVE);
        return response(users.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = users.findByEmailIgnoreCase(request.email().trim())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email o contrasena incorrectos"));
        if (!passwords.matches(request.password(), user.getPasswordHash()))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email o contrasena incorrectos");
        if (user.getStatus() != UserStatus.ACTIVE)
            throw new ApiException(HttpStatus.FORBIDDEN, "La cuenta no esta activa");
        return response(user);
    }

    private AuthResponse response(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("fulbito-api").issuedAt(now)
            .expiresAt(now.plus(expiration)).subject(user.getId().toString())
            .claim("roles", List.of("ROLE_" + user.getRole().name())).build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", expiration.toSeconds(), toResponse(user));
    }

    public static UserResponse toResponse(AppUser u) {
        return new UserResponse(
            u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getNationalId(),
            u.getPhone(), u.getRole(), u.getStatus()
        );
    }
}
