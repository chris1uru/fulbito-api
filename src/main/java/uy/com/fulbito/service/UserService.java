package uy.com.fulbito.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.fulbito.domain.AppUser;
import uy.com.fulbito.dto.AuthDtos.UpdateProfileRequest;
import uy.com.fulbito.dto.AuthDtos.UserResponse;
import uy.com.fulbito.dto.AuthDtos.ChangePasswordRequest;
import uy.com.fulbito.domain.enums.UserRole;
import uy.com.fulbito.domain.enums.UserStatus;
import uy.com.fulbito.repository.VenueRepository;
import uy.com.fulbito.repository.ReservationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import uy.com.fulbito.error.ApiException;
import uy.com.fulbito.repository.UserRepository;

import java.util.UUID;

@Service
public class UserService {
    private final UserRepository users;
    private final VenueRepository venues;
    private final ReservationRepository reservations;
    private final PasswordEncoder passwords;

    public UserService(UserRepository users, VenueRepository venues, ReservationRepository reservations, PasswordEncoder passwords) {
        this.users = users; this.venues = venues; this.reservations = reservations; this.passwords = passwords;
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        AppUser user = users.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(clean(request.phone()));
        return AuthService.toResponse(user);
    }

    @Transactional
    public void changePassword(AppUser user, ChangePasswordRequest request) {
        if (!passwords.matches(request.currentPassword(), user.getPasswordHash()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contrasena actual no es correcta");
        if (passwords.matches(request.newPassword(), user.getPasswordHash()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "La nueva contrasena debe ser diferente");
        user.setPasswordHash(passwords.encode(request.newPassword()));
        user.setAuthVersion(user.getAuthVersion() + 1);
        // El usuario llega desde el filtro de seguridad fuera de esta transacción; por eso puede
        // estar detached y requiere un merge explícito para persistir el cambio.
        users.save(user);
    }

    @Transactional
    public void deleteAccount(AppUser user) {
        if (user.getRole() == UserRole.ADMIN)
            throw new ApiException(HttpStatus.CONFLICT, "Una cuenta administradora debe ser desactivada por otro administrador");
        if (user.getRole() == UserRole.OWNER && !venues.findByOwnerIdOrderByName(user.getId()).isEmpty())
            throw new ApiException(HttpStatus.CONFLICT, "Antes de eliminar la cuenta debes transferir tus complejos");

        reservations.anonymizePlayer(user.getId());
        user.setEmail("deleted+" + user.getId() + "@deleted.invalid");
        user.setPasswordHash(passwords.encode(UUID.randomUUID().toString()));
        user.setFirstName("Cuenta");
        user.setLastName("eliminada");
        user.setNationalId(null);
        user.setPhone(null);
        user.setStatus(UserStatus.INACTIVE);
        user.setAuthVersion(user.getAuthVersion() + 1);
        users.save(user);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
