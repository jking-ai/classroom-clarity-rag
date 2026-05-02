# API Contracts -- Classroom Clarity RAG

Base URL: `/api/v1`

All request and response bodies use `application/json` unless otherwise noted. Document upload uses `multipart/form-data`.

---

## Endpoints Overview

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/documents` | Upload and ingest a PDF document |
| GET | `/api/v1/documents` | List all ingested documents |
| GET | `/api/v1/documents/{id}` | Get a single document's metadata |
| DELETE | `/api/v1/documents/{id}` | Delete a document and its chunks |
| POST | `/api/v1/query` | Ask a question against ingested documents |
| GET | `/api/v1/health` | Health check |

---

## 1. Upload Document

**`POST /api/v1/documents`**

Uploads a PDF file, extracts text, generates embeddings, and stores everything in the database.

### Request

Content-Type: `multipart/form-data`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | file (PDF) | Yes | The PDF document to ingest |
| `title` | string | No | Human-readable title. Defaults to filename if omitted. |
| `description` | string | No | Optional description of the document |

### Example Request (cURL)

```bash
curl -X POST http://localhost:8080/api/v1/documents \
  -F "file=@student-handbook-2025.pdf" \
  -F "title=Student Handbook 2025-2026" \
  -F "description=Policies, procedures, and expectations for enrolled students"
```

### Response -- 201 Created

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "title": "Student Handbook 2025-2026",
  "description": "Policies, procedures, and expectations for enrolled students",
  "filename": "student-handbook-2025.pdf",
  "fileSize": 2048576,
  "pageCount": 42,
  "chunkCount": 187,
  "status": "COMPLETED",
  "createdAt": "2026-02-22T14:30:00Z",
  "updatedAt": "2026-02-22T14:30:45Z"
}
```

### Response -- 400 Bad Request (invalid file type)

```json
{
  "error": "INVALID_FILE_TYPE",
  "message": "Only PDF files are accepted. Received: application/vnd.ms-excel",
  "timestamp": "2026-02-22T14:30:00Z",
  "path": "/api/v1/documents"
}
```

---

## 2. List Documents

**`GET /api/v1/documents`**

Returns a list of all ingested documents with metadata.

### Query Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | 0 | Zero-based page index |
| `size` | integer | 20 | Number of items per page (max 100) |
| `sort` | string | `createdAt,desc` | Sort field and direction |

### Example Request

```bash
curl http://localhost:8080/api/v1/documents?page=0&size=10
```

### Response -- 200 OK

```json
{
  "content": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "title": "Student Handbook 2025-2026",
      "description": "Policies, procedures, and expectations for enrolled students",
      "filename": "student-handbook-2025.pdf",
      "fileSize": 2048576,
      "pageCount": 42,
      "chunkCount": 187,
      "status": "COMPLETED",
      "createdAt": "2026-02-22T14:30:00Z",
      "updatedAt": "2026-02-22T14:30:45Z"
    },
    {
      "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
      "title": "K-5 Math Curriculum Map",
      "description": "Scope and sequence for elementary mathematics",
      "filename": "k5-math-curriculum.pdf",
      "fileSize": 1524288,
      "pageCount": 28,
      "chunkCount": 112,
      "status": "COMPLETED",
      "createdAt": "2026-02-21T10:15:00Z",
      "updatedAt": "2026-02-21T10:16:20Z"
    }
  ],
  "page": {
    "number": 0,
    "size": 10,
    "totalElements": 2,
    "totalPages": 1
  }
}
```

---

## 3. Get Document

**`GET /api/v1/documents/{id}`**

Returns metadata for a single document.

### Example Request

```bash
curl http://localhost:8080/api/v1/documents/a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

### Response -- 200 OK

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "title": "Student Handbook 2025-2026",
  "description": "Policies, procedures, and expectations for enrolled students",
  "filename": "student-handbook-2025.pdf",
  "fileSize": 2048576,
  "pageCount": 42,
  "chunkCount": 187,
  "status": "COMPLETED",
  "createdAt": "2026-02-22T14:30:00Z",
  "updatedAt": "2026-02-22T14:30:45Z"
}
```

### Response -- 404 Not Found

```json
{
  "error": "DOCUMENT_NOT_FOUND",
  "message": "No document found with id: a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "timestamp": "2026-02-22T14:35:00Z",
  "path": "/api/v1/documents/a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

## 4. Delete Document

**`DELETE /api/v1/documents/{id}`**

Deletes a document, all its chunks and embeddings from the database, and the raw PDF from Cloud Storage.

### Example Request

```bash
curl -X DELETE http://localhost:8080/api/v1/documents/a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

### Response -- 204 No Content

No response body.

### Response -- 404 Not Found

```json
{
  "error": "DOCUMENT_NOT_FOUND",
  "message": "No document found with id: a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "timestamp": "2026-02-22T14:40:00Z",
  "path": "/api/v1/documents/a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

## 5. Query (Ask a Question)

**`POST /api/v1/query`**

Submits a natural-language question. The system embeds the question, performs a similarity search over document chunks, retrieves relevant context, and generates a grounded answer using an LLM.

### Request Body

```json
{
  "question": "What is the school's policy on cell phone use during class?",
  "topK": 5,
  "similarityThreshold": 0.7,
  "documentIds": []
}
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `question` | string | Yes | -- | The natural-language question to answer |
| `topK` | integer | No | 5 | Number of most-similar chunks to retrieve |
| `similarityThreshold` | float | No | 0.7 | Minimum cosine similarity score (0.0-1.0) for a chunk to be included |
| `documentIds` | array of strings | No | `[]` (all documents) | Restrict search to specific document IDs. Empty array searches all documents. |

### Example Request

```bash
curl -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is the school policy on cell phone use during class?",
    "topK": 5,
    "similarityThreshold": 0.7
  }'
```

### Response -- 200 OK

```json
{
  "answer": "According to the Student Handbook, cell phones must be turned off and stored in backpacks during instructional time. Students may use phones during lunch and before/after school. Repeated violations result in the device being confiscated and held in the front office until a parent retrieves it.",
  "sources": [
    {
      "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "documentTitle": "Student Handbook 2025-2026",
      "chunkId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
      "content": "Section 4.2 - Electronic Devices: All personal electronic devices, including cell phones, must be turned off and stored in backpacks during instructional time. Students may use personal devices during the lunch period and before or after school hours only.",
      "pageNumber": 15,
      "similarityScore": 0.92
    },
    {
      "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "documentTitle": "Student Handbook 2025-2026",
      "chunkId": "d4e5f6a7-b8c9-0123-defa-234567890123",
      "content": "Section 4.2.1 - Consequences for Device Policy Violations: First offense results in a verbal warning. Second offense: the device is confiscated and held in the main office until the end of the school day. Third and subsequent offenses: the device is confiscated and a parent or guardian must retrieve it from the front office.",
      "pageNumber": 16,
      "similarityScore": 0.87
    }
  ],
  "metadata": {
    "model": "gemini-3.1-flash-lite-preview",
    "chunksRetrieved": 2,
    "chunksConsidered": 5,
    "processingTimeMs": 1245
  }
}
```

### Response -- 400 Bad Request (missing question)

```json
{
  "error": "VALIDATION_ERROR",
  "message": "The 'question' field is required and must not be blank.",
  "timestamp": "2026-02-22T15:00:00Z",
  "path": "/api/v1/query"
}
```

### Response -- 422 Unprocessable Entity (no relevant chunks found)

```json
{
  "error": "NO_RELEVANT_CONTEXT",
  "message": "No document chunks met the similarity threshold of 0.7 for the given question. Try lowering the threshold or uploading more relevant documents.",
  "timestamp": "2026-02-22T15:05:00Z",
  "path": "/api/v1/query"
}
```

---

## 6. Health Check

**`GET /api/v1/health`**

Returns the health status of the application and its dependencies.

### Example Request

```bash
curl http://localhost:8080/api/v1/health
```

### Response -- 200 OK

```json
{
  "status": "UP",
  "components": {
    "database": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "version": "16.x",
        "pgvectorEnabled": true
      }
    },
    "embeddingModel": {
      "status": "UP",
      "details": {
        "model": "text-embedding-005",
        "provider": "vertexai"
      }
    },
    "chatModel": {
      "status": "UP",
      "details": {
        "model": "gemini-3.1-flash-lite-preview",
        "provider": "vertexai"
      }
    },
    "storage": {
      "status": "UP",
      "details": {
        "bucket": "classroom-clarity-documents"
      }
    }
  }
}
```

### Response -- 503 Service Unavailable

```json
{
  "status": "DOWN",
  "components": {
    "database": {
      "status": "DOWN",
      "details": {
        "error": "Connection refused"
      }
    },
    "embeddingModel": {
      "status": "UP",
      "details": {
        "model": "text-embedding-005",
        "provider": "vertexai"
      }
    },
    "chatModel": {
      "status": "UP",
      "details": {
        "model": "gemini-3.1-flash-lite-preview",
        "provider": "vertexai"
      }
    },
    "storage": {
      "status": "UP",
      "details": {
        "bucket": "classroom-clarity-documents"
      }
    }
  }
}
```

---

## Error Response Format

All error responses follow a consistent structure:

```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable description of what went wrong.",
  "timestamp": "2026-02-22T15:00:00Z",
  "path": "/api/v1/endpoint"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `error` | string | Machine-readable error code (e.g., `DOCUMENT_NOT_FOUND`, `VALIDATION_ERROR`) |
| `message` | string | Human-readable error description |
| `timestamp` | string (ISO 8601) | When the error occurred |
| `path` | string | The request path that triggered the error |

### Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Request body failed validation |
| `INVALID_FILE_TYPE` | 400 | Uploaded file is not a PDF |
| `FILE_TOO_LARGE` | 400 | Uploaded file exceeds the 50 MB limit |
| `DOCUMENT_NOT_FOUND` | 404 | No document exists with the given ID |
| `NO_RELEVANT_CONTEXT` | 422 | No chunks met the similarity threshold for the query |
| `PROCESSING_FAILED` | 500 | Document ingestion failed during extraction or embedding |
| `LLM_ERROR` | 502 | The upstream LLM or embedding service returned an error |
| `SERVICE_UNAVAILABLE` | 503 | A required dependency (database, AI service) is unreachable |

---

## Data Models (PostgreSQL with pgvector)

### Enable pgvector Extension

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### Table: `documents`

Stores metadata about each uploaded document.

```sql
CREATE TABLE documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    filename        VARCHAR(500) NOT NULL,
    file_size       BIGINT NOT NULL,
    page_count      INTEGER,
    chunk_count     INTEGER DEFAULT 0,
    storage_path    VARCHAR(1000) NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'PROCESSING',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_created_at ON documents(created_at DESC);
```

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key, auto-generated |
| `title` | VARCHAR(500) | Human-readable document title |
| `description` | TEXT | Optional description |
| `filename` | VARCHAR(500) | Original uploaded filename |
| `file_size` | BIGINT | File size in bytes |
| `page_count` | INTEGER | Number of pages in the PDF |
| `chunk_count` | INTEGER | Number of text chunks generated |
| `storage_path` | VARCHAR(1000) | Cloud Storage path to the raw PDF |
| `status` | VARCHAR(50) | Processing status: `PROCESSING`, `COMPLETED`, `FAILED` |
| `created_at` | TIMESTAMPTZ | When the document was uploaded |
| `updated_at` | TIMESTAMPTZ | Last modification timestamp |

### Table: `document_chunks`

Stores text chunks with their embedding vectors.

```sql
CREATE TABLE document_chunks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    content         TEXT NOT NULL,
    page_number     INTEGER,
    chunk_index     INTEGER NOT NULL,
    token_count     INTEGER,
    embedding       vector(768) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chunks_document_id ON document_chunks(document_id);
CREATE INDEX idx_chunks_embedding ON document_chunks
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 200);
```

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key, auto-generated |
| `document_id` | UUID | Foreign key to `documents.id` (cascading delete) |
| `content` | TEXT | The text content of this chunk |
| `page_number` | INTEGER | Source page number in the PDF (1-indexed) |
| `chunk_index` | INTEGER | Order of this chunk within the document (0-indexed) |
| `token_count` | INTEGER | Approximate token count for this chunk |
| `embedding` | vector(768) | Embedding vector from text-embedding-005 |
| `created_at` | TIMESTAMPTZ | When this chunk was created |

### Similarity Search Query

The core vector search query used by the query endpoint:

```sql
SELECT dc.id, dc.content, dc.page_number, dc.document_id,
       d.title AS document_title,
       1 - (dc.embedding <=> :queryEmbedding) AS similarity_score
FROM document_chunks dc
JOIN documents d ON d.id = dc.document_id
WHERE 1 - (dc.embedding <=> :queryEmbedding) >= :similarityThreshold
ORDER BY dc.embedding <=> :queryEmbedding
LIMIT :topK;
```

When filtering by specific documents:

```sql
SELECT dc.id, dc.content, dc.page_number, dc.document_id,
       d.title AS document_title,
       1 - (dc.embedding <=> :queryEmbedding) AS similarity_score
FROM document_chunks dc
JOIN documents d ON d.id = dc.document_id
WHERE dc.document_id = ANY(:documentIds)
  AND 1 - (dc.embedding <=> :queryEmbedding) >= :similarityThreshold
ORDER BY dc.embedding <=> :queryEmbedding
LIMIT :topK;
```
