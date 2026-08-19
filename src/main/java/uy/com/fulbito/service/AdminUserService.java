package uy.com.fulbito.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.fulbito.domain.AppUser;
import uy.com.fulbito.domain.enums.*;
import uy.com.fulbito.dto.AdminUserDtos.*;
import uy.com.fulbito.error.ApiException;
import uy.com.fulbito.repository.UserRepository;
import java.util.List;
import java.util.UUID;

@Service
public class AdminUserService {
    private final UserRepository users;
    private final PasswordEncoder passwords;

    public AdminUserService(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> search(String query, UserRole role) {
        UserRole searchableRole = role == null ? UserRole.OWNER : role;
        if (searchableRole == UserRole.ADMIN)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Los administradores no se gestionan desde la app");
        String cleanQuery = query == null ? "" : query.trim();
        String nationalId = digits(cleanQuery);
        return users.searchByRole(searchableRole, cleanQuery, nationalId, PageRequest.of(0, 50))
            .stream().map(AdminUserService::response).toList();
    }

    @Transactional
    public AdminUserResponse create(CreateUserRequest request) {
        if (request.role() == UserRole.ADMIN)
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se pueden crear administradores desde la app");

        String email = request.email().trim().toLowerCase();
        String nationalId = normalizeNationalId(request.nationalId());
        if (users.existsByEmailIgnoreCase(email))
            throw new ApiException(HttpStatus.CONFLICT, "El email ya esta registrado");
        if (users.existsByNationalId(nationalId))
            throw new ApiException(HttpStatus.CONFLICT, "La cedula ya esta registrada");

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setNationalId(nationalId);
        user.setPhone(clean(request.phone()));
        user.setRole(request.role());
        user.setStatus(UserStatus.ACTIVE);
        return response(users.save(user));
    }

    @Transactional
    public AdminUserResponse updateStatus(UUID id, UpdateUserStatusRequest request) {
        AppUser user = users.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (user.getRole() == UserRole.ADMIN)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Los administradores no se gestionan desde la app");
        user.setStatus(request.status());
        return response(user);
    }

    public AppUser requireActiveOwner(UUID ownerId) {
        AppUser owner = users.findById(ownerId)
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Responsable no encontrado"));
        if (owner.getRole() != UserRole.OWNER)
            throw new ApiException(HttpStatus.BAD_REQUEST, "El responsable debe tener rol OWNER");
        if (owner.getStatus() != UserStatus.ACTIVE)
            throw new ApiException(HttpStatus.BAD_REQUEST, "El responsable debe estar activo");
        return owner;
    }

    private static String normalizeNationalId(String value) {
        String normalized = digits(value);
        if (normalized.length() < 7 || normalized.length() > 8)
            throw new ApiException(HttpStatus.BAD_REQUEST, "La cedula debe tener 7 u 8 digitos");
        return normalized;
    }

    private static String digits(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static AdminUserResponse response(AppUser user) {
        return new AdminUserResponse(
            user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
            user.getNationalId(), user.getPhone(), user.getRole(), user.getStatus()
        );
    }
}
