package com.jkingai.classroomclarity.controller;

import com.jkingai.classroomclarity.config.DocumentProperties;
import com.jkingai.classroomclarity.config.RateLimitProperties;
import com.jkingai.classroomclarity.dto.LimitsResponse;
import com.jkingai.classroomclarity.dto.LimitsResponse.DocumentLimits;
import com.jkingai.classroomclarity.dto.LimitsResponse.RateLimits;
import com.jkingai.classroomclarity.repository.DocumentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class LimitsController {

    private final DocumentRepository documentRepository;
    private final DocumentProperties documentProperties;
    private final RateLimitProperties rateLimitProperties;

    public LimitsController(DocumentRepository documentRepository,
                            DocumentProperties documentProperties,
                            RateLimitProperties rateLimitProperties) {
        this.documentRepository = documentRepository;
        this.documentProperties = documentProperties;
        this.rateLimitProperties = rateLimitProperties;
    }

    @GetMapping("/limits")
    public ResponseEntity<LimitsResponse> getLimits() {
        long currentCount = documentRepository.count();

        var documents = new DocumentLimits(
                currentCount,
                documentProperties.maxCount(),
                documentProperties.retentionDays()
        );

        var rateLimit = new RateLimits(
                rateLimitProperties.requestsPerMinute(),
                rateLimitProperties.queryRequestsPerMinute()
        );

        return ResponseEntity.ok(new LimitsResponse(documents, rateLimit));
    }
}
