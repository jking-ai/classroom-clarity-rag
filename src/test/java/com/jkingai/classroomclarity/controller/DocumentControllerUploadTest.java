package com.jkingai.classroomclarity.controller;

import com.jkingai.classroomclarity.TestApiKey;
import com.jkingai.classroomclarity.config.TestAiConfig;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(TestAiConfig.class)
class DocumentControllerUploadTest {

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

    @Test
    void uploadPdfReturns201WithDocumentMetadata() throws Exception {
        byte[] pdfBytes = createTestPdf("Test content for upload.");
        MockMultipartFile file = new MockMultipartFile(
                "file", "handbook.pdf", "application/pdf", pdfBytes);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("title", "Test Handbook")
                        .param("description", "A test")
                        .header(TestApiKey.HEADER, TestApiKey.VALUE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Test Handbook"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.pageCount").value(1))
                .andExpect(jsonPath("$.chunkCount").isNumber());
    }

    @Test
    void uploadNonPdfReturns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "data.xlsx", "application/vnd.ms-excel", "not pdf".getBytes());

        mockMvc.perform(multipart("/api/v1/documents").file(file)
                        .header(TestApiKey.HEADER, TestApiKey.VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_FILE_TYPE"));
    }

    @Test
    void uploadDefaultsTitleToFilename() throws Exception {
        byte[] pdfBytes = createTestPdf("Content.");
        MockMultipartFile file = new MockMultipartFile(
                "file", "student-handbook.pdf", "application/pdf", pdfBytes);

        mockMvc.perform(multipart("/api/v1/documents").file(file)
                        .header(TestApiKey.HEADER, TestApiKey.VALUE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("student-handbook"));
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
