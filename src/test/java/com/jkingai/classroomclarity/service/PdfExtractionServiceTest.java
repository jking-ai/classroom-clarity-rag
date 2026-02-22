package com.jkingai.classroomclarity.service;

import com.jkingai.classroomclarity.dto.PdfExtractionResult;
import com.jkingai.classroomclarity.exception.DocumentProcessingException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfExtractionServiceTest {

    private final PdfExtractionService service = new PdfExtractionService();

    @Test
    void extractsTextFromMultiPagePdf() throws IOException {
        byte[] pdfBytes = createTestPdf("Page one content here.", "Page two content here.", "Page three content here.");

        PdfExtractionResult result = service.extract(new ByteArrayInputStream(pdfBytes));

        assertThat(result.pageCount()).isEqualTo(3);
        assertThat(result.pageTexts()).hasSize(3);
        assertThat(result.pageTexts().get(0)).contains("Page one content here.");
        assertThat(result.pageTexts().get(1)).contains("Page two content here.");
        assertThat(result.pageTexts().get(2)).contains("Page three content here.");
    }

    @Test
    void extractsSinglePagePdf() throws IOException {
        byte[] pdfBytes = createTestPdf("Hello world from a single page.");

        PdfExtractionResult result = service.extract(new ByteArrayInputStream(pdfBytes));

        assertThat(result.pageCount()).isEqualTo(1);
        assertThat(result.pageTexts()).hasSize(1);
        assertThat(result.pageTexts().get(0)).contains("Hello world");
    }

    @Test
    void throwsDocumentProcessingExceptionForCorruptPdf() {
        byte[] corruptBytes = "this is not a pdf".getBytes();

        assertThatThrownBy(() -> service.extract(new ByteArrayInputStream(corruptBytes)))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("Failed to extract text from PDF");
    }

    @Test
    void handlesEmptyPagePdf() throws IOException {
        byte[] pdfBytes = createTestPdf("");

        PdfExtractionResult result = service.extract(new ByteArrayInputStream(pdfBytes));

        assertThat(result.pageCount()).isEqualTo(1);
        assertThat(result.pageTexts()).hasSize(1);
        assertThat(result.pageTexts().get(0)).isEmpty();
    }

    private byte[] createTestPdf(String... pageTexts) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (String text : pageTexts) {
                PDPage page = new PDPage();
                document.addPage(page);
                if (!text.isEmpty()) {
                    try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                        contentStream.beginText();
                        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        contentStream.newLineAtOffset(50, 700);
                        contentStream.showText(text);
                        contentStream.endText();
                    }
                }
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
