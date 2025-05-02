package com.example.spring_security.service;

import com.example.spring_security.Users.Roles;
import com.example.spring_security.Users.User;
import com.example.spring_security.dto.AuthenticationRequest;
import com.example.spring_security.dto.AuthenticationResponse;
import com.example.spring_security.dto.RegisterRequest;
import com.example.spring_security.entities.RefreshToken;
import com.example.spring_security.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request){
        // setting default role if not provided
        Set<Roles> rolesSet = request.rolesSet();
        if (rolesSet == null || rolesSet.isEmpty()) {
            rolesSet = new HashSet<>();
            rolesSet.add(Roles.USER);
        }

        //create a new user
        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .userName(request.userName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .rolesSet(rolesSet)
                .build();

        // Save user
        User savedUser = userRepository.save(user);

        //Generate Tokens
        String accessToken = jwtService.generateAccessToken(user);
        // Create and store refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);

        return new AuthenticationResponse(
                accessToken, refreshToken.getToken(), System.currentTimeMillis()
                + jwtService.getAccessTokenExpiration()
        );
    }

    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest){
        // Try to authenticate with username or email
        String usernameOrEmail = authenticationRequest.userNameOrEmail();

        User user = userRepository.findByEmail(usernameOrEmail)
                .orElseGet(() -> userRepository.findByUserName(usernameOrEmail)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found")));

        // Authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(), authenticationRequest.password()
                )
        );

        // Generate Tokens
        String accessToken = jwtService.generateAccessToken(user);

        // Create and store refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthenticationResponse(
                accessToken, refreshToken.getToken(), System.currentTimeMillis() + jwtService.getAccessTokenExpiration()
        );
    }

    @Transactional
    public AuthenticationResponse refreshToken(String refreshTokenStr){
        // find the token in the database
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenStr)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        // Verifying token is not expired
        refreshTokenService.verifyExpiration(refreshToken);

        // Get user from token
        User user = refreshToken.getUser();

        // Generate new access Token
        String accessToken = jwtService.generateAccessToken(user);

        return new AuthenticationResponse(
                accessToken, refreshTokenStr, System.currentTimeMillis() + jwtService.getAccessTokenExpiration()
        );
    }

    // Add Logout functionality
    @Transactional
    public void logout(String username){
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        refreshTokenService.deleteByUserId(user);
    }
}
