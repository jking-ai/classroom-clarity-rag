package com.jkingai.classroomclarity.config;

import com.jkingai.classroomclarity.TestApiKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.requests-per-minute=2",
        "app.rate-limit.burst-capacity=2",
        "app.rate-limit.query-requests-per-minute=1",
        "app.rate-limit.query-burst-capacity=1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(TestAiConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class RateLimitFilterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsRateLimitExceededAfterBurstCapacity() throws Exception {
        // First two requests should succeed (burst capacity = 2)
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/api/v1/documents")
                            .header(TestApiKey.HEADER, TestApiKey.VALUE))
                    .andExpect(status().isOk());
        }

        // Third request should be rate limited
        mockMvc.perform(get("/api/v1/documents")
                        .header(TestApiKey.HEADER, TestApiKey.VALUE))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(header().string("Retry-After", "60"));
    }

    @Test
    void healthEndpointIsExemptFromRateLimiting() throws Exception {
        // Exhaust the general rate limit first
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/documents")
                    .header(TestApiKey.HEADER, TestApiKey.VALUE));
        }

        // Health endpoint should still work
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }
}
