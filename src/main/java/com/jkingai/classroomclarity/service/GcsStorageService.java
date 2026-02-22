package com.jkingai.classroomclarity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class GcsStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(GcsStorageService.class);

    private final String bucketName;

    public GcsStorageService(@Value("${app.storage.gcs.bucket-name}") String bucketName) {
        this.bucketName = bucketName;
        log.warn("GcsStorageService is a stub — GCS integration will be implemented in Phase 3");
    }

    @Override
    public String store(String filename, byte[] content) {
        throw new UnsupportedOperationException("GCS storage not yet implemented. Use local profile for development.");
    }

    @Override
    public void delete(String storagePath) {
        throw new UnsupportedOperationException("GCS storage not yet implemented. Use local profile for development.");
    }
}
