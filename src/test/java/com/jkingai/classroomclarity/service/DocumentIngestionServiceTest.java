package com.jkingai.classroomclarity.service;

import com.jkingai.classroomclarity.config.TestAiConfig;
import com.jkingai.classroomclarity.model.Document;
import com.jkingai.classroomclarity.model.DocumentStatus;
import com.jkingai.classroomclarity.repository.DocumentChunkRepository;
import com.jkingai.classroomclarity.repository.DocumentRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestAiConfig.class)
class DocumentIngestionServiceTest {

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
    private DocumentIngestionService ingestionService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Test
    void ingestProcessesPdfEndToEnd() throws IOException {
        byte[] pdfBytes = createTestPdf("This is page one with some content about school policies.",
                "This is page two with information about student behavior.");
        MockMultipartFile file = new MockMultipartFile(
                "file", "test-handbook.pdf", "application/pdf", pdfBytes);

        Document result = ingestionService.ingest(file, "Test Handbook", "A test document");

        assertThat(result.getId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(DocumentStatus.COMPLETED);
        assertThat(result.getPageCount()).isEqualTo(2);
        assertThat(result.getChunkCount()).isGreaterThan(0);
        assertThat(result.getStoragePath()).isNotBlank();
        assertThat(result.getTitle()).isEqualTo("Test Handbook");

        // Verify chunks were persisted
        long chunkCount = documentChunkRepository.count();
        assertThat(chunkCount).isEqualTo((long) result.getChunkCount());
    }

    @Test
    void ingestSetsFailedStatusOnError() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "bad.pdf", "application/pdf", "not a pdf".getBytes());

        try {
            ingestionService.ingest(file, "Bad PDF", null);
        } catch (Exception ignored) {
        }

        // Document should be saved with FAILED status
        Document failed = documentRepository.findAll().stream()
                .filter(d -> d.getTitle().equals("Bad PDF"))
                .findFirst()
                .orElse(null);
        assertThat(failed).isNotNull();
        assertThat(failed.getStatus()).isEqualTo(DocumentStatus.FAILED);
    }

    private byte[] createTestPdf(String... pageTexts) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (String text : pageTexts) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    contentStream.newLineAtOffset(50, 700);
                    contentStream.showText(text);
                    contentStream.endText();
                }
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
