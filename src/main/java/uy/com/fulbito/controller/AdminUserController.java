package uy.com.fulbito.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import uy.com.fulbito.domain.enums.UserRole;
import uy.com.fulbito.dto.AdminUserDtos.*;
import uy.com.fulbito.service.AdminUserService;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminUserController {
    private final AdminUserService service;

    public AdminUserController(AdminUserService service) { this.service = service; }

    @GetMapping
    public List<AdminUserResponse> search(
        @RequestParam(defaultValue = "") @jakarta.validation.constraints.Size(max = 100) String query,
        @RequestParam(required = false) UserRole role
    ) {
        return service.search(query, role);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{id}/status")
    public AdminUserResponse updateStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return service.updateStatus(id, request);
    }
}
