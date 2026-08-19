package uy.com.fulbito.dto;

import jakarta.validation.constraints.*;
import uy.com.fulbito.domain.enums.UserRole;
import uy.com.fulbito.domain.enums.UserStatus;
import java.util.UUID;

public final class AdminUserDtos {
    private AdminUserDtos() {}

    public record CreateUserRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @Pattern(regexp = "^\\+[1-9][0-9]{7,14}$") String phone,
        @NotBlank @Size(max = 20) String nationalId,
        @NotNull UserRole role
    ) {}

    public record UpdateUserStatusRequest(@NotNull UserStatus status) {}

    public record AdminUserResponse(
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
