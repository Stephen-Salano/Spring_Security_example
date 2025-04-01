package com.example.spring_security.dto;

import com.example.spring_security.Users.Roles;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RegisterRequest(
        @NotBlank(message = "Fiest name cannot be blank")
        String firstName,

        @NotBlank(message = "Last name cannot be blank")
        String lastName,

        @NotBlank(message = "Username cannot be blank")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String userName,

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Please provide a valid email address")
        String email,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, message = "password must be at least 8 charactes long")
        String password,

        Set<Roles> rolesSet
) {
}
