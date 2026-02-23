package com.jkingai.classroomclarity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int requestsPerMinute,
        int burstCapacity,
        int queryRequestsPerMinute,
        int queryBurstCapacity
) {
    public RateLimitProperties {
        if (requestsPerMinute <= 0) {
            requestsPerMinute = 60;
        }
        if (burstCapacity <= 0) {
            burstCapacity = 10;
        }
        if (queryRequestsPerMinute <= 0) {
            queryRequestsPerMinute = 20;
        }
        if (queryBurstCapacity <= 0) {
            queryBurstCapacity = 5;
        }
    }
}
