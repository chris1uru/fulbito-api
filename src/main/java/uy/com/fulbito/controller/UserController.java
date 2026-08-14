package uy.com.fulbito.controller;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uy.com.fulbito.dto.AuthDtos.UserResponse;
import uy.com.fulbito.security.CurrentUserService;
import uy.com.fulbito.service.AuthService;

@RestController @RequestMapping("/api/users")
public class UserController {

    private final CurrentUserService current;public UserController(CurrentUserService current)
    {this.current=current;}

    @GetMapping("/me")
    public UserResponse me(Authentication a)
    {
        return AuthService.toResponse(current.require(a));
    }
}
