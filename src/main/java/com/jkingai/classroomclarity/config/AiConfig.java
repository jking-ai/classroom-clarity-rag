package com.jkingai.classroomclarity.config;

import org.springframework.ai.vertexai.embedding.VertexAiEmbeddingConnectionDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"prod", "local-ai"})
public class AiConfig {

    // Workaround for Spring AI 1.0.0 bug: the VertexAiEmbeddingConnectionAutoConfiguration
    // has an incorrect @ConditionalOnClass check that prevents this bean from being created.
    // See: https://github.com/spring-projects/spring-ai/issues/4800
    @Bean
    @ConditionalOnMissingBean
    VertexAiEmbeddingConnectionDetails vertexAiEmbeddingConnectionDetails(
            @Value("${spring.ai.vertex.ai.embedding.project-id}") String projectId,
            @Value("${spring.ai.vertex.ai.embedding.location:us-central1}") String location) {
        return VertexAiEmbeddingConnectionDetails.builder()
                .projectId(projectId)
                .location(location)
                .build();
    }
}
