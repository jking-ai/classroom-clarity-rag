package com.jkingai.classroomclarity.dto;

import java.util.List;
import java.util.UUID;

public record QueryResponse(
        String answer,
        List<Source> sources,
        Metadata metadata
) {
    public record Source(
            UUID documentId,
            String documentTitle,
            UUID chunkId,
            String content,
            Integer pageNumber,
            double similarityScore
    ) {
    }

    public record Metadata(
            String model,
            int chunksRetrieved,
            int chunksConsidered,
            long processingTimeMs
    ) {
    }
}
