package com.example.spring_security.service;

import com.example.spring_security.dto.ImageDetailsResponse;
import com.example.spring_security.dto.ImageResponse;
import com.example.spring_security.dto.ImageUploadRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface ImageService {

    ImageResponse createImage(UUID postId, MultipartFile file, ImageUploadRequest request) throws IOException;

    /**
     * Creates a new image with optimization prefrences
     *
     * @param postId the id of the post this image belongs to
     * @param file the uploaded image file
     * @return the created image as DTO
     * @throws IOException if file processing fails
     */
    ImageResponse createImage(UUID postId, MultipartFile file) throws IOException;

    // Get image by Id
    ImageResponse getImageById(UUID id);

    // Get all images for a post
    List<ImageResponse> getImagesByPost(UUID id);

    // Delete Image by ID
    void deleteImage(UUID id);

    ImageResponse updateImage(UUID id, MultipartFile file) throws IOException;

    /**
     * Updates the existing image with optimization prefrences
     *
     *
     * @param id the id of the image being updated
     * @param file the new image file
     * @param request Optional request containing optimization preferences
     * @return the updated image as DTO
     * @throws IOException if file processing fails
     */
    ImageResponse updateImage(UUID id, MultipartFile file, ImageUploadRequest request) throws IOException;

    ImageDetailsResponse getImageDetails(UUID id);
}
