package com.example.spring_security.service;

import com.example.spring_security.dto.ImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface ImageService {
    // creat a new Image
    ImageResponse createImage(UUID postId, MultipartFile file) throws IOException;

    // Get image by Id
    ImageResponse getImageById(UUID id);

    // Get all images for a post
    List<ImageResponse> getImagesByPost(UUID id);

    // Delete Image by ID
    void deleteImage(UUID id);

    // Update image details
    ImageResponse updateImage(UUID id, MultipartFile file) throws IOException;
}
