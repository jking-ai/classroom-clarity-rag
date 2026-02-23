package com.jkingai.classroomclarity.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.jkingai.classroomclarity.exception.DocumentProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Profile("prod")
public class GcsStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(GcsStorageService.class);

    private final Storage storage;
    private final String bucketName;

    public GcsStorageService(@Value("${app.storage.gcs.bucket-name}") String bucketName) {
        this.storage = StorageOptions.getDefaultInstance().getService();
        this.bucketName = bucketName;
        log.info("GcsStorageService initialized with bucket: {}", bucketName);
    }

    @Override
    public String store(String filename, byte[] content) {
        String storagePath = "documents/" + UUID.randomUUID() + "/" + filename;
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, storagePath)).build();
        try {
            storage.create(blobInfo, content);
            log.info("Stored file in GCS: gs://{}/{}", bucketName, storagePath);
            return storagePath;
        } catch (Exception e) {
            throw new DocumentProcessingException("Failed to store file in GCS: " + filename, e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            boolean deleted = storage.delete(BlobId.of(bucketName, storagePath));
            if (deleted) {
                log.info("Deleted file from GCS: gs://{}/{}", bucketName, storagePath);
            } else {
                log.warn("File not found in GCS for deletion: gs://{}/{}", bucketName, storagePath);
            }
        } catch (Exception e) {
            log.error("Failed to delete file from GCS: gs://{}/{}", bucketName, storagePath, e);
        }
    }
}
