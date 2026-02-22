# Classroom Clarity RAG

**Grounded Q&A for school-specific curriculum maps and handbooks using Retrieval-Augmented Generation.**

---

## Problem Statement

Teachers, administrators, and parents frequently need quick, accurate answers drawn from school-specific documents -- curriculum maps, student handbooks, policy guides, and course catalogs. Today these documents live as static PDFs buried in shared drives or school websites. Finding a specific policy or curricular detail requires manually searching through dozens of pages across multiple files.

Classroom Clarity RAG solves this by ingesting school documents, chunking and embedding them, and exposing a semantic search-powered Q&A API. Users ask a natural-language question and receive a grounded answer with citations back to the source document and page.

## Target User Persona

**Primary:** School district IT administrators or curriculum coordinators who want to provide a self-service Q&A tool for staff and families.

**Secondary:** AI/ML engineers evaluating the project as a demonstration of RAG pipeline engineering patterns.

## Skills and Engineering Patterns Showcased

| Pattern | Description |
|---------|-------------|
| ETL Pipeline (Extract-Transform-Load) | PDF ingestion, text extraction, chunking, embedding generation, and vector storage |
| Semantic Search | Similarity search over document embeddings using pgvector with HNSW indexing |
| Retrieval-Augmented Generation | Combining retrieved context with an LLM prompt to produce grounded answers |
| Spring AI Integration | Using the Spring AI framework for embedding models, vector stores, and chat completion |
| Cloud-Native Deployment | Containerized Spring Boot service deployed to Google Cloud Run with Cloud SQL |
| API Design | Clean RESTful API contracts with proper error handling and pagination |

## Success Criteria

The project is considered **done** when all of the following are true:

1. **Document Ingestion Works End-to-End:** A user can upload a PDF via the API. The system extracts text, chunks it, generates embeddings, and stores them in PostgreSQL with pgvector.
2. **Semantic Q&A Returns Grounded Answers:** A user can submit a natural-language question and receive an answer that includes cited passages from the ingested documents.
3. **Document Management:** Users can list all ingested documents and delete a document (which also removes its chunks and embeddings).
4. **Deployed to Cloud Run:** The application runs as a containerized service on Google Cloud Run, connected to Cloud SQL for PostgreSQL.
5. **Health Check Passes:** A `/health` endpoint confirms the service and its dependencies (database, embedding model) are operational.
6. **Demo-Ready:** A scripted demo can walk through uploading a sample school handbook, asking 3-5 questions, and showing grounded answers with citations.

## Level of Effort

**Medium** -- Estimated 3-4 focused development phases. The core complexity is in the ETL pipeline and ensuring retrieval quality. Spring AI abstracts much of the LLM and vector store integration, reducing boilerplate.
