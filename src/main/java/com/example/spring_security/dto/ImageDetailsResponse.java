package com.example.spring_security.dto;

import com.example.spring_security.entities.Image;

import javax.xml.transform.sax.SAXResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record ImageDetailsResponse(
        UUID id,
        String fileName,
        String filetype,

        // original file info
        String originalUrl,
        long originalSize,

        // Optimized file info (if availlable)
        String optimizedUrl,
        long optimizedSize,

        // Optimization status and metrics
        boolean optimized,
        double compressionRatio,
        long bytesSaved,

        // Standard metadata
        LocalDateTime uplodadedAt,
        LocalDateTime lastModified,
        UUID postId
) {
    public static ImageDetailsResponse fromImage(Image image){
        // parse size
        long originalSize = 0;
        try{
            originalSize = Long.parseLong(image.getOriginalFileSize());
        }catch (NumberFormatException e){
            throw new NumberFormatException("Long Parsing failed");
        }
        long optimizedSize = image.getFileSize();

        // calculate optimization metrics
        double compressionratio = originalSize > 0 ? (double) originalSize / (double) Math.max(1, optimizedSize): 1.0;

        long bytesSaved = originalSize - optimizedSize;

        return new ImageDetailsResponse(
                image.getId(),
                image.getFileName(),
                image.getFileType(),
                image.getOriginalFilePath(),
                originalSize,
                image.getFilePath(),
                optimizedSize,
                image.isOptimized(),
                compressionratio,
                Math.max(0, bytesSaved), // ensures we don't report negative savings
                image.getUploadedAt(),
                image.getUploadedAt(), // Using uploadedAt as last modified for now
                image.getPost() != null ? image.getPost().getId() : null
        );
    }
}
