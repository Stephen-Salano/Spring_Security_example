package com.example.spring_security.service;

import com.example.spring_security.config.FileStorageProperties;
import com.example.spring_security.dto.ImageDetailsResponse;
import com.example.spring_security.dto.ImageResponse;
import com.example.spring_security.dto.ImageUploadRequest;
import com.example.spring_security.entities.Image;
import com.example.spring_security.entities.Post;
import com.example.spring_security.exception.FileValidationException;
import com.example.spring_security.repository.ImageRepository;
import com.example.spring_security.repository.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ImageServiceImpl implements ImageService{

    // Dependency Injection
    private final ImageRepository imageRepository;
    private final PostRepository postRepository;
    private final FileStorageService fileStorageService;
    private final FileStorageProperties properties;
    private final ImageOptimizationService imageOptimizationService;

    private static final Logger logger = LoggerFactory.getLogger(ImageServiceImpl.class);

    @Override
    @Transactional
    public ImageResponse createImage(UUID postId, MultipartFile file) {
       // look up the Post
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException(" Post not found with ID: " + postId));

       // 2 Validate file
        validateFile(file, properties.getMaxFileSize(), properties.getAllowedTypes());

        // 3. store on disk
        String originalFileName = fileStorageService.storeOriginalFile(file);

        //  Build file pointing to the original image
        File originalFile = properties
                .getOriginalStoragePath()
                .resolve(originalFileName)
                .toFile();
        // Store original file size
        long originalFileSize = originalFile.length();
        // Generate Urls for both versions
        String originalUrl = fileStorageService.getOriginalFileUrl(originalFileName);

        boolean shouldOptimize = imageOptimizationService.shouldOptimize(originalFile);

        String optimizedFileName;
        String optimizedUrl;
        long finalFileSize = 0;
        boolean wasOptimized = false;

        if (shouldOptimize){
            try{
                // process the image through our optimization pipeline
                File optimizedFile = imageOptimizationService.optimizeImage(originalFile);
                wasOptimized = !optimizedFile.equals(originalFile);

                if (wasOptimized){
                    // Store the optimized version
                    optimizedFileName = fileStorageService.storeOptimizedFile(optimizedFile);
                    optimizedUrl = fileStorageService.getOptimizedFileUrl(optimizedFileName);
                }else {
                    // if processing didn't create a new file, use original
                    optimizedFileName = originalFileName;
                    optimizedUrl = originalUrl;
                    finalFileSize = originalFileSize;
                }
            }catch (Exception e){
                // On optimization failure, fall back to the original
                logger.error("Image optimization failed, using original: {}", e.getMessage());
                optimizedFileName = originalFileName;
                optimizedUrl = originalUrl;
                finalFileSize = originalFileSize;
                wasOptimized = false;
            }
        } else {
            // No optimization needed
            optimizedFileName = originalFileName;
            optimizedUrl = originalUrl;
            finalFileSize = originalFileSize;
        }

        Image image = Image.builder()
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(finalFileSize)
                .filePath(optimizedUrl) // Main pain points to optimized if available
                .originalFilePath(String.valueOf(originalFileSize))
                .optimized(wasOptimized)
                .post(post)
                .build();

        Image saved = imageRepository.save(image);

        return ImageResponse.fromImage(saved);
    }

    @Override
    public ImageResponse createImage(UUID postId, MultipartFile file, ImageUploadRequest request) throws IOException {
        return null;
    }

    private void validateFile(MultipartFile file, DataSize maxFileSize, List<String> allowedTypes) {
        if (file.isEmpty()){
            throw new FileValidationException("Cannot upload empty file");
        }

        if (file.getSize()> maxFileSize.toBytes())
            throw new FileValidationException("File size " + file.getSize() + " exceeds max allowed: " + maxFileSize);

        String ct = Optional.ofNullable(file.getContentType()).orElse("");
        if (!allowedTypes.contains(ct))
            throw new FileValidationException("Invalid file type: " + ct);
    }

    @Override
    public ImageResponse getImageById(UUID id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(" Image not found with id: " + id));

        return ImageResponse.fromImage(image);
    }

    @Override
    public List<ImageResponse> getImagesByPost(UUID id) {
        return imageRepository.findByPostId(id).stream()
                .map(ImageResponse::fromImage)
                .toList();
    }

    @Override
    public ImageDetailsResponse getImageDetails(UUID id) {
        return null;
    }

    @Override
    @Transactional
    public void deleteImage(UUID id) {
       Image image = imageRepository.findById(id)
               .orElseThrow(() -> new EntityNotFoundException("Image not found: " + id));

       // Delete the physical file
        String fileName = Path.of(image.getFilePath()).getFileName().toString();
        fileStorageService.deleteFile(fileName);
        // Remove the Database Record of the file
        imageRepository.delete(image);
    }

    @Override
    @Transactional
    public ImageResponse updateImage(UUID id, MultipartFile file) {
        // 1. Look up the existing image
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(" Image not found with Id: " + id));

        // Delete both original and optimized versions if they exist
        if (image.getFilePath() != null){
            String optimizedFileName = Path.of(image.getFilePath()).getFileName().toString();
            fileStorageService.deleteFile(optimizedFileName);
        }

        if (image.getOriginalFilePath() != null && !image.getOriginalFilePath().equals(image.getFilePath())){
            String originalFileName = Path.of(image.getOriginalFilePath()).getFileName().toString();
            fileStorageService.deleteFile(originalFileName);
        }

        // validate new file
        validateFile(file, properties.getMaxFileSize(), properties.getAllowedTypes());

        // Store original file
        String originalFileName = fileStorageService.storeOriginalFile(file);
        File originalFile = properties.getOriginalStoragePath().resolve(originalFileName).toFile();
        long originalFileSize = originalFile.length();
        String originalUrl = fileStorageService.getOriginalFileUrl(originalFileName);

        // Determine if optimization is needed
        boolean shouldOptimize = imageOptimizationService.shouldOptimize(originalFile);

        // Variables to track final state
        String optimizedFileName;
        String optimizedUrl;
        long finalFileSize;
        boolean wasOptimized = false;

        if (shouldOptimize){
            try{
                // Process the image through our optimization pipeline
                File optimizedFile = imageOptimizationService.optimizeImage(originalFile);
                wasOptimized = !optimizedFile.equals(originalFile);

                if (wasOptimized){
                    // Store the optimized version
                    optimizedFileName = fileStorageService.storeOptimizedFile(optimizedFile);
                    optimizedUrl = fileStorageService.getOptimizedFileUrl(optimizedFileName);
                    finalFileSize = optimizedFile.length();
                } else {
                    // If processing didn't create a new file, use original
                    optimizedFileName = originalFileName;
                    optimizedUrl = originalUrl;
                    finalFileSize = originalFileSize;
                }
            }catch (Exception e){
                // On optimization failure, fall back to the original
                logger.error("Image optimization failed, using original: {}", e.getMessage());

                optimizedFileName = originalFileName;
                optimizedUrl = originalUrl;
                finalFileSize = originalFileSize;
                wasOptimized = false;
            }

        }else {
            // No optimization needed
            optimizedFileName = originalFileName;
            optimizedUrl = originalUrl;
            finalFileSize = originalFileSize;
        }

        // Updating the image entity
        image.setFileName(file.getOriginalFilename());
        image.setFileType(file.getContentType());
        image.setFileSize(finalFileSize);
        image.setFilePath(optimizedUrl);
        image.setOriginalFilePath(originalUrl);
        image.setOriginalFileSize(String.valueOf(originalFileSize));
        image.setOptimized(wasOptimized);
        // Post remains unchanged

        Image updatedImage = imageRepository.save(image);
        return ImageResponse.fromImage(updatedImage);
    }

    @Override
    public ImageResponse updateImage(UUID id, MultipartFile file, ImageUploadRequest request) throws IOException {
        return null;
    }
}
