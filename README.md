# Classroom Clarity RAG

A semantic Q&A API for school documents, powered by Retrieval-Augmented Generation (RAG).

Teachers, administrators, and parents can ask natural-language questions about school handbooks, curriculum maps, and policy documents — and get grounded answers with source citations.

## Tech Stack

- **Java 21** / **Spring Boot 3.5.x** / **Spring AI 1.1.x**
- **PostgreSQL 16** with **pgvector** for vector similarity search
- **Vertex AI** — text-embedding-005 (embeddings) + Gemini 2.0 Flash (chat)
- **Google Cloud Run** / **Cloud SQL** / **Cloud Storage**
- **Flyway** for database migrations
- **Testcontainers** for integration testing

## Getting Started

### Prerequisites

- Java 21
- Docker (for local PostgreSQL with pgvector)
- Gradle 9.x (wrapper included)

### Local Development

```bash
# Start PostgreSQL with pgvector
docker compose up -d

# Run the application
./gradlew bootRun --args='--spring.profiles.active=local'

# Run tests
./gradlew test
```

### Health Check

```bash
curl http://localhost:8080/api/v1/health
```

## Documentation

- [Local Development Guide](docs/local-dev-guide.md) — setup, run, and configure locally
- [Local Testing Guide](docs/local-testing-guide.md) — automated tests, Bruno suite, curl examples, and reset procedures
- [Project Overview](docs/README.md)
- [Architecture](docs/architecture.md)
- [API Contracts](docs/api-contracts.md)
- [Milestones](docs/milestones.md)

## License

Private — All rights reserved.
