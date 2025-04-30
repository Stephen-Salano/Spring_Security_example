package com.example.spring_security.entities;

import com.example.spring_security.Users.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "Posts")
public class Post {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    @NotBlank(message = "Title cannot be blank")
    private String title;

    @NotBlank(message = "Content cannot be blank")
    @Column(nullable = false)
    private String content;

    @ManyToOne()
    @JoinColumn(nullable = false)// JPA uses id from app user
    private User author;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Image> images = new ArrayList<>();

    // Helper method that adds an image to a post
    public void addImage(Image image){
        images.add(image);
        image.setPost(this);
    }

    // Helper method to remove an image from a post
    public void removeImage(Image image){
        images.remove(image);
        image.setPost(null);
    }

}
