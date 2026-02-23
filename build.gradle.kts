plugins {
    java
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.google.cloud.tools.jib") version "3.4.4"
}

group = "com.jkingai"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:1.1.2")
        mavenBom("org.testcontainers:testcontainers-bom:2.0.3")
    }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.google.cloud.sql:postgres-socket-factory:1.24.1")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Spring AI (Phase 2+)
    implementation("org.springframework.ai:spring-ai-starter-model-vertex-ai-gemini")
    implementation("org.springframework.ai:spring-ai-starter-model-vertex-ai-embedding")
    implementation("org.springframework.ai:spring-ai-starter-vector-store-pgvector")

    // Google Cloud (Phase 2+)
    implementation("com.google.cloud:google-cloud-storage:2.46.0")

    // PDF processing (Phase 2+)
    implementation("org.apache.pdfbox:pdfbox:3.0.6")

    // Rate limiting
    implementation("com.bucket4j:bucket4j-core:8.10.1")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jib {
    from {
        image = "eclipse-temurin:21-jre"
    }
    to {
        image = "<REGION>-docker.pkg.dev/<YOUR_GCP_PROJECT>/<YOUR_ARTIFACT_REPO>/classroom-clarity-rag"
        tags = setOf("latest", version.toString())
    }
    container {
        mainClass = "com.jkingai.classroomclarity.ClassroomClarityApplication"
        jvmFlags = listOf(
            "-XX:+UseG1GC",
            "-XX:MaxRAMPercentage=75.0"
        )
        ports = listOf("8080")
        environment = mapOf("SPRING_PROFILES_ACTIVE" to "prod")
        creationTime.set("USE_CURRENT_TIMESTAMP")
    }
}
