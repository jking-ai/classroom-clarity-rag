#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

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

echo "Starting Spring Boot app..."
"$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" bootRun --args='--spring.profiles.active=local' &
APP_PID=$!

wait "$APP_PID"
