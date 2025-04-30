package com.example.spring_security.controller;

import com.example.spring_security.dto.ImageResponse;
import com.example.spring_security.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/images")
@RequiredArgsConstructor
public class ImageController {
    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);
    private final ImageService imageService;

    // create a new image (authenticated users only)
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ImageResponse> createImage(
            @RequestParam("postId") UUID postId,
            @RequestParam("file")MultipartFile file
            ){
        logger.info("Creating a new image entry{}", file.getOriginalFilename());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(imageService.createImage(postId, file));
    }

    // Get image by ID (public endpoint)
    @GetMapping("/{id}")
    public ResponseEntity<ImageResponse> getImageById(@PathVariable UUID id){
        logger.info("Fetching image with ID: {}", id);
        return ResponseEntity.ok(imageService.getImageById(id));
    }

    // Get all image for a post (public endpoint)
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<ImageResponse>> getImageByPostId(@PathVariable UUID postId){
        logger.info("Fetching all images for post with ID: {}", postId);
        return ResponseEntity.ok(imageService.getImagesByPost(postId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletImage(@PathVariable UUID id){
        imageService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ImageResponse> updateImage(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file
    ){
        return ResponseEntity.ok(imageService.updateImage(id, file));

    }
}
