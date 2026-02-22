#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

# Load .env file if it exists
if [[ -f "$PROJECT_DIR/.env" ]]; then
    echo "Loading environment from .env"
    set -a
    source "$PROJECT_DIR/.env"
    set +a
fi

# Determine profile: use "local-ai" if GCP credentials are configured, otherwise "local" (mock)
PROFILE="${1:-}"
if [[ -z "$PROFILE" ]]; then
    if [[ -n "${GCP_PROJECT_ID:-}" && -n "${GOOGLE_APPLICATION_CREDENTIALS:-}" ]]; then
        PROFILE="local-ai"
        echo "GCP credentials detected — using real Vertex AI (local-ai profile)"
    else
        PROFILE="local"
        echo "No GCP credentials — using mock AI models (local profile)"
    fi
fi

cleanup() {
    echo ""
    echo "Shutting down..."
    if [[ -n "${APP_PID:-}" ]]; then
        kill "$APP_PID" 2>/dev/null && wait "$APP_PID" 2>/dev/null
    fi
    docker compose -f "$PROJECT_DIR/docker-compose.yml" down
    echo "All services stopped."
}

trap cleanup EXIT INT TERM

echo "Starting PostgreSQL (pgvector)..."
docker compose -f "$PROJECT_DIR/docker-compose.yml" up -d

echo "Waiting for PostgreSQL to be ready..."
until docker exec classroom-clarity-db pg_isready -U postgres > /dev/null 2>&1; do
    sleep 1
done
echo "PostgreSQL is ready."

echo "Starting Spring Boot app (profile: $PROFILE)..."
"$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" bootRun --args="--spring.profiles.active=$PROFILE" &
APP_PID=$!

wait "$APP_PID"
