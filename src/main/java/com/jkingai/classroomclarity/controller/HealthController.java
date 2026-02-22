package com.jkingai.classroomclarity.controller;

import com.jkingai.classroomclarity.dto.HealthResponse;
import com.jkingai.classroomclarity.dto.HealthResponse.ComponentHealth;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        Map<String, ComponentHealth> components = new LinkedHashMap<>();

        ComponentHealth dbHealth = checkDatabase();
        components.put("database", dbHealth);
        components.put("embeddingModel", ComponentHealth.unknown());
        components.put("chatModel", ComponentHealth.unknown());
        components.put("storage", ComponentHealth.unknown());

        String overallStatus = dbHealth.status().equals("UP") ? "UP" : "DOWN";
        HealthResponse response = new HealthResponse(overallStatus, components);

        int statusCode = overallStatus.equals("UP") ? 200 : 503;
        return ResponseEntity.status(statusCode).body(response);
    }

    private ComponentHealth checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(5)) {
                return ComponentHealth.up();
            }
            return ComponentHealth.down();
        } catch (Exception e) {
            return ComponentHealth.down();
        }
    }
}
