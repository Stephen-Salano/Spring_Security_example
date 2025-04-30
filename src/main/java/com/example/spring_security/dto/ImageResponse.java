package com.example.spring_security.dto;

import com.example.spring_security.entities.Image;

import java.time.LocalDateTime;
import java.util.UUID;

public record ImageResponse(
        UUID id,
        String fileName,
        String fileType,
        Long fileSize,
        String filePath,
        LocalDateTime uploadedAt,
        UUID postId
) {
    // conversion method from Image entity to DTO
    public static ImageResponse fromImage(Image image){
        return new ImageResponse(
                image.getId(),
                image.getFileName(),
                image.getFileType(),
                image.getFileSize(),
                image.getFilePath(),
                image.getUploadedAt(),
                image.getPost() != null ? image.getPost().getId() : null
        );
    }
}
