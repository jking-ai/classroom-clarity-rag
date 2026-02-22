package com.jkingai.classroomclarity.service;

import com.jkingai.classroomclarity.dto.TextChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

    private static final double TOKENS_PER_WORD = 1.3;

    private final int chunkSize;
    private final int chunkOverlap;

    public ChunkingService(
            @Value("${app.chunking.chunk-size:512}") int chunkSize,
            @Value("${app.chunking.chunk-overlap:100}") int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public List<TextChunk> chunk(List<String> pageTexts) {
        if (pageTexts == null || pageTexts.isEmpty()) {
            return List.of();
        }

        List<WordWithPage> words = buildWordList(pageTexts);
        if (words.isEmpty()) {
            return List.of();
        }

        int chunkSizeInWords = (int) (chunkSize / TOKENS_PER_WORD);
        int overlapInWords = (int) (chunkOverlap / TOKENS_PER_WORD);
        int stepSize = chunkSizeInWords - overlapInWords;
        if (stepSize < 1) {
            stepSize = 1;
        }

        List<TextChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;
        int start = 0;

        while (start < words.size()) {
            int end = Math.min(start + chunkSizeInWords, words.size());
            List<WordWithPage> windowWords = words.subList(start, end);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < windowWords.size(); i++) {
                if (i > 0) sb.append(' ');
                sb.append(windowWords.get(i).word);
            }

            String content = sb.toString();
            int pageNumber = windowWords.get(0).page;
            int tokenCount = estimateTokens(windowWords.size());

            chunks.add(new TextChunk(content, pageNumber, chunkIndex, tokenCount));
            chunkIndex++;

            start += stepSize;
            if (end >= words.size()) {
                break;
            }
        }

        return chunks;
    }

    private List<WordWithPage> buildWordList(List<String> pageTexts) {
        List<WordWithPage> words = new ArrayList<>();
        for (int pageIdx = 0; pageIdx < pageTexts.size(); pageIdx++) {
            String text = pageTexts.get(pageIdx);
            if (text == null || text.isBlank()) continue;
            int pageNumber = pageIdx + 1;
            for (String word : text.split("\\s+")) {
                if (!word.isEmpty()) {
                    words.add(new WordWithPage(word, pageNumber));
                }
            }
        }
        return words;
    }

    private int estimateTokens(int wordCount) {
        return (int) Math.ceil(wordCount * TOKENS_PER_WORD);
    }

    private record WordWithPage(String word, int page) {}
}
