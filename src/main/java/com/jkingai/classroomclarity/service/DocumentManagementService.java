package com.jkingai.classroomclarity.service;

import com.jkingai.classroomclarity.exception.DocumentNotFoundException;
import com.jkingai.classroomclarity.model.Document;
import com.jkingai.classroomclarity.repository.DocumentChunkRepository;
import com.jkingai.classroomclarity.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DocumentManagementService {

    private static final Logger log = LoggerFactory.getLogger(DocumentManagementService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final StorageService storageService;

    public DocumentManagementService(DocumentRepository documentRepository,
                                     DocumentChunkRepository documentChunkRepository,
                                     StorageService storageService) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.storageService = storageService;
    }

    public Page<Document> listDocuments(Pageable pageable) {
        return documentRepository.findAll(pageable);
    }

    public Document getDocument(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }

    @Transactional
    public void deleteDocument(UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));

        if (document.getStoragePath() != null) {
            storageService.delete(document.getStoragePath());
        }

        // Delete chunks via native query to avoid Hibernate loading pgvector
        // columns (which causes PSQLException). DB has ON DELETE CASCADE as a
        // safety net, but explicit deletion keeps intent clear.
        documentChunkRepository.deleteByDocumentId(id);
        documentRepository.deleteById(id);
        log.info("Deleted document: id={}, title={}", id, document.getTitle());
    }
}
