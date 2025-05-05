package com.example.spring_security.service;

import com.example.spring_security.config.ImageProcessingProperties;
import lombok.RequiredArgsConstructor;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * FFmpeg-backed implementation of ImageProcessingService
 */
@Service
@RequiredArgsConstructor
public class FFmpegImageProcessingServiceImpl implements ImageProcessingService{
    private final ImageProcessingProperties properties;

    /**
     * Resize down to maxwidth/ maxwidth (preserving aspect ratio)
     * @param inputFile the original image on disk
     * @return a new file pointing to the resized image
     * @throws IOException on read/write errors
     */
    @Override
    public File resize(File inputFile) throws IOException {
        /**
         * FFmpegFrameGrabber opens the image file as a singe-frame video stream
         */
        try(FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile)){
            // 1. Read original dimensions
            int origW = grabber.getImageWidth();
            int origH = grabber.getImageHeight();

            // 2. Compute scale factor (never upscale-ratio <= 1)
            double widthRatio = properties.getMaxWidth() / (double) origW;
            double heightRatio = properties.getMaxHeight() / (double) origH;
            double ratio = Math.min(Math.min(widthRatio, heightRatio), 1.0);

            int newW = (int) (origW * ratio);
            int newH = (int) (origH * ratio);

            // 3. Grab a single frame (static image)
            Frame frame = grabber.grabImage();

            // 4. Convert to BufferedImage for easy Java2D resizing
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage src = converter.getBufferedImage(frame);

            // 5. Draw scaled image
            BufferedImage scaled = new BufferedImage(newW, newH, src.getType());
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, newW, newH, null);
            g.dispose();

            grabber.stop();

            // 6. Write the scaled image back out FFmpegFrameRecorder
            File out = Files.createTempFile("resized-", ".jpg").toFile();
            try(FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(out, newW, newH)){

                recorder.setFormat("jpg"); // output format
                recorder.setVideoCodec(avcodec.AV_CODEC_ID_MJPEG); // MJPEG for JPEG files
                recorder.start();
                recorder.record(converter.convert(scaled));
                recorder.stop();;
            }
                return out;
        } catch (FrameGrabber.Exception | FrameRecorder.Exception e){
            throw new IOException("Image resize failed", e);
        }
    }

    /**
     * Compress JPEG quality if file > compressionThreshold
     * @param inputFile the original (or already resized)
     * @return File pointing to image
     * @throws IOException for read/write errors
     */
    @Override
    public File compress(File inputFile) throws IOException {
        // skip compression if below threshold
        if (inputFile.length() <= properties.getCompressionThreshold().toBytes()){
            return inputFile;
        }
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
                recorder.setVideoQuality(properties.getCompressionQuality());
                recorder.start();
                recorder.record(converter.convert(img));
                recorder.stop();
            }
            return output;

        } catch (FrameGrabber.Exception | FrameRecorder.Exception e){
            throw new IOException("Image compression failed", e);
        }
    }

    /**
     * Run resize then compress according to thresholds.
     * @param inputFile the original upload
     * @return File
     * @throws IOException for read/write operations
     */

    @Override
    public File process(File inputFile) throws IOException {
        File current = inputFile;

        // 1: Resize if either dimension exceeds its max
        try(FFmpegFrameGrabber check = new FFmpegFrameGrabber(current)){
            check.start();
            if (check.getImageWidth() > properties.getMaxWidth() ||
            check.getImageHeight() > properties.getMaxHeight()){
                current = resize(current);
            }
            check.start();
        } catch (FrameGrabber.Exception e){
            throw new IOException("Failed to inspect image dimensions", e);
        }

        // 2: Compress if file size is above threshold
        if (current.length() > properties.getCompressionThreshold().toBytes()){
            current = compress(current);
        }
        return current;
    }
}
