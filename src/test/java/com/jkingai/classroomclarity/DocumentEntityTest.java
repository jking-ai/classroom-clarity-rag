package com.jkingai.classroomclarity;

import com.jkingai.classroomclarity.model.Document;
import com.jkingai.classroomclarity.model.DocumentChunk;
import com.jkingai.classroomclarity.model.DocumentStatus;
import com.jkingai.classroomclarity.repository.DocumentChunkRepository;
import com.jkingai.classroomclarity.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DocumentEntityTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Test
    void shouldSaveAndRetrieveDocument() {
        Document doc = new Document();
        doc.setTitle("Test Handbook");
        doc.setFilename("handbook.pdf");
        doc.setFileSize(1024L);
        doc.setStatus(DocumentStatus.PROCESSING);

        Document saved = documentRepository.save(doc);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Document found = documentRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("Test Handbook");
        assertThat(found.getFilename()).isEqualTo("handbook.pdf");
        assertThat(found.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
    }

    @Test
    void shouldSaveDocumentWithChunks() {
        Document doc = new Document();
        doc.setTitle("Curriculum Map");
        doc.setFilename("curriculum.pdf");
        doc.setFileSize(2048L);
        doc.setStatus(DocumentStatus.COMPLETED);
        doc.setChunkCount(1);

        Document savedDoc = documentRepository.save(doc);

        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocument(savedDoc);
        chunk.setContent("This is a test chunk about math curriculum.");
        chunk.setPageNumber(1);
        chunk.setChunkIndex(0);
        chunk.setTokenCount(8);

        DocumentChunk savedChunk = documentChunkRepository.save(chunk);

        assertThat(savedChunk.getId()).isNotNull();
        assertThat(savedChunk.getDocument().getId()).isEqualTo(savedDoc.getId());
        assertThat(savedChunk.getContent()).contains("math curriculum");
    }

    @Test
    void shouldCascadeDeleteChunks() {
        Document doc = new Document();
        doc.setTitle("Policy Doc");
        doc.setFilename("policy.pdf");
        doc.setFileSize(512L);
        doc.setStatus(DocumentStatus.COMPLETED);

        DocumentChunk chunk = new DocumentChunk();
        chunk.setContent("Policy content here.");
        chunk.setChunkIndex(0);
        chunk.setDocument(doc);
        doc.getChunks().add(chunk);

        Document saved = documentRepository.save(doc);
        assertThat(documentChunkRepository.count()).isEqualTo(1);

        documentRepository.delete(saved);
        assertThat(documentChunkRepository.count()).isEqualTo(0);
    }
}
