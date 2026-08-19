package uy.com.fulbito.controller;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uy.com.fulbito.dto.AuthDtos.UpdateProfileRequest;
import uy.com.fulbito.dto.AuthDtos.UserResponse;
import uy.com.fulbito.security.CurrentUserService;
import uy.com.fulbito.service.AuthService;
import uy.com.fulbito.service.UserService;

@RestController @RequestMapping("/api/users")
public class UserController {

    private final CurrentUserService current;
    private final UserService users;

    public UserController(CurrentUserService current, UserService users) {
        this.current = current;
        this.users = users;
    }

    @GetMapping("/me")
    public UserResponse me(Authentication a)
    {
        return AuthService.toResponse(current.require(a));
    }

    @PutMapping("/me")
    public UserResponse updateMe(
        Authentication authentication,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        return users.updateProfile(current.require(authentication).getId(), request);
    }
}
