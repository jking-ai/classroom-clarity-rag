package com.jkingai.classroomclarity.service;

import com.jkingai.classroomclarity.config.DocumentProperties;
import com.jkingai.classroomclarity.model.Document;
import com.jkingai.classroomclarity.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class DocumentCleanupService {

    private static final Logger log = LoggerFactory.getLogger(DocumentCleanupService.class);

    private final DocumentRepository documentRepository;
    private final DocumentManagementService managementService;
    private final DocumentProperties documentProperties;

    public DocumentCleanupService(DocumentRepository documentRepository,
                                  DocumentManagementService managementService,
                                  DocumentProperties documentProperties) {
        this.documentRepository = documentRepository;
        this.managementService = managementService;
        this.documentProperties = documentProperties;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredDocuments() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(documentProperties.retentionDays());
        List<Document> expired = documentRepository.findByCreatedAtBefore(cutoff);

        if (expired.isEmpty()) {
            log.info("Document cleanup: no expired documents found (retention={} days)",
                    documentProperties.retentionDays());
            return;
        }

        log.info("Document cleanup: found {} expired documents older than {}",
                expired.size(), cutoff);

        int deleted = 0;
        for (Document doc : expired) {
            try {
                managementService.deleteDocument(doc.getId());
                deleted++;
            } catch (Exception e) {
                log.error("Document cleanup: failed to delete document id={}, title={}",
                        doc.getId(), doc.getTitle(), e);
            }
        }

        log.info("Document cleanup: deleted {}/{} expired documents", deleted, expired.size());
    }
}
