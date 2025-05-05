package com.example.spring_security.service;

import java.io.File;
import java.io.IOException;

// Defines image optimization operations
public interface ImageProcessingService {

    /**
     * Resize the given image file down to the configured max dimensions,
     * preserving aspect ratio
     *
     * @param inputFile the original image on disk
     * @return a new file pointing to the resized image
     * @throws IOException on read/write errors
     */
    File resize(File inputFile) throws IOException;

    /**
     * Compress the given image file if it's larger than the configured threshold
     *
     * @param inputFile the original (or already resized)
     * @return a new File pointing to the compressed image
     * @throws IOException on a read/write errors
     */
    File compress(File inputFile) throws IOException;

    /**
     * Apply both resize and compress in one call based on our thresholds
     * @param inputFile the original upload
     * @return a new File pointing to the final optimized image
     * @throws IOException on read/write errors
     */
    File process(File inputFile) throws IOException;
}
