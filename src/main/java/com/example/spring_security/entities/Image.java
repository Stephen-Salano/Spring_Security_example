package com.example.spring_security.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "images")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String fileName;
    @Column(nullable = false)
    private String fileType;
    @Column(nullable = false)
    private Long fileSize;
    // URL path to image
    @Column(nullable = true)
    private String filePath;
    @Column(nullable = true)
    private String originalFilePath;
    @Column(nullable = true)
    private String originalFileSize;
    @Column(nullable = false)
    private boolean optimized;
    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    // Many Images can belong to one post
    @ManyToOne(fetch = FetchType.LAZY) // so that fetching the image metadata doesn't inadvertently load the entire post object
    @JoinColumn(name = "post_id")
    private Post post;

    // This sets uploadedAt automatically
    @PrePersist
    protected void onCreate(){
        uploadedAt = LocalDateTime.now();
    }

}
