package com.example.spring_security.controller;

import com.example.spring_security.dto.AuthenticationRequest;
import com.example.spring_security.dto.AuthenticationResponse;
import com.example.spring_security.dto.RegisterRequest;
import com.example.spring_security.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpServletRequest
            )
    {
        logger.info("Registration attempt for user: {} from IP: {}", request.userName(), getClientIp(httpServletRequest));
        try{
            AuthenticationResponse response = authService.register(request);
            logger.info("User registered successfully: {}", request.userName());
            return  ResponseEntity.ok(response);
        } catch (Exception e){
            logger.error("Registration failed for user: {}: {}", request.userName(), e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate (
            @Valid @RequestBody AuthenticationRequest request,
            HttpServletRequest httpServletRequest
            )
    {
        logger.info("Authenticate attempt for: {} from IP: {}", request.userNameOrEmail() , getClientIp(httpServletRequest) );
        try{
            AuthenticationResponse response = authService.authenticate(request);
            logger.info("Authentication successful for: {}", request.userNameOrEmail());
            return ResponseEntity.ok(authService.authenticate(request));
        } catch (Exception e){
            logger.warn("Authentication failed for: {} : {}", request.userNameOrEmail(), e.getMessage());
            throw e;
        }

    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @RequestHeader("Authorization") String refreshToken,
            HttpServletRequest httpServletRequest
    )
    {
        // logger
        logger.info("Token refreh request from IP: {}", getClientIp(httpServletRequest));
        try {
            // Extract token from header
            if (refreshToken != null && refreshToken.startsWith("Bearer ")) {
                refreshToken = refreshToken.substring(7);
                AuthenticationResponse response = authService.refreshToken(refreshToken);
                logger.info("Token refreshed successfully");
                return ResponseEntity.ok(response);
            }
            logger.warn("Invalid refresh token format");
            return ResponseEntity.badRequest().build();
        } catch (Exception e){
            logger.warn("Token refresh failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Extract a client's IP address from request
    private String getClientIp(HttpServletRequest request) {
        String  xForwadedFor = request.getHeader("X-Forwarded-For");
        if (xForwadedFor != null) {
            return xForwadedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

}
