package com.jkingai.classroomclarity.service;

import com.jkingai.classroomclarity.exception.DocumentProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@Profile({"local", "test"})
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path uploadDir;

    public LocalStorageService(@Value("${app.storage.local.upload-dir:data/uploads}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir);
        ensureDirectoryExists();
    }

    @Override
    public String store(String filename, byte[] content) {
        ensureDirectoryExists();
        String uniqueName = UUID.randomUUID() + "_" + filename;
        Path filePath = uploadDir.resolve(uniqueName);
        try {
            Files.write(filePath, content);
            log.info("Stored file locally: {}", filePath);
            return filePath.toString();
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to store file: " + filename, e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Path filePath = Path.of(storagePath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted file: {}", storagePath);
            } else {
                log.warn("File not found for deletion: {}", storagePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", storagePath, e);
        }
    }

    private void ensureDirectoryExists() {
        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("Created upload directory: {}", uploadDir);
            }
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to create upload directory: " + uploadDir, e);
        }
    }
}
