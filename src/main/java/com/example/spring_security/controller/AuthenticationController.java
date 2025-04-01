package com.example.spring_security.controller;

import com.example.spring_security.dto.AuthenticationRequest;
import com.example.spring_security.dto.AuthenticationResponse;
import com.example.spring_security.dto.RegisterRequest;
import com.example.spring_security.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request
            )
    {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate (
            @Valid @RequestBody AuthenticationRequest request
            )
    {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @RequestHeader("Authorization") String refreshToken
    )
    {
        // Extract token from header
        if (refreshToken != null && refreshToken.startsWith("Bearer ")){
            refreshToken = refreshToken.substring(7);
            return ResponseEntity.ok(authService.refreshToken(refreshToken));
        }
        return ResponseEntity.badRequest().build();
    }


}
