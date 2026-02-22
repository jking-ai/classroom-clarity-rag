package com.jkingai.classroomclarity.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record QueryRequest(
        @NotBlank(message = "The 'question' field is required and must not be blank.")
        String question,
        Integer topK,
        Double similarityThreshold,
        List<UUID> documentIds
) {
    public QueryRequest {
        if (topK == null) topK = 5;
        if (similarityThreshold == null) similarityThreshold = 0.7;
        if (documentIds == null) documentIds = List.of();
    }
}
