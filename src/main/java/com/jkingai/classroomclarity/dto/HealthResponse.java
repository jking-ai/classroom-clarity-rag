package com.jkingai.classroomclarity.dto;

import java.util.Map;

public record HealthResponse(
        String status,
        Map<String, ComponentHealth> components
) {
    public record ComponentHealth(
            String status
    ) {
        public static ComponentHealth up() {
            return new ComponentHealth("UP");
        }

        public static ComponentHealth down() {
            return new ComponentHealth("DOWN");
        }

        public static ComponentHealth unknown() {
            return new ComponentHealth("UNKNOWN");
        }
    }
}
