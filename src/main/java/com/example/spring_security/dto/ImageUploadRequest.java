package com.example.spring_security.dto;

import lombok.Builder;

public record ImageUploadRequest(
        String postId,
        boolean forceOptimization,
        Float compressionQuality,
        boolean asyncOptimization
        ) {}