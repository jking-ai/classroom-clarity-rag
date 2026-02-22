package com.jkingai.classroomclarity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record ApiSecurityProperties(
        String apiKey,
        List<String> allowedOrigins,
        List<String> allowedIps
) {
    public ApiSecurityProperties {
        if (allowedOrigins == null) {
            allowedOrigins = List.of();
        }
        if (allowedIps == null) {
            allowedIps = List.of();
        }
    }
}
