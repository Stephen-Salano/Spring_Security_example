package com.example.spring_security.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/***
 * This class acts as a centralized configuration bean for image storage settings.
 * Instead of scattering `@Value` annotations all over the service classes, we encapsulate related properties
 * under one class and bind it to the `image` section of `application.yaml`
 *
 */
@ConfigurationProperties(prefix = "image") // Tells spring to bind this class to all properties under image in application.yaml
@Data
public class FileStorageProperties {
    //  instance vars
    private String storagePath;
    private List<String> allowedTypes;
    private DataSize maxFileSize; // Handle values like 10MB natively
    private String originalImagePath; // path for storing original images
    private String optimizedImagePath; // Path for storing optimized images

    // Derived property for absolute path
    public Path getFullStoragepath(){
        return Paths.get(storagePath).toAbsolutePath().normalize();
    }

    /**
     * Gets the full absolute path for storing original images
     * @return Path object for original images storage
     */
    public Path getOriginalStoragePath(){
        return originalImagePath != null?
                Paths.get(originalImagePath).toAbsolutePath().normalize():
                Paths.get(storagePath, "original").toAbsolutePath().normalize();
    }

    public Path getOptimizedStoragePath(){
        return optimizedImagePath != null?
                Paths.get(optimizedImagePath).toAbsolutePath().normalize():
                Paths.get(storagePath, "optimized").toAbsolutePath().normalize();
    }

}
