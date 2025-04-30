package com.example.spring_security.dto;

import com.example.spring_security.entities.Post;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record PostResponse(
        UUID id,
        String title,
        String content,
        UserResponse author,
        List<ImageResponse> images
) {
    // Conversion method
    public static PostResponse fromPost(Post post){
        List<ImageResponse> imageResponses = post.getImages() != null ?
                post.getImages().stream()
                        .map(ImageResponse::fromImage)
                        .toList() : List.of();

        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                UserResponse.fromUser(post.getAuthor()),
                imageResponses
        );
    }
}
