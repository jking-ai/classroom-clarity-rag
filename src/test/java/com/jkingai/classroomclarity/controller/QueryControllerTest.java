package com.jkingai.classroomclarity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jkingai.classroomclarity.config.TestAiConfig;
import com.jkingai.classroomclarity.dto.QueryRequest;
import com.jkingai.classroomclarity.model.Document;
import com.jkingai.classroomclarity.model.DocumentChunk;
import com.jkingai.classroomclarity.model.DocumentStatus;
import com.jkingai.classroomclarity.repository.DocumentChunkRepository;
import com.jkingai.classroomclarity.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(TestAiConfig.class)
class QueryControllerTest {

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
    private ObjectMapper objectMapper;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private EmbeddingModel embeddingModel;

    @BeforeEach
    void cleanUp() {
        documentChunkRepository.deleteAllInBatch();
        documentRepository.deleteAllInBatch();
    }

    @Test
    void queryReturnsAnswerWithSources() throws Exception {
        seedDocumentWithChunks();

        QueryRequest request = new QueryRequest(
                "What is the cell phone policy?", 5, 0.0, List.of());

        mockMvc.perform(post("/api/v1/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").isNotEmpty())
                .andExpect(jsonPath("$.sources").isArray())
                .andExpect(jsonPath("$.sources[0].documentId").isNotEmpty())
                .andExpect(jsonPath("$.sources[0].content").isNotEmpty())
                .andExpect(jsonPath("$.metadata.processingTimeMs").isNumber());
    }

    @Test
    void queryReturns422WhenNoChunksMatchThreshold() throws Exception {
        QueryRequest request = new QueryRequest(
                "What is the meaning of life?", 5, 0.99, List.of());

        mockMvc.perform(post("/api/v1/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("NO_RELEVANT_CONTEXT"));
    }

    @Test
    void queryReturns400ForBlankQuestion() throws Exception {
        String body = """
                {"question": "", "topK": 5}
                """;

        mockMvc.perform(post("/api/v1/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    private void seedDocumentWithChunks() {
        Document doc = new Document();
        doc.setTitle("Student Handbook");
        doc.setFilename("handbook.pdf");
        doc.setFileSize(1024L);
        doc.setStatus(DocumentStatus.COMPLETED);
        doc.setChunkCount(1);
        doc = documentRepository.save(doc);

        String chunkText = "Cell phones must be turned off during class. Students may use phones during lunch.";
        float[] embedding = embeddingModel.embed(chunkText);

        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocument(doc);
        chunk.setContent(chunkText);
        chunk.setPageNumber(1);
        chunk.setChunkIndex(0);
        chunk.setTokenCount(15);
        chunk.setEmbedding(embedding);
        documentChunkRepository.save(chunk);
    }
}
