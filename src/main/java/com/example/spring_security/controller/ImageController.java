package com.example.spring_security.controller;

import com.example.spring_security.dto.ImageDetailsResponse;
import com.example.spring_security.dto.ImageResponse;
import com.example.spring_security.dto.ImageUploadRequest;
import com.example.spring_security.service.FileStorageService;
import com.example.spring_security.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/images")
@RequiredArgsConstructor
public class ImageController {
    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);
    private final ImageService imageService;
    private final FileStorageService fileStorageService;

    // create a new image (authenticated users only)
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ImageResponse> createImage(
            @RequestParam(value = "request", required = false)ImageUploadRequest request,
            @RequestParam("file") MultipartFile file
            )throws IOException{
        UUID postId = null;

        // Extract postID from Request if available, otherwise try and get from param
        if (request != null && request.postId() != null){
            try{
                postId = UUID.fromString(request.postId());
            } catch (IllegalArgumentException e){
                throw new IllegalArgumentException("Invalid post ID format");
            }
        }
        if (postId == null){
            throw new IllegalArgumentException("Post Id is required");
        }

        logger.info("Creating a new file image entry: {} with optimized preferences: {}",
                file.getOriginalFilename(), request != null ? request:"default");

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(imageService.createImage(postId, file, request));
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
            @RequestPart(value = "request", required = false) ImageUploadRequest request,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        return ResponseEntity.ok(imageService.updateImage(id, file, request));

    }

    /**
     * When someone (browser, frontend app, API) wants to see or download the actual image file
     *      not just it's metadata, they need an endpoint like `GET /api/v1/images/files/some-image-id.png`
     *      This endpoint will return the file contents-- just like when you visit a URL that shows a photo
     *      or downloads an image
     * @param filename whatever value is passed in the file name part of the URL, capture it and pass it as
     *                 a String argument
     * @return Response entity that is the image
     *
     * This method makes the image file viewable and downloadable via URL
     */
    @GetMapping("/files/{filename:.+}") // allows dots in the path variable
    public ResponseEntity<Resource> serveFile(@PathVariable String filename){
        logger.info("Attempting to download file{}" , filename);
        /**
         * Go to disk and load this file as a Spring `Resource`(basically something you can send back in a HTTP response)
         * It relies on the `FileStorageImpl` class that actually reads from the `/uploads/...` folder
         */
        try {
            Resource resource = fileStorageService.loadFile(filename);

            /**
             * Try to figure out what kind of file this is
             * If it can't guess, it falls back to `application/octet-stream` which just means
             *      "some binary file we don't recognize"
             */
            String contentType = Optional.ofNullable(
                    URLConnection.guessContentTypeFromName(resource.getFilename())
            ).orElse("application/octet-stream");
            logger.info("Guessed content{} for file{}", contentType, filename);

            /**
             * This builds the actual HTTP response
             *      - Status 200 OK ✅
             *      - sets the Content-type header based on the file
             *      - It attaches the file bytes as the response body
             *
             */
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            logger.info("Failed to serve file{}", filename);
            throw e;
        }
    }

    // Additional endpoints for accessing either original images and optimization status

    /**
     * Get detailed information about an image including optimzation stats
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<ImageDetailsResponse> getImageDetails(
            @PathVariable UUID id
    ){
        logger.info("Fetching detailed image information for ID: {}", id);
        return ResponseEntity.ok(imageService.getImageDetails(id));
    }

    /**
     * Get the original unoptimized version of an image
     */

    @GetMapping("/{id}/original")
    public ResponseEntity<Resource> getOriginalImage(@PathVariable UUID id){
        logger.info("Fetching original image for ID: {}", id);

        // Get the image entity
        ImageResponse image = imageService.getImageById(id);

        // Etxract the filename form the original path
        String originalFilePath = image.originalFilePath();
        String originalFileName = Path.of(originalFilePath).getFileName().toString();

        try{
            // Load the resource
            Resource resource = fileStorageService.loadFile(originalFileName);

            // Determine content type
            String contentType = Optional.of(
                    URLConnection.guessContentTypeFromName(resource.getFilename())
            ).orElse("application/octet-stream");

            // Set content disposition for browser download
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + image.fileName() + "\"")
                    .body(resource);
        }catch (Exception e){
            logger.error("Failed to serve original file: {}", originalFileName, e);
            throw e;
        }
    }

    // Endpoint to serve files from the original directory
    @GetMapping("/files/original/{filename:.+")
    public ResponseEntity<Resource> serveOriginalFile(@PathVariable String filename){
        logger.info("Attempting to download original file: {}", filename);

        try{
            Resource resource = fileStorageService.loadFile(filename);

            String contentType = Optional.ofNullable(
                    URLConnection.guessContentTypeFromName(resource.getFilename())
            ).orElse("application/octet-stream");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        }catch (Exception e){
            logger.error("Failed to serve original file: {}", filename, e);
            throw e;
        }
    }

    // ENdpoint to serve files from the optimized directory
    @GetMapping("/files/optimized/{filename:.+}")
    public ResponseEntity<Resource> serveOptimizedFile(
            @PathVariable String filename
    ){
        logger.info("Attempting to download optimized file: {}", filename);

        try{
            Resource resource = fileStorageService.loadFile(filename);

            String contentType = Optional.ofNullable(
                    URLConnection.guessContentTypeFromName(resource.getFilename())
            ).orElse("application/octet-stream");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        }catch (Exception e){
            logger.error("Failed to serve optimized file: {}", filename);
            throw e;
        }
    }

    //

}
