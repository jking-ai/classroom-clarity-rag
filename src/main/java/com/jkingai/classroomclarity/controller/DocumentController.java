package com.jkingai.classroomclarity.controller;

import com.jkingai.classroomclarity.dto.DocumentUploadResponse;
import com.jkingai.classroomclarity.exception.InvalidFileTypeException;
import com.jkingai.classroomclarity.model.Document;
import com.jkingai.classroomclarity.service.DocumentIngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentIngestionService ingestionService;

    public DocumentController(DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description) {

        if (file.isEmpty()) {
            throw new InvalidFileTypeException("empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new InvalidFileTypeException(contentType);
        }

        if (title == null || title.isBlank()) {
            String filename = file.getOriginalFilename();
            title = (filename != null && filename.contains("."))
                    ? filename.substring(0, filename.lastIndexOf('.'))
                    : filename;
        }

        Document document = ingestionService.ingest(file, title, description);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DocumentUploadResponse.from(document));
    }
}
