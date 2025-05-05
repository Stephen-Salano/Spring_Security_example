package com.example.spring_security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Data //parses human readable sizes
@Component // Ensures Spring picks it up for injection
@ConfigurationProperties(prefix = "image.processing") // binds the nested YAML block
public class ImageProcessingProperties {

    // If an image's width exceeds this, we'll resize it down to this max
    private int maxWidth;
    // If an image's height exceeds this, we'll resize it down to this max
    private int maxHeight;
    /**
     * Only run compression on files larger than this threshold
     * Datasize converts 2MB -> bytes
     */
    private DataSize compressionThreshold;
    // JPEG quality factor (0.0 - 1.0) when compressing
    private float compressionQuality;


}
