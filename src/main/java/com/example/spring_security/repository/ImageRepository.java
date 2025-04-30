package com.example.spring_security.repository;

import com.example.spring_security.entities.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImageRepository extends JpaRepository<Image, UUID> {

    // Find images by post id
    List<Image> findByPostId(UUID postId);

    // Find images by fileName
    List<Image> findByFileNameContaining(String fileName);

    // Find images by Filetype
    List<Image> findByFileType(String fileType);
}
