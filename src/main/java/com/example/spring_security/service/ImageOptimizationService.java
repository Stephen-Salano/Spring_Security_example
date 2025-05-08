package com.example.spring_security.service;

import com.example.spring_security.config.ImageProcessingProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class ImageOptimizationService {

    private static final Logger logger = LoggerFactory.getLogger(ImageOptimizationService.class);
    private final ImageProcessingService imageProcessingService;
    private final ImageProcessingProperties properties;

    // Thread pool for handling parallel optimization tasks
    private final ExecutorService optimizationExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2)
    );

    /**
     * Determines if an image should be optimized based on configured thresholds
     *
     * @param file the image file to evaluate
     * @return true if the image should be optimized
     */
    public boolean shouldOptimize(File file){
        if (file == null || !file.exists()){
            return false;
        }

        // Checking if the file exceeds size threshold
        boolean exceedSize = file.length() > properties.getCompressionThreshold().toBytes();

        if (properties.isEnableDetailedLogging()){
            logger.debug("Image optimization evaluation: file={}, size={}, threshold={}, exceedsSize={}",
                    file.getName(), file.length(), properties.getCompressionThreshold(), exceedSize);
        }
        return exceedSize;
    }

    /**
     * Synchronously optimize an image file
     *
     *
     * @param originalFile the file to optimize
     * @return the optimized file, or the original if optimization wasn't needed/possible
     * @throws IOException if processing fails
     */
    public File optimizeImage(File originalFile)throws IOException{
        if (!shouldOptimize(originalFile)){
            logger.debug("Skipping optimization for {}", originalFile.getName());
            return originalFile;
        }

        logger.info("Optimizing image: {}", originalFile.getName());
        return imageProcessingService.process(originalFile);
    }

    /**
     * Asynchronously optimizes an image file
     * @param originalFile
     * @return
     */
    public CompletableFuture<File> optimizeImageAsync(File originalFile){
        return CompletableFuture.supplyAsync(() -> {
            try{
                return optimizeImage(originalFile);
            }catch (IOException e){
                logger.error("Async Image optimization failed for {}", originalFile.getName(), e);
                // Return the original file if optimization fails
                return originalFile;
            }

        }, optimizationExecutor);
    }

}
