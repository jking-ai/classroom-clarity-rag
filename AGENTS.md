# Classroom Clarity RAG — Agent Instructions

This is the source of truth for all AI coding agents working on this project (Claude Code, Copilot, Cursor, etc.).

---

## Project Overview

A semantic Q&A API for school documents, powered by Retrieval-Augmented Generation (RAG). Java 21 / Spring Boot 3.5.x / Spring AI 1.1.x / PostgreSQL 16 with pgvector / Vertex AI (Gemini 3.1 Flash-Lite + text-embedding-005).

---

## Workflow Rules

- **Always use feature branches.** Branch from the latest `main` for all code changes. Never commit directly to `main`.
- **Ask questions for clarity.** Never make assumptions about requirements or approach. If something is ambiguous, ask.
- **Run tests before committing.** All 40+ tests must pass (`./gradlew test`) before pushing.
- **Keep PRs focused.** One logical change per PR. Don't bundle unrelated changes.
- **Commit messages should explain "why", not "what".** The diff shows what changed; the message should explain the reason.

---

## API Spec Sync Rule

The OpenAPI spec at `docs/openapi.yaml` is the contract for frontend consumers.

**When any of the following change, you MUST update `docs/openapi.yaml` to match:**
- Controller request/response mappings (`@GetMapping`, `@PostMapping`, etc.)
- DTO fields (add/remove/rename fields in any record in `dto/`)
- Query parameter names, types, or defaults
- Error codes or HTTP status codes in `GlobalExceptionHandler`
- Validation rules (`@NotBlank`, `@Valid`, compact constructor defaults)

**Do not let the spec drift from the implementation.** If you change the API, update the spec in the same commit.

---

## Project Structure

```
src/main/java/com/jkingai/classroomclarity/
  config/         Spring @Configuration classes (profile-specific beans)
  controller/     REST controllers (@RestController, @RequestMapping)
  dto/            Data Transfer Objects (Java records)
  exception/      Custom exceptions + GlobalExceptionHandler
  model/          JPA entities (@Entity) and enums
  repository/     Spring Data JPA repositories
  service/        Business logic layer

src/main/resources/
  application.yml             Common config (all profiles)
  application-local.yml       Local dev with mock AI
  application-local-ai.yml    Local dev with real Vertex AI
  application-prod.yml        Production (Cloud Run)
  application-test.yml        Test profile (autoconfig exclusions)
  db/migration/               Flyway SQL migrations (V1__, V2__, ...)

src/test/java/                Tests mirror the main source structure
src/test/resources/           Test-specific config

docs/
  openapi.yaml                OpenAPI 3.1 spec (keep in sync with API)
  api-contracts.md            Detailed API documentation
  local-dev-guide.md          Setup and run locally
  local-testing-guide.md      Testing and reset procedures
  architecture.md             System architecture
  milestones.md               Project roadmap

bruno/                        Bruno 3.1 API test suite
scripts/                      Shell scripts (start.sh)
```

---

## Coding Standards

### Java Conventions

- **Java 21** — use modern features: records, pattern matching, sealed classes where appropriate.
- **Constructor injection** — never use `@Autowired` on fields. Declare dependencies as `private final` fields.
- **DTOs are Java records** — immutable, with static factory methods (e.g., `DocumentUploadResponse.from(Document)`). Use compact constructors for defaults and validation (see `QueryRequest`).
- **Exceptions extend `RuntimeException`** — unchecked exceptions, handled by `GlobalExceptionHandler`.
- **Loggers** — `private static final Logger log = LoggerFactory.getLogger(ClassName.class);`

### Naming

| Type | Convention | Example |
|------|-----------|---------|
| Controller | `*Controller` | `DocumentController` |
| Service | `*Service` | `DocumentIngestionService` |
| Repository | `*Repository` | `DocumentChunkRepository` |
| Request DTO | `*Request` | `QueryRequest` |
| Response DTO | `*Response` | `DocumentUploadResponse` |
| Exception | `*Exception` | `DocumentNotFoundException` |
| Config | `*Config` | `AiConfig` |
| Flyway migration | `V{n}__{description}.sql` | `V3__create_hnsw_index.sql` |

### API Conventions

- All endpoints are under `/api/v1/`.
- Request/response bodies use `application/json` except document upload (`multipart/form-data`).
- Error responses always use the `ErrorResponse` schema (error code, message, timestamp, path).
- Pagination uses `page` (zero-based), `size` (max 100), `sort` (field,direction).

### Security

- **API key authentication** — all endpoints except health probes require a valid `X-API-Key` header.
- **Public endpoints** (no API key required, exempt from rate limiting): `/api/v1/health`, `/api/v1/limits`, `/actuator/**`.
- **CORS** — allowed origins are configured per profile via `app.security.allowed-origins`.
- **Unauthorized requests** return `401` with the standard `ErrorResponse` schema and error code `UNAUTHORIZED`.
- **Rate limiting** — per-IP token bucket via `RateLimitFilter` (bucket4j). General: 60 req/min, Query: 6 req/min. Configurable via `app.rate-limit.*` properties.
- **Document limits** — max document count and auto-delete retention enforced via `DocumentProperties` (`app.documents.max-count`, `app.documents.retention-days`).
- Configuration lives in `ApiSecurityProperties`, `ApiKeyAuthenticationFilter`, `RateLimitFilter`, `DocumentProperties`, `RateLimitProperties`, and `SecurityConfig` (all in `config/`).

### Error Handling

All errors go through `GlobalExceptionHandler`. Each custom exception maps to a specific HTTP status and error code:

| Exception | HTTP Status | Error Code |
|-----------|-------------|------------|
| *(missing/invalid API key)* | 401 | `UNAUTHORIZED` |
| `DocumentNotFoundException` | 404 | `DOCUMENT_NOT_FOUND` |
| `DocumentLimitExceededException` | 409 | `DOCUMENT_LIMIT_EXCEEDED` |
| `InvalidFileTypeException` | 400 | `INVALID_FILE_TYPE` |
| `NoRelevantContextException` | 422 | `NO_RELEVANT_CONTEXT` |
| `LlmServiceException` | 502 | `LLM_ERROR` |
| `DocumentProcessingException` | 500 | `PROCESSING_FAILED` |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` |
| `MaxUploadSizeExceededException` | 400 | `FILE_TOO_LARGE` |

---

## Spring Profiles

| Profile | Purpose | AI Models | Storage |
|---------|---------|-----------|---------|
| `local` | Local dev (no GCP) | Mock embedding + mock chat | Local filesystem |
| `local-ai` | Local dev (real AI) | Vertex AI (Gemini + text-embedding-005) | Local filesystem |
| `test` | Automated tests | Mock beans via `TestAiConfig` | Local filesystem |
| `prod` | Production | Vertex AI | Google Cloud Storage |

Config classes use `@Profile` to activate the right beans. Mock and test profiles exclude Vertex AI autoconfigurations in their YAML files.

---

## Testing Requirements

- Tests use **Testcontainers** for PostgreSQL with pgvector — no external DB needed.
- Tests use **mock AI models** — no GCP credentials needed.
- All test classes are annotated with `@ActiveProfiles("test")` and `@Import(TestAiConfig.class)`.
- Use `@SpringBootTest` + `@AutoConfigureMockMvc` for controller integration tests.
- Use `deleteAllInBatch()` (not `deleteAll()`) when clearing entities that have pgvector columns.
- Test class naming: `*Test` (e.g., `DocumentControllerCrudTest`, `ChunkingServiceTest`).

---

## Database

- **PostgreSQL 16 with pgvector** extension.
- Migrations managed by **Flyway** in `src/main/resources/db/migration/`.
- Migration naming: `V{number}__{description}.sql` (double underscore).
- Embeddings stored as `vector(768)` (text-embedding-005 dimension).
- HNSW index on the embedding column for fast cosine similarity search.
- Foreign keys use `ON DELETE CASCADE` (deleting a document removes its chunks).

---

## Key Files

| File | Purpose |
|------|---------|
| `build.gradle.kts` | Dependencies and build config |
| `docker-compose.yml` | Local PostgreSQL with pgvector |
| `scripts/start.sh` | One-command local startup |
| `.env.example` | Template for GCP credentials |
| `docs/openapi.yaml` | OpenAPI spec (KEEP IN SYNC) |
