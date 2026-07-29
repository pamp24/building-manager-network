package com.buildingmanager.auth;

import com.buildingmanager.token.TokenRequest;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import com.buildingmanager.user.UserResponse;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Timed(value = "auth.register", description = "User registration")
    public ResponseEntity<?> register(
            @RequestBody @Valid RegistrationRequest request
    ) throws MessagingException {
        log.info("Registering user with email: {}", request.getEmail());
        authenticationService.register(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/authenticate")
    @Timed(value = "auth.authenticate", description = "User authentication")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request
    ){
        log.debug("Authentication attempt for email: {}", request.getEmail());
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @PostMapping("/activate-account")
    @Timed(value = "auth.activate", description = "Account activation")
    public ResponseEntity<?> confirm(
            @RequestBody TokenRequest request
    ) throws MessagingException {
        authenticationService.activateAccount(request.getToken());
        return ResponseEntity.ok().build();
    }
    @PostMapping("/forgot-password")
    @Timed(value = "auth.forgot-password", description = "Forgot password request")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) throws MessagingException {
        authenticationService.sendPasswordResetToken(request.getEmail());
        log.info("Password reset email sent to: {}", request.getEmail());
        return ResponseEntity.ok("Email επαναφοράς στάλθηκε");
    }
    @PostMapping("/reset-password")
    @Timed(value = "auth.reset-password", description = "Password reset")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(request.getToken(), request.getNewPassword());
        log.info("Password reset successful");
        return ResponseEntity.ok("Password reset successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.authentication.BadCredentialsException("User is not authenticated");
        }

        User user = (User) authentication.getPrincipal();

        UserResponse response = UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .name(user.fullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .profileImageUrl(user.getProfileImageUrl())
                .professionalsFavoritesOnly(user.isProfessionalsFavoritesOnly())
                .build();

        return ResponseEntity.ok(response);
    }


}
