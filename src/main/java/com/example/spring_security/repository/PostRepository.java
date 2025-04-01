package com.example.spring_security.repository;

import com.example.spring_security.Users.User;
import com.example.spring_security.entities.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {


    // Find all posts by author's username
    // Note: Changed from Optional<Post> to List<Post> since one author can have multiple posts
    @Query("SELECT p FROM Post p JOIN p.author u WHERE u.userName = :userName")
    List<Post> findByAuthorUsername(String userName);

    // Find the autor of a specific post
    @Query("SELECT p.author FROM Post p WHERE p.id = :postId")
    Optional<User> findAuthorByPostId(@Param("postId") UUID postID);
}
