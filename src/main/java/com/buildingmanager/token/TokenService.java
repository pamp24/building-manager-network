package com.buildingmanager.token;

import com.buildingmanager.user.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TokenService {
    private final TokenRepository tokenRepository;

    public TokenService(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public void savePasswordResetToken(User user, String token) {
        Token resetToken = Token.builder()
                .token(token)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();
        tokenRepository.save(resetToken);
    }
}
