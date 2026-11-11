package vn.datve.dat_ve.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.datve.dat_ve.auth.dto.AuthResponse;
import vn.datve.dat_ve.auth.dto.ChangePasswordRequest;
import vn.datve.dat_ve.auth.dto.LoginRequest;
import vn.datve.dat_ve.auth.dto.RegisterRequest;
import vn.datve.dat_ve.auth.dto.UpdateProfileRequest;
import vn.datve.dat_ve.auth.dto.UserResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request.email(), request.password(), request.fullName());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(CurrentUser.id(jwt));
    }

    @PutMapping("/me")
    public UserResponse updateProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(CurrentUser.id(jwt), request.fullName());
    }

    @PutMapping("/password")
    public AuthResponse changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(CurrentUser.id(jwt), request.currentPassword(), request.newPassword());
    }
}
