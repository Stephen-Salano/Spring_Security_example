package com.example.spring_security.service;

import com.example.spring_security.dto.ImageResponse;
import com.example.spring_security.entities.Image;
import com.example.spring_security.entities.Post;
import com.example.spring_security.exception.FileValidationException;
import com.example.spring_security.repository.ImageRepository;
import com.example.spring_security.repository.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImageServiceImpl implements ImageService{

    // Dependency Injection
    private final ImageRepository imageRepository;
    private final PostRepository postRepository;
    private final DataSize maxFileSize;
    private final List<String> allowedTypes;
    private final String storagePathPrefix;

    public ImageServiceImpl(
            ImageRepository imageRepository,
            PostRepository postRepository,
            @Value("${image.max-file-size}") DataSize maxFileSize,
            @Value("${image.allowed-types}") List<String> allowedTypes,
            @Value("${image.storage-path}") String storagePathPrefix
            ){
        this.imageRepository = imageRepository;
        this.postRepository = postRepository;
        this.maxFileSize = maxFileSize;
        this.allowedTypes = allowedTypes;
        this.storagePathPrefix = storagePathPrefix;
    }

    @Override
    @Transactional
    public ImageResponse createImage(UUID postId, MultipartFile file){
       // look up if Post
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException(" Post not found with ID: " + postId));

        // 2 Validate file
        if(file.isEmpty())
            throw new FileValidationException("Cannot upload empty file");
        if(file.getSize() > maxFileSize.toBytes())
            throw new FileValidationException("file size " + file.getSize() + "exceeds max allowed: " + maxFileSize);
        String contentType = file.getContentType();
        if(!allowedTypes.contains(contentType))
            throw new FileValidationException("Invalid file type: " + contentType);

        // 3. Generate Stub Storage Path
        String extension = Optional.ofNullable(file.getOriginalFilename())
                .filter(name -> name.contains("."))
                .map(name -> name.substring(name.lastIndexOf(".")))
                .orElse("");
        String generatedName = UUID.randomUUID() + extension;
        String storagePath = storagePathPrefix + "/" + generatedName;

        // 4. Build & save Entity
        Image image = Image.builder()
                .fileName(file.getOriginalFilename())
                .fileType(contentType)
                .fileSize(file.getSize())
                .filePath(storagePath)
                .post(post)
                .build();
        Image saved = imageRepository.save(image) ;

        // 5 Map to DTO
        return ImageResponse.fromImage(saved);
    }

    @Override
    public ImageResponse getImageById(UUID id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(" Image not found with id: " + id));

        return ImageResponse.fromImage(image);
    }

    @Override
    public List<ImageResponse> getImagesByPost(UUID id) {
        List<Image> images = imageRepository.findByPostId(id);
        return images.stream()
                .map(ImageResponse::fromImage)
                .toList();
    }

    @Override
    @Transactional
    public void deleteImage(UUID id) {
        if (!imageRepository.existsById(id)){
            throw new EntityNotFoundException("Image not found with ID: " + id);
        }
        imageRepository.deleteById(id);
    }

    @Override
    @Transactional
    public ImageResponse updateImage(UUID id, MultipartFile file) {
        // 1. Look up the existing image
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(" Image not found with Id: " + id));

        // 2. Validate the file
        if (file.isEmpty())
            throw new FileValidationException("Cannot upload empty file");
        if (file.getSize() > maxFileSize.toBytes())
            throw new FileValidationException("File size " + file.getSize() + " exceeds max allowed: " + maxFileSize);
        if (!allowedTypes.contains(file.getContentType()))
            throw new FileValidationException(" Invalid file type: " + file.getContentType());
        // 3 Generate new storage path stub
        String extension = Optional.ofNullable(file.getOriginalFilename())
                .filter(name -> name.contains("."))
                        .map(name -> name.substring(name.lastIndexOf('.')))
                                .orElse("");
        String newGenerateName = UUID.randomUUID() + extension;
        String newStoragePath = storagePathPrefix + "/" + newGenerateName;

        // 4. update image properties
        image.setFileName(file.getOriginalFilename());
        image.setFileType(file.getContentType());
        image.setFileSize(file.getSize());
        image.setFilePath(newStoragePath);
        // The post remains unchanged

        // 5. Save the updated image
        Image updatedImage = imageRepository.save(image);
        return ImageResponse.fromImage(updatedImage);
    }
}
