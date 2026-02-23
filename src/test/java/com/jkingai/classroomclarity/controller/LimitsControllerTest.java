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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(TestAiConfig.class)
class LimitsControllerTest {

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
        documentRepository.deleteAllInBatch();
    }

    @Test
    void limitsEndpointReturns200WithExpectedStructure() throws Exception {
        mockMvc.perform(get("/api/v1/limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents.currentCount").value(0))
                .andExpect(jsonPath("$.documents.maxCount").isNumber())
                .andExpect(jsonPath("$.documents.retentionDays").isNumber())
                .andExpect(jsonPath("$.rateLimit.requestsPerMinute").isNumber())
                .andExpect(jsonPath("$.rateLimit.queryRequestsPerMinute").isNumber());
    }

    @Test
    void limitsEndpointDoesNotRequireApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/limits"))
                .andExpect(status().isOk());
    }

    @Test
    void limitsEndpointReflectsActualDocumentCount() throws Exception {
        Document doc = new Document();
        doc.setTitle("Test Document");
        doc.setFilename("test.pdf");
        doc.setFileSize(1024L);
        doc.setStatus(DocumentStatus.COMPLETED);
        documentRepository.save(doc);

        mockMvc.perform(get("/api/v1/limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents.currentCount").value(1));
    }
}
