package com.jkingai.classroomclarity.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
public class AiConfig {
    // In prod profile, Spring AI autoconfiguration handles Vertex AI beans.
    // This class exists as a marker and future customization point.
}
