# Local Testing Guide

How to run tests, use the Bruno API test suite, and reset your local environment.

---

## 1. Automated Test Suite

Tests use Testcontainers (spins up a temporary PostgreSQL container) and mock AI models. No GCP credentials or running database needed.

```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests 'com.jkingai.classroomclarity.controller.QueryControllerTest'

# Run tests matching a pattern
./gradlew test --tests '*DocumentController*'

# Verbose output
./gradlew test --info

# Force re-run (skip cache)
./gradlew test --rerun
```

### Test classes (40 tests across 10 classes)

| Class | Tests | What it covers |
|-------|-------|----------------|
| `HealthControllerTest` | 1 | Health endpoint returns UP |
| `DocumentControllerUploadTest` | 3 | PDF upload, invalid file rejection |
| `DocumentControllerCrudTest` | 6 | List, get, delete, pagination, 404s |
| `QueryControllerTest` | 3 | Semantic query, blank question validation, document filter |
| `DocumentIngestionServiceTest` | 2 | Full ingestion pipeline with mock embeddings |
| `PdfExtractionServiceTest` | 4 | PDF text extraction, multi-page, invalid files |
| `ChunkingServiceTest` | 6 | Token-based chunking, overlap, edge cases |
| `LocalStorageServiceTest` | 5 | File store, retrieve, delete, directory creation |
| `GlobalExceptionHandlerTest` | 7 | All error response formats |
| `DocumentEntityTest` | 3 | JPA entity mapping and relationships |

### Test reports

After running tests, view the HTML report:

```bash
open build/reports/tests/test/index.html
```

---

## 2. Bruno API Test Suite

A [Bruno](https://www.usebruno.com/) collection is included at `bruno/` for interactive API testing against a running instance.

### Setup

1. Install Bruno 3.1+ from [usebruno.com](https://www.usebruno.com/)
2. Open Bruno and click **Open Collection**
3. Navigate to the `bruno/` folder in this repo

### Environment

The collection includes a `local` environment pre-configured with:

- `host`: `http://localhost:8080`

Select the **local** environment in Bruno before running requests.

### Running the tests

Start the app first (see [Local Development Guide](local-dev-guide.md)):

```bash
./scripts/start.sh
```

Then run requests in Bruno. The collection is organized into three folders:

#### Health

| Request | Method | Description |
|---------|--------|-------------|
| Health Check | `GET /api/v1/health` | Verify the app is running |

#### Documents

Run these in order — later requests depend on the `documentId` variable set by the upload request:

| # | Request | Method | Description |
|---|---------|--------|-------------|
| 1 | Upload Document | `POST /api/v1/documents` | Uploads `sample-handbook.pdf`, stores `documentId` |
| 2 | List Documents | `GET /api/v1/documents` | Paginated document list |
| 3 | Get Document | `GET /api/v1/documents/{{documentId}}` | Single document by ID |
| 4 | Upload Invalid File | `POST /api/v1/documents` | Expects 400 — sends a `.txt` file |
| 5 | Get Document Not Found | `GET /api/v1/documents/{random-uuid}` | Expects 404 |
| 6 | Delete Document | `DELETE /api/v1/documents/{{documentId}}` | Expects 204 |
| 7 | Delete Document Not Found | `DELETE /api/v1/documents/{random-uuid}` | Expects 404 |
| 8 | Verify Deleted | `GET /api/v1/documents/{{documentId}}` | Confirms 404 after delete |

#### Query

This folder has its own setup/teardown — the first request uploads a document, and the last one deletes it:

| # | Request | Method | Description |
|---|---------|--------|-------------|
| 1 | Setup - Upload Document | `POST /api/v1/documents` | Uploads test PDF |
| 2 | Query - Basic | `POST /api/v1/query` | "What is the cell phone policy?" |
| 3 | Query - Filter by Document | `POST /api/v1/query` | Query with `documentIds` filter |
| 4 | Query - Attendance | `POST /api/v1/query` | "What is the attendance policy?" |
| 5 | Query - Dress Code | `POST /api/v1/query` | "What is the dress code?" |
| 6 | Query - No Relevant Context | `POST /api/v1/query` | Expects 422 for unrelated question |
| 7 | Query - Blank Question | `POST /api/v1/query` | Expects 400 for empty question |
| 8 | Teardown - Delete Document | `DELETE /api/v1/documents/{{documentId}}` | Cleanup |

### Test fixtures

| File | Purpose |
|------|---------|
| `bruno/sample-handbook.pdf` | Multi-chapter student handbook (attendance, phones, dress code, grading, cafeteria) |
| `bruno/not-a-pdf.txt` | Plain text file for testing invalid upload rejection |

---

## 3. Manual API Testing with curl

If you prefer curl over Bruno, here's the full workflow. Start the app first.

### Upload a document

```bash
curl -s -X POST http://localhost:8080/api/v1/documents \
  -F "file=@bruno/sample-handbook.pdf" \
  -F "title=Student Handbook 2025-2026" | python3 -m json.tool
```

Save the `id` from the response:

```bash
DOC_ID="paste-the-uuid-here"
```

### List documents

```bash
curl -s http://localhost:8080/api/v1/documents | python3 -m json.tool
```

### Query

```bash
curl -s -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the attendance policy?"}' | python3 -m json.tool
```

With optional parameters:

```bash
curl -s -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d "{
    \"question\": \"What is the grading policy?\",
    \"topK\": 3,
    \"similarityThreshold\": 0.5,
    \"documentIds\": [\"$DOC_ID\"]
  }" | python3 -m json.tool
```

### Delete a document

```bash
curl -s -X DELETE http://localhost:8080/api/v1/documents/$DOC_ID -w "\nHTTP Status: %{http_code}\n"
```

---

## 4. Resetting Your Environment

### Reset everything (database + uploaded files)

Destroys the database volume and all data. Flyway re-runs migrations on next startup.

```bash
# Stop the app (Ctrl+C if using start.sh)

# Destroy database container and volume
docker compose down -v

# Delete uploaded PDFs
rm -rf data/uploads/*

# Restart
./scripts/start.sh
```

### Reset data only (keep the database running)

Clears all documents and chunks without restarting the container:

```bash
docker exec -it classroom-clarity-db psql -U postgres -d classroomclarity -c "
  DELETE FROM document_chunks;
  DELETE FROM documents;
"
rm -rf data/uploads/*
```

### Restart the database container

If the container is in a bad state or you need to pull a new image:

```bash
docker compose down
docker compose up -d
```

Verify it's healthy:

```bash
docker exec classroom-clarity-db pg_isready -U postgres
```

### Inspect the database

Connect directly to PostgreSQL for debugging:

```bash
docker exec -it classroom-clarity-db psql -U postgres -d classroomclarity
```

Useful queries:

```sql
-- List all documents
SELECT id, title, status, chunk_count, page_count FROM documents;

-- Count chunks per document
SELECT d.title, COUNT(dc.id) AS chunks
FROM documents d
JOIN document_chunks dc ON dc.document_id = d.id
GROUP BY d.title;

-- Preview chunk content (without the large embedding column)
SELECT id, document_id, chunk_index, page_number, token_count,
       LEFT(content, 100) AS content_preview
FROM document_chunks
ORDER BY chunk_index;

-- Check pgvector extension
SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';
```

---

## 5. Troubleshooting Tests

**Tests fail with "Cannot connect to Docker"**
Testcontainers requires Docker to be running. Start Docker Desktop (or equivalent).

**Tests are slow on first run**
Testcontainers pulls the `pgvector/pgvector:pg16` image on the first run. Subsequent runs reuse the cached image.

**A single test fails intermittently**
Run it in isolation to check:

```bash
./gradlew test --tests 'com.jkingai.classroomclarity.controller.DocumentControllerCrudTest.listDocumentsReturnsPaginatedResults'
```

**Bruno requests fail with connection refused**
The app isn't running. Start it with `./scripts/start.sh` first.

**Upload returns 400 INVALID_FILE_TYPE**
Only PDF files are accepted. Make sure you're uploading `sample-handbook.pdf`, not `not-a-pdf.txt`.

**Query returns 422 NO_RELEVANT_CONTEXT**
The similarity threshold (default `0.7`) is too high for the query. Either:
- Lower it: `"similarityThreshold": 0.3`
- Upload a document with content relevant to your question

**Query returns 500 LLM_SERVICE_ERROR (Real AI mode)**
Check your GCP credentials and that the Vertex AI API is enabled. See the [Local Development Guide](local-dev-guide.md#1-configure-credentials-for-real-ai-mode).
