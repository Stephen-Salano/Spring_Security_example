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

    @Autowired
    public LocalFileStorageServiceImpl(FileStorageProperties properties){
        this.rootLocation = properties.getFullStoragepath();
        try{
            Files.createDirectories(rootLocation);
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
            Path file = rootLocation.resolve(filename).normalize();
            // Prevent path traversal
            if (!file.getParent().equals(rootLocation)){
                throw new FileStorageException("Cannot read file outside current directory.");
            }
            UrlResource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()){
                return resource;
            }else {
                throw new FileNotFoundException("File not found: " + filename);
            }
        }catch (MalformedURLException ex){
            throw new FileNotFoundException("File not found: " + filename, ex);
        }
    }

    @Override
    public boolean deleteFile(String fileName) {
        try{
            Path file = rootLocation.resolve(fileName).normalize();
            // prevents Path traversal
            if (!file.getParent().equals(rootLocation)){
                throw new FileStorageException("Cannot delete file outside of current directory");
            }
            return Files.deleteIfExists(file);
        }catch (IOException ex){
            throw new FileStorageException("Error deleting file: " + fileName, ex);
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
}
