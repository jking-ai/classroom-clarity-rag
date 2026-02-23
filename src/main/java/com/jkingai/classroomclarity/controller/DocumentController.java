package com.jkingai.classroomclarity.controller;

import com.jkingai.classroomclarity.config.DocumentProperties;
import com.jkingai.classroomclarity.dto.DocumentListResponse;
import com.jkingai.classroomclarity.dto.DocumentUploadResponse;
import com.jkingai.classroomclarity.exception.DocumentLimitExceededException;
import com.jkingai.classroomclarity.exception.InvalidFileTypeException;
import com.jkingai.classroomclarity.model.Document;
import com.jkingai.classroomclarity.repository.DocumentRepository;
import com.jkingai.classroomclarity.service.DocumentIngestionService;
import com.jkingai.classroomclarity.service.DocumentManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentIngestionService ingestionService;
    private final DocumentManagementService managementService;
    private final DocumentRepository documentRepository;
    private final DocumentProperties documentProperties;

    public DocumentController(DocumentIngestionService ingestionService,
                              DocumentManagementService managementService,
                              DocumentRepository documentRepository,
                              DocumentProperties documentProperties) {
        this.ingestionService = ingestionService;
        this.managementService = managementService;
        this.documentRepository = documentRepository;
        this.documentProperties = documentProperties;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description) {

        long count = documentRepository.count();
        if (count >= documentProperties.maxCount()) {
            throw new DocumentLimitExceededException(documentProperties.maxCount());
        }

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

    @GetMapping
    public ResponseEntity<DocumentListResponse> listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        size = Math.min(size, 100);
        String[] sortParts = sort.split(",");
        Sort sortObj = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(Sort.Direction.ASC, sortParts[0])
                : Sort.by(Sort.Direction.DESC, sortParts[0]);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<Document> documents = managementService.listDocuments(pageable);
        return ResponseEntity.ok(DocumentListResponse.from(documents));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentUploadResponse> getDocument(@PathVariable UUID id) {
        Document document = managementService.getDocument(id);
        return ResponseEntity.ok(DocumentUploadResponse.from(document));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        managementService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
