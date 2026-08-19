package uy.com.fulbito.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.fulbito.domain.AppUser;
import uy.com.fulbito.dto.AuthDtos.UpdateProfileRequest;
import uy.com.fulbito.dto.AuthDtos.UserResponse;
import uy.com.fulbito.error.ApiException;
import uy.com.fulbito.repository.UserRepository;

import java.util.UUID;

@Service
public class UserService {
    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
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

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
