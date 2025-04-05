package com.example.spring_security.dto;

import com.example.spring_security.entities.Post;

import java.util.UUID;

public record PostResponse(
        UUID id,
        String title,
        String content,
        UserResponse author
) {
    // Conversion method
    public static PostResponse fromPost(Post post){
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                UserResponse.fromUser(post.getAuthor())
        );
    }
}
