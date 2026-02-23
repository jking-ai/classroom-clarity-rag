package com.jkingai.classroomclarity.dto;

public record LimitsResponse(
        DocumentLimits documents,
        RateLimits rateLimit
) {
    public record DocumentLimits(long currentCount, int maxCount, int retentionDays) {}
    public record RateLimits(int requestsPerMinute, int queryRequestsPerMinute) {}
}
