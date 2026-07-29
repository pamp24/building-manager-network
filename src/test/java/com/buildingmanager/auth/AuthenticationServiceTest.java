package com.buildingmanager.auth;

import com.buildingmanager.buildingMember.BuildingMemberRepository;
import com.buildingmanager.buildingMember.BuildingMemberStatus;
import com.buildingmanager.email.EmailService;
import com.buildingmanager.email.EmailTemplateActivateAccount;
import com.buildingmanager.email.EmailTemplateForgotPassword;
import com.buildingmanager.exceptions.UserNotFoundException;
import com.buildingmanager.role.Role;
import org.springframework.test.util.ReflectionTestUtils;
import com.buildingmanager.role.RoleRepository;
import com.buildingmanager.security.JwtService;
import com.buildingmanager.token.Token;
import com.buildingmanager.token.TokenRepository;
import com.buildingmanager.token.TokenService;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private AuthenticationService authService;

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private TokenService tokenService;
    @Mock
    private BuildingMemberRepository buildingMemberRepository;

    @Captor
    private ArgumentCaptor<User> userCaptor;
    @Captor
    private ArgumentCaptor<Token> tokenCaptor;

    private Role userRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        authService = new AuthenticationService(
                roleRepository, passwordEncoder, userRepository,
                tokenRepository, emailService, authenticationManager,
                jwtService, tokenService, buildingMemberRepository
        );

        userRole = new Role();
        userRole.setId(1);
        userRole.setName("User");

        ReflectionTestUtils.setField(authService, "activationUrl", "http://localhost:4200/activate");

        testUser = User.builder()
                .id(1)
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .password("encoded-password")
                .enable(false)
                .accountLocked(false)
                .role(userRole)
                .build();
    }

    @Test
    void register_createsUserWithUserRoleAndSendsEmail() throws MessagingException {
        RegistrationRequest request = RegistrationRequest.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .password("password123")
                .build();

        when(roleRepository.findByName("User")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getFirstName()).isEqualTo("Test");
        assertThat(savedUser.getLastName()).isEqualTo("User");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getRole()).isEqualTo(userRole);
        assertThat(savedUser.isEnable()).isFalse();
        assertThat(savedUser.isAccountLocked()).isFalse();

        verify(tokenRepository).save(tokenCaptor.capture());
        Token savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getToken()).hasSize(6);
        assertThat(savedToken.getUser()).isEqualTo(savedUser);
        assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now());

        verify(emailService).sendEmail(
                eq("test@example.com"),
                eq("Test User"),
                eq(EmailTemplateActivateAccount.ACTIVATE_ACCOUNT),
                anyString(),
                anyString(),
                eq("Ενεργοποίηση Λογαριασμού")
        );
    }

    @Test
    void register_whenRoleNotFound_throwsException() {
        RegistrationRequest request = RegistrationRequest.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .password("password123")
                .build();

        when(roleRepository.findByName("User")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("δεν έχει αρχικοποιηθεί");
    }

    @Test
    void authenticate_returnsTokenAndUserResponse() {
        AuthenticationRequest authRequest = AuthenticationRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();
        var authToken = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(jwtService.generateToken(anyMap(), eq(testUser))).thenReturn("jwt-token-123");
        when(buildingMemberRepository.findFirstByUserIdAndStatus(
                eq(1), eq(BuildingMemberStatus.JOINED)))
                .thenReturn(Optional.empty());

        AuthenticationResponse response = authService.authenticate(authRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token-123");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUser().getRole()).isEqualTo("User");
        assertThat(response.getUser().getName()).isEqualTo("Test User");
    }

    @Test
    void activateAccount_withValidToken_activatesUser() throws MessagingException {
        Token token = Token.builder()
                .id(1)
                .token("123456")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(testUser)
                .build();

        when(tokenRepository.findByToken("123456")).thenReturn(Optional.of(token));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        authService.activateAccount("123456");

        assertThat(testUser.isEnable()).isTrue();
        verify(userRepository).save(testUser);
        verify(tokenRepository).save(token);
        assertThat(token.getValidatedAt()).isNotNull();
    }

    @Test
    void activateAccount_withInvalidToken_throwsException() {
        when(tokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.activateAccount("invalid"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid Token");
    }

    @Test
    void activateAccount_withExpiredToken_resendsEmail() throws MessagingException {
        Token token = Token.builder()
                .id(1)
                .token("999999")
                .createdAt(LocalDateTime.now().minusHours(1))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .user(testUser)
                .build();

        when(tokenRepository.findByToken("999999")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.activateAccount("999999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("has Expired");

        verify(emailService).sendEmail(anyString(), anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void sendPasswordResetToken_sendsEmailWithToken() throws MessagingException {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        authService.sendPasswordResetToken("test@example.com");

        verify(tokenService).savePasswordResetToken(eq(testUser), anyString());
        verify(emailService).sendEmail(
                eq("test@example.com"),
                eq("Test User"),
                eq(EmailTemplateForgotPassword.RESET_PASSWORD),
                contains("/auth/reset-password?token="),
                anyString(),
                eq("Επαναφορά Κωδικού Πρόσβασης")
        );
    }

    @Test
    void sendPasswordResetToken_userNotFound_throwsException() {
        when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.sendPasswordResetToken("nonexistent@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ο χρήστης δεν βρέθηκε");
    }

    @Test
    void resetPassword_withValidToken_updatesPassword() {
        Token resetToken = Token.builder()
                .id(1)
                .token("reset-token-123")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(testUser)
                .build();

        when(tokenRepository.findByToken("reset-token-123")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-encoded-password");

        authService.resetPassword("reset-token-123", "newPassword123");

        assertThat(testUser.getPassword()).isEqualTo("new-encoded-password");
        verify(userRepository).save(testUser);
        verify(tokenRepository).save(resetToken);
        assertThat(resetToken.getValidatedAt()).isNotNull();
    }

    @Test
    void resetPassword_invalidToken_throwsException() {
        when(tokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("bad-token", "newPass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Δεν είναι έγκυρο");
    }

    @Test
    void resetPassword_expiredToken_throwsException() {
        Token expiredToken = Token.builder()
                .id(1)
                .token("expired-token")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .user(testUser)
                .build();

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.resetPassword("expired-token", "newPass"))
                .isInstanceOf(com.buildingmanager.exceptions.ActivationTokenException.class)
                .hasMessageContaining("λήξει");
    }

    @Test
    void getUserByEmail_returnsUserDTO() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        var dto = authService.getUserByEmail("test@example.com");

        assertThat(dto).isNotNull();
        assertThat(dto.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void getUserByEmail_userNotFound_throwsException() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserByEmail("unknown@test.com"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User with email");
    }
}
