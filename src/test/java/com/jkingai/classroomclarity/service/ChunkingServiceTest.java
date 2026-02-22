package com.jkingai.classroomclarity.service;

import com.jkingai.classroomclarity.dto.TextChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkingServiceTest {

    private final ChunkingService service = new ChunkingService(512, 100);

    @Test
    void emptyInputReturnsEmptyList() {
        assertThat(service.chunk(List.of())).isEmpty();
        assertThat(service.chunk(null)).isEmpty();
        assertThat(service.chunk(List.of(""))).isEmpty();
    }

    @Test
    void singleShortPageReturnsSingleChunk() {
        List<TextChunk> chunks = service.chunk(List.of("This is a short page of text."));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).content()).isEqualTo("This is a short page of text.");
        assertThat(chunks.get(0).pageNumber()).isEqualTo(1);
        assertThat(chunks.get(0).chunkIndex()).isEqualTo(0);
        assertThat(chunks.get(0).tokenCount()).isGreaterThan(0);
    }

    @Test
    void longTextProducesMultipleOverlappingChunks() {
        // Create text with ~1000 words (more than 512 tokens)
        String longText = IntStream.range(0, 1000)
                .mapToObj(i -> "word" + i)
                .collect(Collectors.joining(" "));

        List<TextChunk> chunks = service.chunk(List.of(longText));

        assertThat(chunks.size()).isGreaterThan(1);

        // Verify sequential chunk indices
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).chunkIndex()).isEqualTo(i);
        }

        // Verify overlap: consecutive chunks should share some words
        if (chunks.size() >= 2) {
            String[] firstWords = chunks.get(0).content().split("\\s+");
            String[] secondWords = chunks.get(1).content().split("\\s+");
            // The end of first chunk should overlap with beginning of second
            String lastWordOfFirst = firstWords[firstWords.length - 1];
            assertThat(chunks.get(1).content()).contains(lastWordOfFirst);
        }
    }

    @Test
    void multiPageTextTracksPageNumbers() {
        String page1 = "Page one content with several words to fill the space.";
        String page2 = "Page two has different content about another topic entirely.";
        String page3 = "Page three wraps up with a conclusion.";

        List<TextChunk> chunks = service.chunk(List.of(page1, page2, page3));

        assertThat(chunks).isNotEmpty();
        // First chunk should start from page 1
        assertThat(chunks.get(0).pageNumber()).isEqualTo(1);
    }

    @Test
    void skipsEmptyPages() {
        List<TextChunk> chunks = service.chunk(List.of("", "Content on page two.", ""));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).pageNumber()).isEqualTo(2);
        assertThat(chunks.get(0).content()).contains("Content on page two.");
    }

    @Test
    void chunksHaveReasonableTokenCounts() {
        String longText = IntStream.range(0, 1000)
                .mapToObj(i -> "word" + i)
                .collect(Collectors.joining(" "));

        List<TextChunk> chunks = service.chunk(List.of(longText));

        // All chunks except possibly the last should have token counts around 512
        for (int i = 0; i < chunks.size() - 1; i++) {
            assertThat(chunks.get(i).tokenCount()).isBetween(400, 600);
        }
    }
}
