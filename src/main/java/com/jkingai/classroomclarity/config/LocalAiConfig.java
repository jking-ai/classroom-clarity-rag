package com.jkingai.classroomclarity.config;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.stream.IntStream;

@Configuration
@Profile("local")
public class LocalAiConfig {

    @Bean
    public EmbeddingModel embeddingModel() {
        return new MockEmbeddingModel();
    }

    @Bean
    public ChatModel chatModel() {
        return new MockChatModel();
    }

    static class MockEmbeddingModel implements EmbeddingModel {

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = IntStream.range(0, request.getInstructions().size())
                    .mapToObj(i -> new Embedding(generateDeterministicVector(request.getInstructions().get(i)), i))
                    .toList();
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return generateDeterministicVector(document.getText());
        }

        private float[] generateDeterministicVector(String text) {
            float[] vector = new float[768];
            int hash = text.hashCode();
            for (int i = 0; i < 768; i++) {
                vector[i] = (float) Math.sin(hash + i) * 0.1f;
            }
            return vector;
        }
    }

    static class MockChatModel implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            AssistantMessage message = new AssistantMessage(
                    "Based on the provided context, here is a mock answer for local development.");
            Generation generation = new Generation(message);
            return new ChatResponse(List.of(generation));
        }
    }
}
