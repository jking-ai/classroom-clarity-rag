package com.jkingai.classroomclarity.dto;

public record TextChunk(
        String content,
        int pageNumber,
        int chunkIndex,
        int tokenCount
) {
}
