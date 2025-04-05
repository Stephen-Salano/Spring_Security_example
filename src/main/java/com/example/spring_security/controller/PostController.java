package com.example.spring_security.controller;

import com.example.spring_security.dto.PostRequest;
import com.example.spring_security.dto.PostResponse;
import com.example.spring_security.entities.Post;
import com.example.spring_security.service.JwtService;
import com.example.spring_security.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final JwtService jwtService;

    //✅ Public Endpoint: Fetch all posts
    @GetMapping
    public ResponseEntity<List<PostResponse>> getAllPosts(){
        return ResponseEntity.ok(postService.getAllPosts());
    }

    // 🔒 Secured Endpoint: Create a new post (requires authentication)
    @PostMapping("/user/create")
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostRequest postRequest,
            Authentication authentication
            ){
        return ResponseEntity.ok(postService.createPost(postRequest, authentication.getName()));
    }
}
