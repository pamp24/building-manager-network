package com.buildingmanager.auth;


import com.buildingmanager.email.EmailService;
import com.buildingmanager.email.EmailTemplateActivateAccount;
import com.buildingmanager.email.EmailTemplateForgotPassword;
import com.buildingmanager.role.RoleRepository;
import com.buildingmanager.security.JwtService;
import com.buildingmanager.token.Token;
import com.buildingmanager.token.TokenRepository;
import com.buildingmanager.token.TokenService;
import com.buildingmanager.user.*;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    @Value("${spring.application.mailing.frontend.activation-url}")
    private String activationUrl;
    private final TokenService tokenService;
    public void register(RegistrationRequest request) throws MessagingException {
        var userRole = roleRepository.findByName("User")
                //todo - better exception handling
                .orElseThrow(()-> new IllegalStateException("Ο ρόλος δεν έχει αρχικοποιηθεί"));
        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .accountLocked(false)
                .enable(false)
                .roles(List.of(userRole))
                .build();
        userRepository.save(user);
        sendValidationEmail(user);
    }

    private void sendValidationEmail(User user) throws MessagingException {
        var newToken = generateAndSaveActivationToken(user);

        emailService.sendEmail(
                user.getEmail(),
                user.fullName(),
                EmailTemplateActivateAccount.ACTIVATE_ACCOUNT,
                activationUrl,
                newToken,
                "Ενεργοποίηση Λογαριασμού"
        );

    }

    private String generateAndSaveActivationToken(User user) {
        //generate a token
        String generatedToken = generateActivationCode(6);
        var token = Token.builder()
                .token(generatedToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();
        tokenRepository.save(token);

        return generatedToken;
    }

    private String generateActivationCode(int length) {
        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();
        for(int i = 0; i < length; i++){
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }
        return codeBuilder.toString();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest) {

        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authenticationRequest.getEmail(),
                        authenticationRequest.getPassword()
                )
        );
        var claims = new HashMap<String, Object>();
        var user = ((User) auth.getPrincipal());
        claims.put("fullName", user.fullName());

        // Find main role
        String mainRole = user.getRoles().stream()
                .findFirst()
                .map(role -> role.getName())
                .orElse("USER"); // default if no role found
        var jwtToken = jwtService.generateToken(claims, user);
        // Create UserResponse object
        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .name(user.fullName())
                .role(mainRole)
                .build();

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .user(userResponse)
                .build();
    }


    public void activateAccount(String token) throws MessagingException {
        Token savedToken = tokenRepository.findByToken(token)
                //todo exception has to be defined
                .orElseThrow(() -> new RuntimeException("Invalid Token"));
        if(LocalDateTime.now().isAfter(savedToken.getExpiresAt())){
            sendValidationEmail(savedToken.getUser());
            throw new RuntimeException("Activation Token has Expired. A new token has been send to the same email Address");
        }
        var user = userRepository.findById(savedToken.getUser().getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not Found"));
        user.setEnable(true);
        userRepository.save(user);
        savedToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(savedToken);
    }

    public void sendPasswordResetToken(String email) throws MessagingException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Ο χρήστης δεν βρέθηκε."));

        String token = UUID.randomUUID().toString();
        tokenService.savePasswordResetToken(user, token);

        String resetUrl = "http://localhost:4200/auth/reset-password?token=" + token;

        try {
            emailService.sendEmail(
                    user.getEmail(),
                    user.fullName(),
                    EmailTemplateForgotPassword.RESET_PASSWORD,
                    resetUrl,
                    token,
                    "Επαναφορά Κωδικού Πρόσβασης"
            );
        } catch (MessagingException e) {
            // Καταγραφή του σφάλματος πριν το πετάξεις
            System.err.println("Σφάλμα αποστολής email: " + e.getMessage());
            throw e;
        }
    }
    public void resetPassword(String token, String newPassword) {
        Token resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Δεν είναι έγκυρο!"));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Ληξή");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(resetToken);
    }
}
