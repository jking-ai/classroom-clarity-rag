package com.jkingai.classroomclarity.service;

import com.jkingai.classroomclarity.dto.QueryRequest;
import com.jkingai.classroomclarity.dto.QueryResponse;
import com.jkingai.classroomclarity.exception.LlmServiceException;
import com.jkingai.classroomclarity.exception.NoRelevantContextException;
import com.jkingai.classroomclarity.model.Document;
import com.jkingai.classroomclarity.repository.DocumentChunkRepository;
import com.jkingai.classroomclarity.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QueryService {

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant that answers questions based solely on the provided context.
            If the context does not contain enough information to answer the question, say so clearly.
            Always reference the specific sources when possible.
            Do not make up information that is not in the provided context.
            """;

    private final EmbeddingModel embeddingModel;
    private final ChatModel chatModel;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;

    public QueryService(EmbeddingModel embeddingModel,
                        ChatModel chatModel,
                        DocumentChunkRepository documentChunkRepository,
                        DocumentRepository documentRepository) {
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;
        this.documentChunkRepository = documentChunkRepository;
        this.documentRepository = documentRepository;
    }

    public QueryResponse query(QueryRequest request) {
        long startTime = System.currentTimeMillis();

        float[] questionEmbedding = embeddingModel.embed(request.question());
        String embeddingStr = DocumentIngestionService.embeddingToString(questionEmbedding);

        List<Object[]> rawResults;
        if (request.documentIds() != null && !request.documentIds().isEmpty()) {
            UUID[] docIds = request.documentIds().toArray(UUID[]::new);
            rawResults = documentChunkRepository.findSimilarChunksFilteredByDocumentsRaw(
                    embeddingStr, request.similarityThreshold(), request.topK(), docIds);
        } else {
            rawResults = documentChunkRepository.findSimilarChunksRaw(
                    embeddingStr, request.similarityThreshold(), request.topK());
        }

        if (rawResults.isEmpty()) {
            throw new NoRelevantContextException(request.similarityThreshold());
        }

        List<ChunkResult> chunkResults = parseRawResults(rawResults);
        String context = buildContext(chunkResults);
        String answer = generateAnswer(request.question(), context);

        List<QueryResponse.Source> sources = chunkResults.stream()
                .map(cr -> new QueryResponse.Source(
                        cr.documentId,
                        cr.documentTitle,
                        cr.chunkId,
                        cr.content,
                        cr.pageNumber,
                        cr.similarityScore
                ))
                .toList();

        long processingTimeMs = System.currentTimeMillis() - startTime;
        QueryResponse.Metadata metadata = new QueryResponse.Metadata(
                "gemini-2.0-flash",
                sources.size(),
                request.topK(),
                processingTimeMs
        );

        return new QueryResponse(answer, sources, metadata);
    }

    private List<ChunkResult> parseRawResults(List<Object[]> rawResults) {
        // Collect unique document IDs
        List<UUID> docIds = rawResults.stream()
                .map(row -> (UUID) row[1])
                .distinct()
                .toList();

        Map<UUID, Document> docMap = documentRepository.findAllById(docIds).stream()
                .collect(Collectors.toMap(Document::getId, Function.identity()));

        List<ChunkResult> results = new ArrayList<>();
        for (Object[] row : rawResults) {
            UUID chunkId = (UUID) row[0];
            UUID documentId = (UUID) row[1];
            String content = (String) row[2];
            Integer pageNumber = row[3] != null ? ((Number) row[3]).intValue() : null;
            double similarityScore = row[8] != null ? ((Number) row[8]).doubleValue() : 0.0;

            Document doc = docMap.get(documentId);
            String docTitle = doc != null ? doc.getTitle() : "Unknown";

            results.add(new ChunkResult(chunkId, documentId, docTitle, content, pageNumber, similarityScore));
        }
        return results;
    }

    private String buildContext(List<ChunkResult> chunks) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            ChunkResult chunk = chunks.get(i);
            sb.append("[Source ").append(i + 1).append("] ");
            sb.append("(").append(chunk.documentTitle);
            if (chunk.pageNumber != null) {
                sb.append(", page ").append(chunk.pageNumber);
            }
            sb.append(")\n");
            sb.append(chunk.content).append("\n\n");
        }
        return sb.toString();
    }

    private String generateAnswer(String question, String context) {
        String userMessage = """
                Context:
                %s

                Question: %s

                Answer the question based only on the context provided above."""
                .formatted(context, question);

        try {
            Prompt prompt = new Prompt(SYSTEM_PROMPT + "\n\n" + userMessage);
            ChatResponse response = chatModel.call(prompt);
            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage(), e);
            throw new LlmServiceException("Failed to generate answer from LLM", e);
        }
    }

    private record ChunkResult(
            UUID chunkId,
            UUID documentId,
            String documentTitle,
            String content,
            Integer pageNumber,
            double similarityScore
    ) {}
}
