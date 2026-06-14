#!/usr/bin/env bash
#
# Local dev stack: Docker Postgres + mvn spring-boot:run (-Plocal) + MkDocs dev server.
# Embedded docs are not on the classpath in this mode; /docs on :8094 redirects to MkDocs :8000.
#
# Usage:
#   ./scripts/start-local-stack.sh
#   ./scripts/start-local-stack.sh --clean-db   # recreate Postgres volume (fresh Flyway)
#
# Environment:
#   APP_PORT, DB_PORT, MKDOCS_PORT, PROFILE (default local)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_PATH="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="${PROJECT_PATH}/docker-compose.yml"

APP_PORT="${APP_PORT:-8094}"
DB_PORT="${DB_PORT:-5433}"
MKDOCS_PORT="${MKDOCS_PORT:-8000}"
PROFILE="${PROFILE:-local}"
CLEAN_DB="${CLEAN_DB:-0}"

CONSOLE_LOG="${PROJECT_PATH}/logs/spring-boot-console.log"
APP_LOG="${PROJECT_PATH}/logs/med-expert-match.log"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

while [ $# -gt 0 ]; do
    case "$1" in
        --clean-db) CLEAN_DB=1 ;;
        -h|--help)
            sed -n '2,12p' "$0" | sed 's/^# \?//'
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}" >&2
            exit 1
            ;;
    esac
    shift
done

docker_compose() {
    if docker compose version >/dev/null 2>&1; then
        docker compose -f "${COMPOSE_FILE}" "$@"
    else
        docker-compose -f "${COMPOSE_FILE}" "$@"
    fi
}

wait_for_postgres() {
    echo "Waiting for PostgreSQL to become healthy..."
    for i in $(seq 1 30); do
        if docker_compose ps postgres 2>/dev/null | grep -q "healthy"; then
            echo -e "${GREEN}PostgreSQL is healthy${NC}"
            return 0
        fi
        if [ "$i" -eq 30 ]; then
            echo -e "${RED}PostgreSQL failed to become healthy within 60s${NC}" >&2
            docker_compose logs postgres --tail 20
            exit 1
        fi
        sleep 2
    done
}

wait_for_mkdocs() {
    echo "Waiting for MkDocs on port ${MKDOCS_PORT}..."
    for i in $(seq 1 20); do
        code="$(curl -sS -o /dev/null -w "%{http_code}" "http://127.0.0.1:${MKDOCS_PORT}/" 2>/dev/null || echo "000")"
        if [ "$code" = "200" ]; then
            echo -e "${GREEN}MkDocs is responding (HTTP ${code})${NC}"
            return 0
        fi
        if [ "$i" -eq 20 ]; then
            echo -e "${YELLOW}MkDocs not responding — install: pip install -r requirements-docs.txt${NC}"
            return 1
        fi
        sleep 1
    done
}

wait_for_app() {
    echo "Waiting for Spring Boot on port ${APP_PORT} (may take 30-90s)..."
    for i in $(seq 1 60); do
        code="$(curl -sS -o /dev/null -w "%{http_code}" "http://127.0.0.1:${APP_PORT}/actuator/health/liveness" 2>/dev/null || echo "000")"
        if [ "$code" = "200" ]; then
            echo -e "${GREEN}Application is responding (HTTP ${code})${NC}"
            return 0
        fi
        if [ "$i" -eq 60 ]; then
            echo -e "${RED}Application failed to start within 120s${NC}" >&2
            echo "Last 30 lines of console log:"
            tail -30 "${CONSOLE_LOG}" 2>/dev/null || true
            exit 1
        fi
        sleep 2
    done
}

verify_docs() {
    echo ""
    echo "Verifying documentation endpoints..."
    local mkdocs_code redirect_code location
    mkdocs_code="$(curl -sS -o /dev/null -w "%{http_code}" "http://127.0.0.1:${MKDOCS_PORT}/" 2>/dev/null || echo "000")"
    location="$(curl -sS -D - -o /dev/null "http://127.0.0.1:${APP_PORT}/docs/" 2>/dev/null | awk '/^[Ll]ocation:/ {print $2}' | tr -d '\r' || true)"
    redirect_code="$(curl -sS -L -o /dev/null -w "%{http_code}" "http://127.0.0.1:${APP_PORT}/docs/index.html" 2>/dev/null || echo "000")"

    if [ "$mkdocs_code" = "200" ]; then
        echo -e "${GREEN}  MkDocs direct:     http://127.0.0.1:${MKDOCS_PORT}/ (HTTP ${mkdocs_code})${NC}"
    else
        echo -e "${RED}  MkDocs direct:     HTTP ${mkdocs_code}${NC}"
    fi
    if echo "${location}" | grep -q ":${MKDOCS_PORT}"; then
        echo -e "${GREEN}  App /docs redirect: ${location}${NC}"
    else
        echo -e "${YELLOW}  App /docs redirect: ${location:-none} (expected MkDocs on :${MKDOCS_PORT})${NC}"
    fi
    if [ "$redirect_code" = "200" ]; then
        echo -e "${GREEN}  /docs follow:      HTTP ${redirect_code}${NC}"
    else
        echo -e "${YELLOW}  /docs follow:      HTTP ${redirect_code}${NC}"
    fi
}

echo -e "${BLUE}=== Starting MedExpertMatch Local Stack ===${NC}"
echo "Profile: ${PROFILE}"
echo ""

cd "${PROJECT_PATH}"
mkdir -p "${PROJECT_PATH}/logs"

if ! command -v docker >/dev/null 2>&1 || ! docker info >/dev/null 2>&1; then
    echo -e "${RED}Docker is not available${NC}" >&2
    exit 1
fi

# Docker app container binds host port 8094 — stop it when using mvn local profile
if docker_compose ps app 2>/dev/null | grep -q "Up"; then
    echo -e "${YELLOW}Stopping Docker app container (local profile uses mvn spring-boot:run)...${NC}"
    docker_compose stop app
fi

if [ "${CLEAN_DB}" -eq 1 ]; then
    echo -e "${YELLOW}Recreating PostgreSQL volume (--clean-db)...${NC}"
    docker_compose down -v
fi

echo -e "${YELLOW}Starting PostgreSQL (docker/Dockerfile.dev)...${NC}"
docker_compose up -d postgres
wait_for_postgres

echo ""
echo -e "${YELLOW}Starting MkDocs dev server (0.0.0.0:${MKDOCS_PORT})...${NC}"
MKDOCS_RESTART=0 MKDOCS_ADDR=0.0.0.0 MKDOCS_PORT="${MKDOCS_PORT}" bash "${SCRIPT_DIR}/start-mkdocs.sh" || true
wait_for_mkdocs || true

if pgrep -f "med-expert-match.*spring-boot:run" >/dev/null 2>&1; then
    echo -e "${YELLOW}Spring Boot already running — skipping mvn start${NC}"
else
    echo ""
    echo -e "${YELLOW}Starting Spring Boot (mvn spring-boot:run -Plocal)...${NC}"
    export MEDEXPERTMATCH_DB_URL="jdbc:postgresql://127.0.0.1:${DB_PORT}/medexpertmatch"
    export MEDEXPERTMATCH_DB_USERNAME="medexpertmatch"
    export MEDEXPERTMATCH_DB_PASSWORD="medexpertmatch"
    export MEDEXPERTMATCH_DOCS_DEV_URL="http://127.0.0.1:${MKDOCS_PORT}"
    export MEDEXPERTMATCH_AUTH_ENABLED="false"
    export SERVER_PORT="${APP_PORT}"
    unset SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD 2>/dev/null || true

    # Console log must differ from med-expert-match.log (Logback FILE appender uses the latter)
    nohup mvn spring-boot:run -Plocal \
        -Dspring-boot.run.arguments="--spring.profiles.active=${PROFILE} --server.port=${APP_PORT}" \
        >"${CONSOLE_LOG}" 2>&1 &
    echo "Maven PID: $!"
    echo "Console log: ${CONSOLE_LOG}"
    echo "App log (Logback): ${APP_LOG}"
fi

wait_for_app
verify_docs

echo ""
echo -e "${GREEN}=== Local Stack Started ===${NC}"
echo "  App:              http://127.0.0.1:${APP_PORT}"
echo "  Swagger:          http://127.0.0.1:${APP_PORT}/swagger-ui.html"
echo "  Docs (redirect):  http://127.0.0.1:${APP_PORT}/docs  -> MkDocs :${MKDOCS_PORT}"
echo "  MkDocs (live):    http://127.0.0.1:${MKDOCS_PORT}"
echo "  Database:         127.0.0.1:${DB_PORT} (medexpertmatch/medexpertmatch)"
echo ""
echo "Logs:"
echo "  tail -f logs/spring-boot-console.log"
echo "  tail -f logs/med-expert-match.log"
echo "  tail -f logs/mkdocs.log"
echo ""
echo "Stop: ./scripts/stop-local-stack.sh"
