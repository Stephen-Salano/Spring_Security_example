package com.example.spring_security.dto;

import com.example.spring_security.Users.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String userName
) {
    // Static method to convert from a User to UserResponse
    public static UserResponse fromUser(User user){
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername()
        );
    }
}
