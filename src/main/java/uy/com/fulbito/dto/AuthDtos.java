package uy.com.fulbito.dto;

import jakarta.validation.constraints.*;
import uy.com.fulbito.domain.enums.UserRole;
import uy.com.fulbito.domain.enums.UserStatus;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}
    public record RegisterRequest(
        @NotBlank @Size(max=80) String firstName,
        @NotBlank @Size(max=80) String lastName,
        @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(min=10, max=72) String password,
        @Pattern(regexp="^\\+[1-9][0-9]{7,14}$") String phone
    ) {}
    public record LoginRequest(
        @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(max=72) String password
    ) {}
    public record ChangePasswordRequest(
        @NotBlank @Size(max=72) String currentPassword,
        @NotBlank @Size(min=10, max=72) String newPassword
    ) {}
    public record UpdateProfileRequest(
        @NotBlank @Size(max=80) String firstName,
        @NotBlank @Size(max=80) String lastName,
        @Pattern(regexp="^\\+[1-9][0-9]{7,14}$") String phone
    ) {}
    public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {}
    public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String nationalId,
        String phone,
        UserRole role,
        UserStatus status
    ) {}
}
