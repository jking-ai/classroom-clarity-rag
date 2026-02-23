package com.jkingai.classroomclarity.service;

import com.jkingai.classroomclarity.dto.PdfExtractionResult;
import com.jkingai.classroomclarity.dto.TextChunk;
import com.jkingai.classroomclarity.exception.DocumentProcessingException;
import com.jkingai.classroomclarity.model.Document;
import com.jkingai.classroomclarity.model.DocumentChunk;
import com.jkingai.classroomclarity.model.DocumentStatus;
import com.jkingai.classroomclarity.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final PdfExtractionService pdfExtractionService;
    private final ChunkingService chunkingService;
    private final StorageService storageService;
    private final EmbeddingModel embeddingModel;
    private final DocumentRepository documentRepository;
    private final TransactionTemplate transactionTemplate;
    private final int embeddingBatchSize;

    public DocumentIngestionService(
            PdfExtractionService pdfExtractionService,
            ChunkingService chunkingService,
            StorageService storageService,
            EmbeddingModel embeddingModel,
            DocumentRepository documentRepository,
            PlatformTransactionManager transactionManager,
            @Value("${app.chunking.embedding-batch-size:25}") int embeddingBatchSize) {
        this.pdfExtractionService = pdfExtractionService;
        this.chunkingService = chunkingService;
        this.storageService = storageService;
        this.embeddingModel = embeddingModel;
        this.documentRepository = documentRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.embeddingBatchSize = embeddingBatchSize;
    }

    public Document ingest(MultipartFile file, String title, String description) {
        // Create document in its own transaction so it's visible even if processing fails
        Document document = transactionTemplate.execute(status -> {
            Document doc = new Document();
            doc.setTitle(title);
            doc.setDescription(description);
            doc.setFilename(file.getOriginalFilename());
            doc.setFileSize(file.getSize());
            doc.setStatus(DocumentStatus.PROCESSING);
            return documentRepository.save(doc);
        });

        try {
            return transactionTemplate.execute(status -> {
                try {
                    return processDocument(document, file, title);
                } catch (IOException e) {
                    throw new DocumentProcessingException("Failed to read uploaded file", e);
                }
            });
        } catch (Exception e) {
            markFailed(document.getId());
            if (e instanceof DocumentProcessingException) {
                throw (DocumentProcessingException) e;
            }
            throw new DocumentProcessingException("Document ingestion failed: " + e.getMessage(), e);
        }
    }

    private Document processDocument(Document document, MultipartFile file, String title) throws IOException {
        String storagePath = storageService.store(file.getOriginalFilename(), file.getBytes());
        document.setStoragePath(storagePath);

        PdfExtractionResult extraction = pdfExtractionService.extract(file.getInputStream());
        document.setPageCount(extraction.pageCount());

        List<TextChunk> textChunks = chunkingService.chunk(extraction.pageTexts());
        log.info("Document '{}': {} pages, {} chunks", title, extraction.pageCount(), textChunks.size());

        List<String> chunkTexts = textChunks.stream().map(TextChunk::content).toList();
        List<float[]> embeddings = new ArrayList<>();
        for (int i = 0; i < chunkTexts.size(); i += embeddingBatchSize) {
            List<String> batch = chunkTexts.subList(i, Math.min(i + embeddingBatchSize, chunkTexts.size()));
            embeddings.addAll(embeddingModel.embed(batch));
        }

        for (int i = 0; i < textChunks.size(); i++) {
            TextChunk tc = textChunks.get(i);
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocument(document);
            chunk.setContent(tc.content());
            chunk.setPageNumber(tc.pageNumber());
            chunk.setChunkIndex(tc.chunkIndex());
            chunk.setTokenCount(tc.tokenCount());
            chunk.setEmbedding(embeddings.get(i));
            document.getChunks().add(chunk);
        }

        document.setChunkCount(textChunks.size());
        document.setStatus(DocumentStatus.COMPLETED);
        return documentRepository.save(document);
    }

    private void markFailed(UUID documentId) {
        try {
            transactionTemplate.executeWithoutResult(status ->
                    documentRepository.findById(documentId).ifPresent(doc -> {
                        doc.setStatus(DocumentStatus.FAILED);
                        documentRepository.save(doc);
                    })
            );
        } catch (Exception e) {
            log.error("Failed to mark document as FAILED: {}", documentId, e);
        }
    }

    public static String embeddingToString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(embedding[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
