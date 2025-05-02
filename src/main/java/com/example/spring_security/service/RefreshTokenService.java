package com.example.spring_security.service;

import com.example.spring_security.Users.User;
import com.example.spring_security.entities.RefreshToken;
import com.example.spring_security.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public RefreshToken createRefreshToken(User user){
        // First delete any existing refresh tokens for that user
        refreshTokenRepository.deleteByUser(user);

        // Generate JWT token string
        String tokenString = jwtService.generateRefreshToken(user);

        // Create and save the token entity
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenString)
                .expiryDate(Instant.now().plusMillis(refreshTokenExpiration))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token){
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token){
        if (token.isExpired()){
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token is expired. Please login again");
        }
        return token;
    }

    @Transactional
    public void deleteByUserId(User user){
        refreshTokenRepository.deleteByUser(user);
    }
}
