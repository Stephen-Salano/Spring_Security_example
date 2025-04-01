package com.example.spring_security.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthenticationRequest(
        @NotBlank(message = "Username or email is required")
        String userNameOrEmail,

        @NotBlank(message = "Password cannot be blank")
        String password
) {
}
