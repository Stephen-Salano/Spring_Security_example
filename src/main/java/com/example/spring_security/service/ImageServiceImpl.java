package com.example.spring_security.service;

import com.example.spring_security.config.FileStorageProperties;
import com.example.spring_security.config.ImageProcessingProperties;
import com.example.spring_security.dto.ImageResponse;
import com.example.spring_security.entities.Image;
import com.example.spring_security.entities.Post;
import com.example.spring_security.exception.FileValidationException;
import com.example.spring_security.repository.ImageRepository;
import com.example.spring_security.repository.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
    private  final ImageProcessingProperties imageProcessingProperties;
    private final ImageProcessingService imageProcessingService;

    @Override
    @Transactional
    public ImageResponse createImage(UUID postId, MultipartFile file) throws IOException {
       // look up the Post
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException(" Post not found with ID: " + postId));

       // 2 Validate file
        validateFile(file, properties.getMaxFileSize(), properties.getAllowedTypes());

        // 3. store on disk
        String originalFileName = fileStorageService.storeOriginalFile(file);

        //  Build file pointing to the original image
        File originalFile = properties
                .getFullStoragepath()
                .resolve(originalFileName)
                .toFile();
        // Store original file size
        long originalFileSize = originalFile.length();

        // Optimize the image
        File optimizedFile = imageProcessingService.process(originalFile);

        // Track if optimization was performed
        boolean wasOptimized =  !optimizedFile.equals(originalFile);

        // Store optimized file (if optimization occurred)
        String optimizedFileName;
        if (wasOptimized){
            optimizedFileName = fileStorageService.storeOptimizedFile(optimizedFile);
        } else {
            // of no optimization was needed, just use the original
            optimizedFileName = originalFileName;
        }

        // Generate Urls for both versions
        String originalUrl = fileStorageService.getOriginalFileUrl(originalFileName);
        String optimizedUrl = wasOptimized?
                fileStorageService.getOptimizedFileUrl(optimizedFileName) :
                originalUrl;

        // Build & save Entity
        Image image = Image.builder()
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(wasOptimized ? optimizedFile.length() : originalFileSize)
                .filePath(optimizedUrl)
                .originalFilePath(originalUrl)
                .originalFileSize(String.valueOf(originalFileSize))
                .optimized(wasOptimized)
                .post(post)
                .build();
        Image saved = imageRepository.save(image) ;

        // 5 Map to DTO
        return ImageResponse.fromImage(saved);
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
    public ImageResponse updateImage(UUID id, MultipartFile file) throws IOException {
        // 1. Look up the existing image
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(" Image not found with Id: " + id));

        // 2. Delete the old file
        String oldPath = image.getFilePath();
        String oldFileName = Path.of(oldPath).getFileName().toString();
        fileStorageService.deleteFile(oldFileName);

        // 3 Validate ne file
        validateFile(file, properties.getMaxFileSize(), properties.getAllowedTypes());

        // 4. Store New file under a fresh UUID name
        String newStored = fileStorageService.storeFile(file);
        File storedFile = properties.getFullStoragepath().resolve(newStored).toFile();
        // 5. Optimize (resize + compress) to handle both resizing and compressing based on thresholds
        File optimized = imageProcessingService.process(storedFile);

        // 6. Overwrite only if optimization created a new file
        if (!optimized.equals(storedFile)){
            Files.copy(
                    optimized.toPath(),
                    storedFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        }

        // 7. update image properties
        image.setFileName(file.getOriginalFilename());
        image.setFileType(file.getContentType());
        image.setFileSize(storedFile.length());
        image.setFilePath(fileStorageService.getFileUrl(newStored));
        // The post remains unchanged

        // 8. Save the updated image
        Image updatedImage = imageRepository.save(image);
        return ImageResponse.fromImage(updatedImage);
    }
}
