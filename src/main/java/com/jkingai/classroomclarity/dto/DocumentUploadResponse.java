package com.jkingai.classroomclarity.dto;

import com.jkingai.classroomclarity.model.Document;
import com.jkingai.classroomclarity.model.DocumentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentUploadResponse(
        UUID id,
        String title,
        String description,
        String filename,
        Long fileSize,
        Integer pageCount,
        Integer chunkCount,
        DocumentStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static DocumentUploadResponse from(Document doc) {
        return new DocumentUploadResponse(
                doc.getId(),
                doc.getTitle(),
                doc.getDescription(),
                doc.getFilename(),
                doc.getFileSize(),
                doc.getPageCount(),
                doc.getChunkCount(),
                doc.getStatus(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
