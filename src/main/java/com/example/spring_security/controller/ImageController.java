package com.example.spring_security.controller;

import com.example.spring_security.dto.ImageResponse;
import com.example.spring_security.service.FileStorageService;
import com.example.spring_security.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLConnection;
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
            @RequestParam("postId") UUID postId,
            @RequestParam("file")MultipartFile file
            ) throws IOException {
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
    ) throws IOException {
        return ResponseEntity.ok(imageService.updateImage(id, file));

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
}
