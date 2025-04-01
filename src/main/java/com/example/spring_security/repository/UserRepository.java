package com.example.spring_security.repository;

import com.example.spring_security.Users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // finding user by email
    Optional<User> findByEmail(String email);

    Optional<User> findByUserName(String userName);
}
