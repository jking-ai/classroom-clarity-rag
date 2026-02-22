package com.jkingai.classroomclarity.controller;

import com.jkingai.classroomclarity.config.TestAiConfig;
import com.jkingai.classroomclarity.model.Document;
import com.jkingai.classroomclarity.model.DocumentStatus;
import com.jkingai.classroomclarity.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(TestAiConfig.class)
class DocumentControllerCrudTest {

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
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @BeforeEach
    void cleanUp() {
        documentRepository.deleteAll();
    }

    @Test
    void listDocumentsReturnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void listDocumentsReturnsPaginatedResults() throws Exception {
        createTestDocument("Doc 1");
        createTestDocument("Doc 2");

        mockMvc.perform(get("/api/v1/documents?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void getDocumentReturns200() throws Exception {
        Document doc = createTestDocument("Test Doc");

        mockMvc.perform(get("/api/v1/documents/" + doc.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Doc"))
                .andExpect(jsonPath("$.id").value(doc.getId().toString()));
    }

    @Test
    void getDocumentReturns404ForMissing() throws Exception {
        UUID fakeId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/documents/" + fakeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DOCUMENT_NOT_FOUND"));
    }

    @Test
    void deleteDocumentReturns204() throws Exception {
        Document doc = createTestDocument("To Delete");

        mockMvc.perform(delete("/api/v1/documents/" + doc.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/documents/" + doc.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDocumentReturns404ForMissing() throws Exception {
        mockMvc.perform(delete("/api/v1/documents/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DOCUMENT_NOT_FOUND"));
    }

    private Document createTestDocument(String title) {
        Document doc = new Document();
        doc.setTitle(title);
        doc.setFilename("test.pdf");
        doc.setFileSize(1024L);
        doc.setStatus(DocumentStatus.COMPLETED);
        return documentRepository.save(doc);
    }
}
