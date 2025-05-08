package com.example.spring_security.service;

import com.example.spring_security.config.FileStorageProperties;
import com.example.spring_security.exception.FileNotFoundException;
import com.example.spring_security.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final Path rootLocation;
    // Url under which files will be served
    private static final String URL_PREFIX = "/api/v1/images/files";
    private final Path originalLocation;
    private final Path optimizedLocation;
    private static final String ORIGINAL_URL_PREFIX = "/api/v1/images/files/original";
    private static final String OPTIMIZED_URL_PREFIX = "/api/v1/images/files/optimized";

    /**
     * The constructor
     * @param properties
     */
    @Autowired
    public LocalFileStorageServiceImpl(FileStorageProperties properties){
        this.rootLocation = properties.getFullStoragepath();
        this.originalLocation = properties.getOriginalStoragePath();
        this.optimizedLocation = properties.getOptimizedStoragePath();

        try{
            Files.createDirectories(rootLocation);
            Files.createDirectories(originalLocation);
            Files.createDirectories(optimizedLocation);

        } catch (IOException ex){
            throw new FileStorageException("Could not initialize storage directory", ex);
        }
    }


    /**
     *(which we will save in our image entity)
     * @param file accepts a Multipart file
     * @return generated filename
     */
    @Override
    public String storeFile(MultipartFile file) {
        // Sanitize original filename
        String original = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        if (original.contains("..")){
            throw new FileStorageException("Filename contains invalid path sequence" + original);
        }
        // Extract extension
        String extension = "";
        int index = original.lastIndexOf('.');
        if (index >= 0){
            extension = original.substring(index);
        }

        // Generate unique filename using UUID
        String fileName = UUID.randomUUID().toString() + extension;

        try{


            Path target = rootLocation.resolve(fileName).normalize();

            // prevent path traversal
            if (!target.getParent().equals(rootLocation)){
                throw new FileStorageException("Cannot store file outside current directory.");
            }
            // copy file
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException ex) {
            throw new FileStorageException("Failed to store file " + fileName, ex);
        }
    }

    /**
     * @param filename accepts a fileName as parameter
     * @return a spring Resource (URLResource), which we can stream via a controller
     */
    @Override
    public Resource loadFile(String filename) {
       try{
           // first try to load from optimized path
           Path optimizedFile = optimizedLocation.resolve(filename).normalize();
           if (Files.exists(optimizedFile)){
               UrlResource resource = new UrlResource(optimizedFile.toUri());
               if (resource.exists() && resource.isReadable()){
                   return resource;
               }
           }

           // If not found, try original path
           Path originalFile = originalLocation.resolve(filename).normalize();
           if (Files.exists(originalFile)){
               UrlResource resource = new UrlResource(originalFile.toUri());
               if (resource.exists() && resource.isReadable()){
                   return resource;
               }
           }
           // if not found in either location
           throw new FileNotFoundException("File not found: " + filename);
       }catch (MalformedURLException ex){
           throw new FileNotFoundException("File not found: " + filename, ex);
       }
    }

    @Override
    public boolean deleteFile(String fileName) {
        try{
            boolean deleted = false;

            // Try to delete from optimized path
            Path optimizedFile = optimizedLocation.resolve(fileName).normalize();
            if (Files.exists(optimizedFile)){
                deleted = Files.deleteIfExists(optimizedFile);
            }
            // Try to delete from original path
            Path originalFile = originalLocation.resolve(fileName).normalize();
            if (Files.exists(originalFile)){
                deleted = Files.deleteIfExists(originalFile) || deleted;
            }
            return deleted;
        }catch (IOException ex){
            throw new FileStorageException("Error deleting file: " + fileName, ex);
        }
    }

    /**
     * Stores the original user-uploaded file
     * @param file MultipartFile to store
     * @return generated filename
     */
    public String storeOriginalFile(MultipartFile file){
        return storeFileInLocation(file, originalLocation);
    }

    public String storeOptimizedFile(File file){
        try{
            // extract extension from original file name
            String original = file.getName();
            String extension = "";
            int index = original.lastIndexOf('.');
            if (index >= 0){
                extension = original.substring(index);
            }

            // Generating unique filename
            String fileName = UUID.randomUUID().toString() + extension;
            Path target = optimizedLocation.resolve(fileName).normalize();

            // Prevent path traversal
            if (!target.getParent().equals(optimizedLocation)){
                throw new FileStorageException("Cannot store file outside current directory.");
            }
            Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        }catch (IOException ex){
            throw new FileStorageException("Failed to store optimized file", ex);
        }
    }


    /**
     * Simple concatenates a prefix
     * @param fileName accepts fileName as a paremeter
     * @return a concatenated URL prefix with the file name
     */
    @Override
    public String getFileUrl(String fileName) {
        return URL_PREFIX + "/" + fileName;
    }

    // Helper methods for common file storage logic:
    private String storeFileInLocation(MultipartFile file, Path location){
        // Sanitize original file name
        String original = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        if (original.contains("..")){
            throw new FileStorageException("Filename contains invalid path sequence: " + original);
        }

        // Extract extension
        String extension = "";
        int index = original.lastIndexOf('.');
        if (index >= 0){
            extension = original.substring(index);
        }

        // Generate unique filename
        String filename = UUID.randomUUID().toString() + extension;

        try{
            Path target = location.resolve(filename).normalize();

            // prevent path traversal
            if (!target.getParent().equals(location)){
                throw new FileStorageException("Cannot store file outside current directory");
            }

            // copy file
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        }catch (IOException ex){
            throw new FileStorageException("Failed to store file " + filename, ex);
        }
    }

    // Methods to get URLs for both original and optimized files

    /**
     * Gets the URL for accessing the original image
     *
     * @param fileName the stored filename
     * @return URL string for the original image
     */
    public  String getOriginalFileUrl(String fileName){
        return ORIGINAL_URL_PREFIX + "/" + fileName;
    }

    /**
     * Gets the URL for accessing the optimized image
     * @param fileName the stored filename
     * @return URL string for the optimized image
     */
    public  String getOptimizedFileUrl(String fileName){
        return OPTIMIZED_URL_PREFIX + "/" + fileName;
    }
}
