package com.example.spring_security.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/***
 * This is a core abstraction that defines what operations your file storage layer must support
 * without specifying how they're implemented
 *
 */
public interface FileStorageService {

    // Saves the uploaded file to disk and returns the generated unique file name
    String storeFile(MultipartFile file);

    // Loads the file as a Resource (e.g. for streaming or download)
    Resource loadFile(String filename);

    // Deletes the file physically from storage
    boolean deleteFile(String fileName);

    // Returns the public URL to the file ()
    String getFileUrl(String fileName);

    String storeOriginalFile(MultipartFile file);

    String storeOptimizedFile(File file);

    String getOriginalFileUrl(String fileName);

    String getOptimizedFileUrl(String fileName);

}
