package com.example.spring_security.dto;

public record AuthenticationResponse(
        String accessToken,
        String refreshToken,
        Long expiresAt
) {
}
