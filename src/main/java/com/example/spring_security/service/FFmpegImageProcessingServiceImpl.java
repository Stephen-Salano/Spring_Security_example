package com.example.spring_security.service;

import com.example.spring_security.config.ImageProcessingProperties;
import lombok.RequiredArgsConstructor;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;

/**
 * FFmpeg-backed implementation of ImageProcessingService
 * Uses JavaCV(Ffmpeg wrapper)to handle image resizing and compression
 */
@Service
@RequiredArgsConstructor
public class FFmpegImageProcessingServiceImpl implements ImageProcessingService{

    private final ImageProcessingProperties properties;
    private static final Logger logger = LoggerFactory.getLogger(FFmpegImageProcessingServiceImpl.class);

    /**
     * Resize down to maxwidth/ maxwidth (preserving aspect ratio)
     * Only resized if the original image exceeds the maximu dimensions.
     *
     * @param inputFile the original image on disk
     * @return a new file pointing to the resized image
     * @throws IOException on read/write errors
     */
    @Override
    public File resize(File inputFile) throws IOException {

        if (!validateInputFile(inputFile)){
            return inputFile;
        }

        Instant start = Instant.now();
        logger.debug("Starting image resize for file: {}", inputFile.getName());
        /**
         * FFmpegFrameGrabber opens the image file as a singe-frame video stream
         */
        try(FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile)){

            // start the grabber
            grabber.start();

            // 1. Read original dimensions
            int origW = grabber.getImageWidth();
            int origH = grabber.getImageHeight();
            logger.debug("Original dimensions: {}x{}", origW, origH);

            // 2. Compute scale factor (never upscale-ratio <= 1)
            double widthRatio = properties.getMaxWidth() / (double) origW;
            double heightRatio = properties.getMaxHeight() / (double) origH;
            double ratio = Math.min(Math.min(widthRatio, heightRatio), 1.0);

            //  Skip resize if no scaling needed (image is already smaller than max dimensions)
            if (ratio == 1.0){
                logger.debug("Image already within size limits, skipping resize");
                grabber.stop();
                return inputFile;
            }

            int newW = (int) (origW * ratio);
            int newH = (int) (origH * ratio);
            logger.debug("New dimensions: {}x{} (scale ratio: {})", newW, newH, ratio);


            // 3. Grab a single frame (static image)
            Frame frame = grabber.grabImage();

            // 4. Convert to BufferedImage for easy Java2D resizing
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage src = converter.getBufferedImage(frame);

            // 5. Draw scaled image
            BufferedImage scaled = new BufferedImage(newW, newH, src.getType());
            Graphics2D g = scaled.createGraphics();
            // Use bilinear interpolation for better quality resizing
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, newW, newH, null);
            g.dispose();

            grabber.stop();

            // 6. Write the scaled image back out FFmpegFrameRecorder
            File out = Files.createTempFile("resized-", getFileExtension(inputFile)).toFile();
            try(FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(out, newW, newH)){
                String format = getFileExtension(inputFile).replace(".", "");
                recorder.setFormat(format);
                if ("jpg".equals(format) || "jpeg".equals(format)){
                    recorder.setVideoCodec(avcodec.AV_CODEC_ID_MJPEG); // MJPEG for JPEG files
                    recorder.setVideoQuality(properties.getCompressionQuality());
                } else {
                    // For other formats (like PNG), try to maintain quality
                    recorder.setVideoQuality(1.0);
                }

                recorder.start();
                recorder.record(converter.convert(scaled));
                recorder.stop();
            }
                long duration = Duration.between(start, Instant.now()).toMillis();
                logger.info("Image resize completed in {} ms: {} -> {}", duration, inputFile.getName(), out.getName());
                return out;
        } catch (FrameGrabber.Exception | FrameRecorder.Exception e){
            logger.error("Failed to resize image: {}", inputFile.getName(), e);
            throw new IOException("Image resize failed", e);
        }
    }

    private boolean validateInputFile(File inputFile) {
        if (inputFile == null || !inputFile.isFile()){
            logger.error("Invalid input file: {}", inputFile);
            return false;
        }

        // Skip validation if no supported types are configured
        if (properties.getSupportedTypes() == null || properties.getSupportedTypes().isEmpty()){
            return true;
        }

        // Try to determine content type from file extension
        String extension = getFileExtension(inputFile).toLowerCase();
        if (extension.isEmpty()){
            logger.warn("Could not determine file type {}", inputFile.getName());
            return false;
        }

        // Map extension to MIME types
        String mimeType = switch (extension) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            default -> "unknown";
        };

        // Check if this supported type is supported
        boolean supported = properties.getSupportedTypes().contains(mimeType);
        if (!supported){
            logger.warn("Unsupported image types: {} for file: {}", mimeType, inputFile.getName());
        }

        return supported;
    }

    /**
     * Gets the file extension including the dot
     *
     * @param inputFile the file
     * @return the extension (e.g., ".jpg") or enpty string if none
     */
    private String getFileExtension(File inputFile) {
        String name = inputFile.getName();
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex > 0){
            return name.substring(lastDotIndex);
        }
        return "";
    }

    /**
     * Compress JPEG quality if file > compressionThreshold
     * For nonJPEG files, converts to JPEG for compression
     *
     * @param inputFile the original (or already resized)
     * @return File pointing to image
     * @throws IOException for read/write errors
     */
    @Override
    public File compress(File inputFile) throws IOException {
        if (validateInputFile(inputFile)){
            return inputFile;
        }

        // skip compression if below threshold
        if (inputFile.length() <= properties.getCompressionThreshold().toBytes()){
            logger.debug("File size {} bytes is below compression threshold of {} bytes, skipping compression",
                    inputFile.length(), properties.getCompressionThreshold().toBytes());
            return inputFile;
        }
        
        Instant start = Instant.now();
        logger.debug("Starting image compression for file: {} (size: {} bytes)", inputFile.getName(), inputFile.length());
        
        try(FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile)){
            grabber.start();

            // grab static image frame
            Frame frame = grabber.grabImage();
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage img = converter.getBufferedImage(frame);

            int w = img.getWidth();
            int h = img.getHeight();

            grabber.stop();

            // Preparing output files
            File output = Files.createTempFile("compressed-", ".jpg").toFile();
            try(FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(output, w, h)){
                recorder.setFormat("jpg");
                recorder.setVideoCodec(avcodec.AV_CODEC_ID_MJPEG);
                recorder.setFrameRate(1);
                
                // This is the key setting for compression quality
                recorder.setVideoQuality(properties.getCompressionQuality());
                
                
                recorder.start();
                recorder.record(converter.convert(img));
                recorder.stop();
            }
            long duration = Duration.between(start, Instant.now()).toMillis();
            double compressionRatio = (double) inputFile.length() / output.length();

            logger.info("Image compressed in {} ms: {} -> {} bytes ({}x smaller)",
                    duration, inputFile.length(), output.length(),
                    String.format("%.2f", compressionRatio));

            return output;

        } catch (FrameGrabber.Exception | FrameRecorder.Exception e){
            logger.error("Failed to compress image: {}", inputFile.getName(), e);
            throw new IOException("Image compression failed", e);
        }
    }

    /**
     * Run resize then compress according to thresholds.
     * This is the main entry point for image optimization
     *
     * @param inputFile the original upload
     * @return File
     * @throws IOException for read/write operations
     */

    @Override
    public File process(File inputFile) throws IOException {
        if (!validateInputFile(inputFile)){
            logger.warn("Skipping image processing for invalid file: {}", inputFile);
            return inputFile;
        }

        Instant startTime = Instant.now();
        logger.info("Starting image processing for file {} ({} bytes)",
                inputFile.getName(), inputFile.length());

        File current = inputFile;
        boolean modified = false;


        // 1: Resize if either dimension exceeds its max
        try(FFmpegFrameGrabber check = new FFmpegFrameGrabber(current)){
            check.start();
            if (check.getImageWidth() > properties.getMaxWidth() ||
            check.getImageHeight() > properties.getMaxHeight()){
                File resized = resize(current);
                if (!resized.equals(current)){
                    current = resized;
                    modified = true;
                }
            }
            check.stop();
        } catch (FrameGrabber.Exception e){
            throw new IOException("Failed to inspect image dimensions", e);
        }

        // 2: Compress if file size is above threshold
        if (current.length() > properties.getCompressionThreshold().toBytes()){
            File compressed = compress(current);
            if (!compressed.equals(current) && modified){
                // clean up intermediate file if both resize and compress happened
                current.delete();
            }
            if (!current.delete()) {
                logger.warn("Failed to delete intermediate file: {}", current.getAbsolutePath());
            }
            current = compressed;
            modified = true;
        }

        long totalDuration = Duration.between(startTime, Instant.now()).toMillis();
        if (modified){
            double sizeReduction = (double) inputFile.length() / current.length();
            logger.info("Image processing completed in {} ms. Size reduction: {}x ({} -> {} bytes)",
                    totalDuration, String.format("%.2f", sizeReduction), inputFile.length(), current.length());
        } else{
            logger.info("no image processing was needed, completed check in {} ms", totalDuration);
        }
        return current;
    }
}
