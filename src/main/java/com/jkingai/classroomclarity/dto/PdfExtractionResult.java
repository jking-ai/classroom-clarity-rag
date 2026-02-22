package com.jkingai.classroomclarity.dto;

import java.util.List;

public record PdfExtractionResult(
        List<String> pageTexts,
        int pageCount
) {
}
