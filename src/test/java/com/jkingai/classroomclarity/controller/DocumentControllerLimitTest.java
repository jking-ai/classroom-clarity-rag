package com.jkingai.classroomclarity.controller;

import com.jkingai.classroomclarity.TestApiKey;
import com.jkingai.classroomclarity.config.TestAiConfig;
import com.jkingai.classroomclarity.repository.DocumentChunkRepository;
import com.jkingai.classroomclarity.repository.DocumentRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.documents.max-count=2"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(TestAiConfig.class)
class DocumentControllerLimitTest {

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

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @BeforeEach
    void setUp() {
        documentChunkRepository.deleteAllInBatch();
        documentRepository.deleteAllInBatch();
    }

    @Test
    void uploadReturns409WhenDocumentLimitReached() throws Exception {
        byte[] pdfBytes = createTestPdf("Content.");

        // Upload first two (within limit of 2)
        for (int i = 0; i < 2; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "doc-" + i + ".pdf", "application/pdf", pdfBytes);
            mockMvc.perform(multipart("/api/v1/documents").file(file)
                            .header(TestApiKey.HEADER, TestApiKey.VALUE))
                    .andExpect(status().isCreated());
        }

        // Third upload should be rejected
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc-3.pdf", "application/pdf", pdfBytes);
        mockMvc.perform(multipart("/api/v1/documents").file(file)
                        .header(TestApiKey.HEADER, TestApiKey.VALUE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DOCUMENT_LIMIT_EXCEEDED"));
    }

    private byte[] createTestPdf(String text) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
