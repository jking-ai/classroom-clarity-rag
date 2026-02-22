package com.jkingai.classroomclarity.dto;

import com.jkingai.classroomclarity.model.Document;
import org.springframework.data.domain.Page;

import java.util.List;

public record DocumentListResponse(
        List<DocumentUploadResponse> content,
        PageMetadata page
) {
    public record PageMetadata(
            int number,
            int size,
            long totalElements,
            int totalPages
    ) {
    }

    public static DocumentListResponse from(Page<Document> page) {
        List<DocumentUploadResponse> content = page.getContent().stream()
                .map(DocumentUploadResponse::from)
                .toList();
        return new DocumentListResponse(
                content,
                new PageMetadata(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages()
                )
        );
    }
}
