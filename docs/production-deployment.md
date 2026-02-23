# Production Deployment — Classroom Clarity RAG

How to deploy and operate the application on Google Cloud Platform.

---

## Physical Architecture

```mermaid
flowchart TB
    subgraph Internet
        Client[REST Client / Browser]
    end

    subgraph GCP["Google Cloud Platform — jking-ai-labs / us-central1"]
        subgraph CloudRun["Cloud Run"]
            CR[classroom-clarity-rag<br/>512 Mi · 1 vCPU · 0-2 instances]
        end

        subgraph CloudSQL["Cloud SQL"]
            PG[jking-ai-labs-pg<br/>PostgreSQL 16 · db-f1-micro · 10 GB HDD]
            DB[(classroom-clarity)]
            PG --- DB
        end

        subgraph GCS["Cloud Storage"]
            Bucket[gs://jking-ai-labs-classroom-clarity<br/>Standard · us-central1]
        end

        subgraph AR["Artifact Registry"]
            Repo[docker-repo<br/>us-central1-docker.pkg.dev/jking-ai-labs/docker-repo]
        end

        subgraph SM["Secret Manager"]
            S1[classroom-clarity-api-key]
            S2[classroom-clarity-db-password]
        end

        subgraph VertexAI["Vertex AI"]
            Embed[text-embedding-005]
            Chat[Gemini 2.0 Flash]
        end
    end

    Client -- "HTTPS" --> CR
    CR -- "Unix socket<br/>(Cloud SQL socket factory)" --> PG
    CR -- "GCS client library" --> Bucket
    CR -- "gRPC" --> Embed
    CR -- "gRPC" --> Chat
    SM -.-> CR
    AR -.-> CR
```

### Key Connections

| From | To | Method |
|------|----|--------|
| Cloud Run → Cloud SQL | Unix socket via `postgres-socket-factory` (no Auth Proxy sidecar) |
| Cloud Run → GCS | `google-cloud-storage` client library with ADC |
| Cloud Run → Vertex AI | Spring AI gRPC clients with ADC |
| Cloud Run ← Secrets | Mounted as environment variables at container startup |

---

## GCP Resources

| Resource | Name / ID | Spec |
|----------|-----------|------|
| **Project** | `jking-ai-labs` | |
| **Region** | `us-central1` | All resources colocated |
| **Cloud Run service** | `classroom-clarity-rag` | 512 Mi, 1 vCPU, 0-2 instances, scale-to-zero |
| **Cloud SQL instance** | `jking-ai-labs-pg` | PG 16, `db-f1-micro`, 10 GB HDD, Enterprise edition |
| **Database** | `classroom-clarity` | Within `jking-ai-labs-pg` |
| **GCS bucket** | `gs://jking-ai-labs-classroom-clarity` | Standard class, uniform bucket-level access |
| **Artifact Registry** | `docker-repo` | Docker format, `us-central1` |
| **Service account** | `classroom-clarity-run@jking-ai-labs.iam.gserviceaccount.com` | Cloud Run runtime identity |
| **Secrets** | `classroom-clarity-api-key`, `classroom-clarity-db-password` | Latest version mounted as env vars |

### Service Account IAM Roles

| Role | Purpose |
|------|---------|
| `roles/aiplatform.user` | Call Vertex AI embedding and chat models |
| `roles/cloudsql.client` | Connect to Cloud SQL via socket factory |
| `roles/storage.objectAdmin` | Read/write/delete PDFs in GCS |
| `roles/secretmanager.secretAccessor` | Read secrets (bound per-secret, not project-wide) |

---

## Cloud Run Configuration

### Environment Variables

| Variable | Value | Source |
|----------|-------|--------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Set in container image (Jib) |
| `DATABASE_URL` | `jdbc:postgresql:///classroom-clarity?cloudSqlInstance=jking-ai-labs:us-central1:jking-ai-labs-pg&socketFactory=com.google.cloud.sql.postgres.SocketFactory` | Env var |
| `DATABASE_USERNAME` | `postgres` | Env var |
| `DATABASE_PASSWORD` | *(from Secret Manager)* | Secret: `classroom-clarity-db-password:latest` |
| `API_KEY` | *(from Secret Manager)* | Secret: `classroom-clarity-api-key:latest` |
| `GCP_PROJECT_ID` | `jking-ai-labs` | Env var |
| `GCP_LOCATION` | `us-central1` | Env var |
| `GCS_BUCKET_NAME` | `jking-ai-labs-classroom-clarity` | Env var |
| `FRONTEND_ORIGIN` | `*` | Env var (restrict for production frontends) |

### Resource Limits

| Setting | Value | Rationale |
|---------|-------|-----------|
| Memory | 512 Mi | Sufficient for Spring Boot + PDF processing |
| CPU | 1 vCPU | Single-core is adequate for this workload |
| Min instances | 0 | Scale to zero when idle (cost savings) |
| Max instances | 2 | Cap to prevent runaway costs |
| Concurrency | 50 | Requests per instance |
| Timeout | 300s | Large PDF uploads may take time |
| CPU throttling | Enabled | CPU allocated only during request processing |

### Cost Optimization

Estimated monthly cost: **~$10-18** (idle).

| Component | Estimated Cost | Notes |
|-----------|---------------|-------|
| Cloud SQL `db-f1-micro` | ~$10/mo | Dominant cost; runs 24/7 |
| Cloud Run | ~$0-5/mo | Scale-to-zero; pay per request |
| GCS | ~$0.02/GB/mo | Negligible for small document volumes |
| Artifact Registry | ~$0.10/GB/mo | Single image, ~200 MB |
| Vertex AI | Pay-per-use | ~$0.00001/1K chars embedding, ~$0.075/1M input tokens chat |
| Secret Manager | Free tier | 6 active secret versions free |

---

## Build & Deploy

### Prerequisites

- `gcloud` CLI authenticated with access to `jking-ai-labs`
- Docker credential helper configured: `gcloud auth configure-docker us-central1-docker.pkg.dev`
- Java 21 (for Gradle build)

### Build and Push Container Image

```bash
./gradlew jib
```

This uses the [Jib Gradle plugin](https://github.com/GoogleContainerTools/jib) to build an optimized layered image and push it directly to Artifact Registry — no Docker daemon required. The image is tagged with both `latest` and the project version.

Image: `us-central1-docker.pkg.dev/jking-ai-labs/docker-repo/classroom-clarity-rag`

### Deploy to Cloud Run

```bash
gcloud run deploy classroom-clarity-rag \
  --image=us-central1-docker.pkg.dev/jking-ai-labs/docker-repo/classroom-clarity-rag:latest \
  --region=us-central1 \
  --platform=managed \
  --service-account=classroom-clarity-run@jking-ai-labs.iam.gserviceaccount.com \
  --add-cloudsql-instances=jking-ai-labs:us-central1:jking-ai-labs-pg \
  --set-env-vars="DATABASE_URL=jdbc:postgresql:///classroom-clarity?cloudSqlInstance=jking-ai-labs:us-central1:jking-ai-labs-pg&socketFactory=com.google.cloud.sql.postgres.SocketFactory" \
  --set-env-vars="DATABASE_USERNAME=postgres" \
  --set-env-vars="GCP_PROJECT_ID=jking-ai-labs" \
  --set-env-vars="GCP_LOCATION=us-central1" \
  --set-env-vars="GCS_BUCKET_NAME=jking-ai-labs-classroom-clarity" \
  --set-env-vars="FRONTEND_ORIGIN=*" \
  --set-secrets="API_KEY=classroom-clarity-api-key:latest" \
  --set-secrets="DATABASE_PASSWORD=classroom-clarity-db-password:latest" \
  --port=8080 \
  --memory=512Mi \
  --cpu=1 \
  --min-instances=0 \
  --max-instances=2 \
  --timeout=300 \
  --concurrency=50 \
  --cpu-throttling \
  --allow-unauthenticated \
  --project=jking-ai-labs
```

### Quick Redeploy (code changes only)

```bash
./gradlew jib && gcloud run deploy classroom-clarity-rag \
  --image=us-central1-docker.pkg.dev/jking-ai-labs/docker-repo/classroom-clarity-rag:latest \
  --region=us-central1 \
  --project=jking-ai-labs
```

When only the image changes (no env var or secret updates), a minimal deploy command is sufficient — Cloud Run preserves the existing configuration.

---

## Verification

### Health Check

```bash
curl -s https://classroom-clarity-rag-153583612125.us-central1.run.app/api/v1/health | python3 -m json.tool
```

Expected:

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

### Authenticated API Call

```bash
curl -s -H "X-API-Key: <key>" \
  https://classroom-clarity-rag-153583612125.us-central1.run.app/api/v1/documents | python3 -m json.tool
```

### View Logs

```bash
# Recent logs
gcloud run services logs read classroom-clarity-rag --region=us-central1 --limit=50

# Stream logs in real time
gcloud run services logs tail classroom-clarity-rag --region=us-central1

# Full structured logs (Cloud Logging)
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=classroom-clarity-rag" \
  --project=jking-ai-labs --limit=50 --format=json
```

### Database Access

Connect via Cloud SQL Auth Proxy for ad-hoc queries:

```bash
# Install the proxy (if not already installed)
# https://cloud.google.com/sql/docs/postgres/connect-auth-proxy

cloud-sql-proxy jking-ai-labs:us-central1:jking-ai-labs-pg &
psql "host=127.0.0.1 port=5432 dbname=classroom-clarity user=postgres"
```

---

## Troubleshooting

### Container fails to start (port timeout)

Check logs for the specific revision:

```bash
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.revision_name=<REVISION>" \
  --project=jking-ai-labs --limit=30 --format=json
```

Common causes:
- **Wrong main class** in Jib config — verify `build.gradle.kts` has `mainClass = "com.jkingai.classroomclarity.ClassroomClarityApplication"`
- **Missing environment variables** — Cloud Run will start the container but Spring Boot fails during bean initialization
- **Cloud SQL connection failure** — ensure `--add-cloudsql-instances` is set and the service account has `roles/cloudsql.client`

### Cold start latency

Spring Boot on Cloud Run takes ~18-20 seconds for a cold start. This is expected with `--min-instances=0`. If latency matters:

```bash
# Keep one instance warm (~$15-25/mo additional)
gcloud run services update classroom-clarity-rag \
  --min-instances=1 --region=us-central1
```

### Out of memory

If the service OOMs on large PDFs, increase memory:

```bash
gcloud run services update classroom-clarity-rag \
  --memory=1Gi --region=us-central1
```

### Secret rotation

To rotate a secret (e.g., the API key):

```bash
echo -n "<NEW_VALUE>" | gcloud secrets versions add classroom-clarity-api-key --data-file=-

# Redeploy to pick up the new version (secrets reference :latest)
gcloud run deploy classroom-clarity-rag \
  --image=us-central1-docker.pkg.dev/jking-ai-labs/docker-repo/classroom-clarity-rag:latest \
  --region=us-central1
```

---

## Infrastructure Setup (from scratch)

If you need to recreate the infrastructure from scratch, here are the one-time setup commands. These were run during the initial deployment and are documented here for reference.

### Enable APIs

```bash
gcloud services enable \
  artifactregistry.googleapis.com \
  run.googleapis.com \
  sqladmin.googleapis.com \
  secretmanager.googleapis.com \
  storage.googleapis.com \
  --project=jking-ai-labs
```

### Create Resources

```bash
# Artifact Registry
gcloud artifacts repositories create docker-repo \
  --repository-format=docker \
  --location=us-central1 \
  --description="Docker images for jking-ai-labs" \
  --project=jking-ai-labs

# Cloud SQL (takes ~10 minutes)
gcloud sql instances create jking-ai-labs-pg \
  --database-version=POSTGRES_16 \
  --edition=enterprise \
  --tier=db-f1-micro \
  --region=us-central1 \
  --storage-type=HDD \
  --storage-size=10GB \
  --no-storage-auto-increase \
  --project=jking-ai-labs

gcloud sql users set-password postgres \
  --instance=jking-ai-labs-pg \
  --password=<PASSWORD>

gcloud sql databases create classroom-clarity \
  --instance=jking-ai-labs-pg

# GCS bucket
gcloud storage buckets create gs://jking-ai-labs-classroom-clarity \
  --location=us-central1 \
  --default-storage-class=STANDARD \
  --uniform-bucket-level-access

# Service account
gcloud iam service-accounts create classroom-clarity-run \
  --display-name="Classroom Clarity Cloud Run SA"

gcloud projects add-iam-policy-binding jking-ai-labs \
  --member="serviceAccount:classroom-clarity-run@jking-ai-labs.iam.gserviceaccount.com" \
  --role="roles/aiplatform.user"

gcloud projects add-iam-policy-binding jking-ai-labs \
  --member="serviceAccount:classroom-clarity-run@jking-ai-labs.iam.gserviceaccount.com" \
  --role="roles/cloudsql.client"

gcloud projects add-iam-policy-binding jking-ai-labs \
  --member="serviceAccount:classroom-clarity-run@jking-ai-labs.iam.gserviceaccount.com" \
  --role="roles/storage.objectAdmin"

# Secrets
echo -n "<API_KEY>" | gcloud secrets create classroom-clarity-api-key --data-file=-
echo -n "<DB_PASSWORD>" | gcloud secrets create classroom-clarity-db-password --data-file=-

gcloud secrets add-iam-policy-binding classroom-clarity-api-key \
  --member="serviceAccount:classroom-clarity-run@jking-ai-labs.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud secrets add-iam-policy-binding classroom-clarity-db-password \
  --member="serviceAccount:classroom-clarity-run@jking-ai-labs.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

### Disable Container Scanning (cost avoidance)

```bash
gcloud services disable containerscanning.googleapis.com --project=jking-ai-labs
```

Container Scanning auto-scans every image push and can generate significant cost. Artifact Registry storage alone is ~$0.10/GB/month.
