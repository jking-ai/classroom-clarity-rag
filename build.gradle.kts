plugins {
    java
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
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
        mavenBom("org.springframework.ai:spring-ai-bom:1.1.0")
        mavenBom("com.google.cloud:libraries-bom:26.55.0")
        mavenBom("org.testcontainers:testcontainers-bom:2.0.3")
    }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Spring AI (Phase 2+)
    implementation("org.springframework.ai:spring-ai-starter-model-vertex-ai-gemini")
    implementation("org.springframework.ai:spring-ai-starter-model-vertex-ai-embedding")
    implementation("org.springframework.ai:spring-ai-starter-vector-store-pgvector")

    // Google Cloud (Phase 2+)
    implementation("com.google.cloud:google-cloud-storage")

    // PDF processing (Phase 2+)
    implementation("org.apache.pdfbox:pdfbox:3.0.6")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
