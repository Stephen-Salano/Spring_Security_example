package com.example.spring_security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.util.List;

/**
 * Configuration properties for image processing functionality
 * Maps to the 'image.processing' section in application.yaml
 */
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
     * DataSize converts to human-readable values (e.g., "2MB") to bytes
     */
    private DataSize compressionThreshold;
    /**
     * JPEG quality factor (0.0 - 1.0) when compressing.
     * Higher values mean better quality but larger file size
     * - 0.9-1.0: High quality (minimal compression)
     * - 0.7-0.8: Good quality (recommended)
     * - 0.5-0.6: Medium quality
     * - Below 0.5: Low quality (aggressive compression)
      */
    private float compressionQuality;

    /**
     * List of file types supported for FFmpeg processing
     * Example: ["image/jpeg", "image/png"]
     */
    private List<String> supportedTypes;

    /**
     * Whether to enable detailed logging of image processing operations.
     */
    private boolean enableDetailedLogging = false;

}
