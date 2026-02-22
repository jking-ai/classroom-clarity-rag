# Local Development Guide

How to set up, run, and test Classroom Clarity RAG on your local machine.

---

## Prerequisites

| Tool | Version | Check |
|------|---------|-------|
| Java | 21+ | `java --version` |
| Docker | 20+ | `docker --version` |
| Docker Compose | v2+ | `docker compose version` |
| curl | any | `curl --version` |
| Python 3 | 3.x | `python3 --version` (for JSON formatting) |

> The Gradle wrapper (`./gradlew`) is included — you do not need Gradle installed separately.

---

## Two Modes: Real AI vs Mock AI

| Mode | Profile | AI Models | Credentials Required |
|------|---------|-----------|---------------------|
| **Real AI** | `local-ai` | Vertex AI (Gemini 2.0 Flash + text-embedding-005) | Yes — GCP service account |
| **Mock AI** | `local` | Deterministic mock embeddings + static chat response | No |

**Use Real AI** (`local-ai`) when you want actual LLM answers and production-quality embeddings. This is the recommended mode for testing the full experience.

**Use Mock AI** (`local`) when you just need to validate the pipeline mechanics (upload, chunk, store, query) without GCP credentials — useful for CI or quick iteration.

Both modes use the same local PostgreSQL database and local file storage.

---

## 1. Configure Credentials (for Real AI mode)

Skip this section if you only want Mock AI mode.

### Create a GCP Service Account

1. Go to the [GCP Console](https://console.cloud.google.com) and select your project
2. Navigate to **IAM & Admin > Service Accounts**
3. Click **Create Service Account**
4. Give it a name (e.g. `classroom-clarity-local`)
5. Grant the role **Vertex AI User** (`roles/aiplatform.user`)
6. Click **Done**, then click on the new service account
7. Go to the **Keys** tab and click **Add Key > Create new key > JSON**
8. Save the downloaded JSON file somewhere safe (e.g. `~/.gcp/classroom-clarity-sa.json`)

### Set up your `.env` file

```bash
cp .env.example .env
```

Edit `.env` with your values:

```bash
GOOGLE_APPLICATION_CREDENTIALS=/Users/yourname/.gcp/classroom-clarity-sa.json
GCP_PROJECT_ID=your-gcp-project-id
GCP_LOCATION=us-central1
```

> The `.env` file is gitignored and will never be committed.

### Enable the Vertex AI API

If you haven't already, enable the Vertex AI API in your GCP project:

```bash
gcloud services enable aiplatform.googleapis.com --project=your-gcp-project-id
```

Or enable it from the [GCP Console API Library](https://console.cloud.google.com/apis/library/aiplatform.googleapis.com).

---

## 2. Start the Application

### One command (recommended)

```bash
./scripts/start.sh
```

The script will:
1. Load your `.env` file (if it exists)
2. Auto-detect which mode to use:
   - If `GCP_PROJECT_ID` and `GOOGLE_APPLICATION_CREDENTIALS` are set → **Real AI** (`local-ai`)
   - Otherwise → **Mock AI** (`local`)
3. Start PostgreSQL via Docker Compose
4. Wait for the database to be ready
5. Start Spring Boot on `http://localhost:8080`

You can also force a specific mode:

```bash
./scripts/start.sh local       # Force mock AI
./scripts/start.sh local-ai    # Force real AI
```

Press `Ctrl+C` to shut down both the app and database. The script's cleanup trap handles stopping everything gracefully.

### Manual startup (step by step)

```bash
# 1. Load environment (if using real AI)
source .env

# 2. Start PostgreSQL with pgvector
docker compose up -d

# 3. Verify the database is running
docker exec classroom-clarity-db pg_isready -U postgres

# 4. Start the Spring Boot application
./gradlew bootRun --args='--spring.profiles.active=local-ai'   # real AI
# or
./gradlew bootRun --args='--spring.profiles.active=local'       # mock AI
```

### Verify it's running

```bash
curl -s http://localhost:8080/api/v1/health | python3 -m json.tool
```

Expected output:

```json
{
  "status": "UP",
  "components": {
    "database": { "status": "UP" },
    "embeddingModel": { "status": "UNKNOWN" },
    "chatModel": { "status": "UNKNOWN" },
    "storage": { "status": "UNKNOWN" }
  }
}
```

`database: UP` means everything is wired correctly.

You can also check the Spring Boot Actuator endpoint:

```bash
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
```

---

## 3. Test the Full Pipeline

### Step 1: Get a sample PDF

A sample PDF is included in the repository for testing:

```
bruno/sample-handbook.pdf
```

This contains a multi-chapter student handbook covering attendance, cell phone, dress code, grading, and cafeteria policies. You can also use any PDF you have.

### Step 2: Upload a document

```bash
DOC_ID=$(curl -s -X POST http://localhost:8080/api/v1/documents \
  -F "file=@bruno/sample-handbook.pdf" \
  -F "title=Student Handbook 2025-2026" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "Document ID: $DOC_ID"
```

Expected: a UUID is printed and `status: COMPLETED` in the response.

### Step 3: List documents

```bash
curl -s http://localhost:8080/api/v1/documents | python3 -m json.tool
```

### Step 4: Get a single document

```bash
curl -s http://localhost:8080/api/v1/documents/$DOC_ID | python3 -m json.tool
```

### Step 5: Query the document

```bash
curl -s -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the cell phone policy?"}' | python3 -m json.tool
```

**With Real AI (`local-ai`):** The `answer` field will contain an actual grounded response from Gemini, citing the relevant handbook sections.

**With Mock AI (`local`):** The `answer` will always be: _"Based on the provided context, here is a mock answer for local development."_ The `sources` array still shows which chunks matched.

**Query parameters** (all optional):

| Field | Default | Description |
|-------|---------|-------------|
| `topK` | `5` | Max number of chunks to retrieve |
| `similarityThreshold` | `0.7` | Minimum cosine similarity (0.0-1.0). Lower values return more results. |
| `documentIds` | `[]` | Filter to specific documents by UUID |

You can also filter queries to specific documents:

```bash
curl -s -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d "{
    \"question\": \"What is the grading policy?\",
    \"topK\": 3,
    \"similarityThreshold\": 0.0,
    \"documentIds\": [\"$DOC_ID\"]
  }" | python3 -m json.tool
```

### Step 6: Delete a document

```bash
curl -s -X DELETE http://localhost:8080/api/v1/documents/$DOC_ID -w "\nHTTP Status: %{http_code}\n"
```

Expected: `HTTP Status: 204` (No Content). This removes the document, all its chunks, and the stored PDF file.

Verify it's gone:

```bash
curl -s http://localhost:8080/api/v1/documents/$DOC_ID | python3 -m json.tool
```

Expected: 404 with `DOCUMENT_NOT_FOUND` error.

---

## 4. Run the Test Suite

Tests always use mock AI models and Testcontainers — no GCP credentials needed regardless of mode.

```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests 'com.jkingai.classroomclarity.controller.QueryControllerTest'

# Run with verbose output
./gradlew test --info
```

The test suite (40 tests) covers:
- Health endpoint
- Document upload, list, get, delete
- Semantic query with similarity search
- PDF extraction and text chunking
- Document ingestion pipeline
- Global exception handling
- JPA entity mapping

> Tests take about 15-20 seconds. The first run may be slower while Docker images are pulled.

---

## 5. Error Responses

All errors follow a consistent format:

```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable description",
  "timestamp": "2026-02-22T14:30:00Z",
  "path": "/api/v1/..."
}
```

| Code | HTTP Status | Cause |
|------|-------------|-------|
| `VALIDATION_ERROR` | 400 | Missing or invalid request fields |
| `INVALID_FILE_TYPE` | 400 | Uploaded file is not a PDF |
| `DOCUMENT_NOT_FOUND` | 404 | Document ID does not exist |
| `NO_RELEVANT_CONTEXT` | 422 | No chunks met the similarity threshold |
| `PROCESSING_FAILED` | 500 | Document ingestion failed |
| `LLM_SERVICE_ERROR` | 500 | Chat model call failed |

---

## 6. Database Access

Connect directly to the local database for debugging:

```bash
docker exec -it classroom-clarity-db psql -U postgres -d classroomclarity
```

Useful queries:

```sql
-- List all documents
SELECT id, title, status, chunk_count, page_count FROM documents;

-- Count chunks per document
SELECT d.title, COUNT(dc.id) as chunks
FROM documents d
JOIN document_chunks dc ON dc.document_id = d.id
GROUP BY d.title;

-- Check the pgvector extension
SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';

-- Preview chunk content (without the large embedding column)
SELECT id, document_id, chunk_index, page_number, token_count,
       LEFT(content, 100) AS content_preview
FROM document_chunks
ORDER BY chunk_index;
```

---

## 7. Resetting Local State

### Full reset (database + files)

Destroys the database volume and re-creates everything from scratch:

```bash
# Stop everything
docker compose down -v          # -v removes the pgdata volume

# Delete locally uploaded PDFs
rm -rf data/uploads/*

# Restart — Flyway will re-run all migrations
docker compose up -d
```

### Quick reset (data only, keep schema)

Keeps the container running and just clears the data:

```bash
docker exec -it classroom-clarity-db psql -U postgres -d classroomclarity -c "
  DELETE FROM document_chunks;
  DELETE FROM documents;
"
rm -rf data/uploads/*
```

### Restart the database container

If the container is in a bad state:

```bash
docker compose down
docker compose up -d
```

Verify it's healthy:

```bash
docker exec classroom-clarity-db pg_isready -U postgres
```

---

## 8. Troubleshooting

**Port 5432 already in use**
Another PostgreSQL instance is running. Stop it or change the port in `docker-compose.yml`.

```bash
# Find what's using port 5432
lsof -i :5432
```

**Docker container won't start**
Check Docker is running and the pgvector image is accessible:

```bash
docker pull pgvector/pgvector:pg16
```

**Tests fail with "Cannot connect to Docker"**
Testcontainers requires a running Docker daemon. Make sure Docker Desktop (or equivalent) is running.

**Application starts but health returns 503**
The database connection failed. Verify PostgreSQL is running:

```bash
docker compose ps
docker exec classroom-clarity-db pg_isready -U postgres
```

**Upload returns 400 INVALID_FILE_TYPE**
Only PDF files are accepted. Make sure the file has a `.pdf` extension and is a valid PDF.

**Query returns 422 NO_RELEVANT_CONTEXT**
Lower the `similarityThreshold` (try `0.0`) or upload a document with content relevant to the question.

**Real AI mode fails with authentication error**
Verify your `.env` file:
- `GOOGLE_APPLICATION_CREDENTIALS` points to a valid JSON key file
- `GCP_PROJECT_ID` matches your GCP project
- The service account has the **Vertex AI User** role
- The Vertex AI API is enabled in your project

```bash
# Test that credentials work
gcloud auth activate-service-account --key-file=$GOOGLE_APPLICATION_CREDENTIALS
gcloud ai models list --project=$GCP_PROJECT_ID --region=$GCP_LOCATION
```
