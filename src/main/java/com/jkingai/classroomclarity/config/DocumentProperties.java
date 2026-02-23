package com.jkingai.classroomclarity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.documents")
public record DocumentProperties(
        int maxCount,
        int retentionDays
) {
    public DocumentProperties {
        if (maxCount <= 0) {
            maxCount = 100;
        }
        if (retentionDays <= 0) {
            retentionDays = 90;
        }
    }
}
