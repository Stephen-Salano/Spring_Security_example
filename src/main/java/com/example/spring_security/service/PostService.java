package com.example.spring_security.service;

import com.example.spring_security.Users.User;
import com.example.spring_security.dto.PostRequest;
import com.example.spring_security.dto.PostResponse;
import com.example.spring_security.entities.Post;
import com.example.spring_security.repository.PostRepository;
import com.example.spring_security.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public List<PostResponse> getAllPosts(){
        return postRepository.findAll().stream()
                .map(PostResponse::fromPost)
                .toList();
    }
    // Find all posts by an Author
    public List<Post> getAllPostsByAuthor(String username){
        return postRepository.findByAuthorUsername(username);
    }

    public void deletePost(UUID postID){
        postRepository.deleteById(postID);
    }

    // create a post
    @Transactional
    public PostResponse createPost(PostRequest postRequest, String userName){
        // Find the user by userName
        User author = userRepository.findByUserName(userName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Create and save the post
        Post post = Post.builder()
                .title(postRequest.title())
                .content(postRequest.content())
                .author(author)
                .build();

        return PostResponse.fromPost(postRepository.save(post));
    }
}
