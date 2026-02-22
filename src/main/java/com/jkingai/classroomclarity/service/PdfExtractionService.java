package com.jkingai.classroomclarity.service;

import com.jkingai.classroomclarity.dto.PdfExtractionResult;
import com.jkingai.classroomclarity.exception.DocumentProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfExtractionService {

    private static final Logger log = LoggerFactory.getLogger(PdfExtractionService.class);

    public PdfExtractionResult extract(InputStream inputStream) {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            if (document.isEncrypted()) {
                throw new DocumentProcessingException("Cannot process encrypted PDF files");
            }

            int pageCount = document.getNumberOfPages();
            List<String> pageTexts = new ArrayList<>(pageCount);
            PDFTextStripper stripper = new PDFTextStripper();

            for (int i = 1; i <= pageCount; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(document).trim();
                pageTexts.add(text);
            }

            log.info("Extracted text from {} pages", pageCount);
            return new PdfExtractionResult(pageTexts, pageCount);
        } catch (DocumentProcessingException e) {
            throw e;
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to extract text from PDF", e);
        }
    }
}
