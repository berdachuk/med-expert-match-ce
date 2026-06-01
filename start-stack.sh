#!/usr/bin/env bash
#
# Start the full MedExpertMatch stack:
#   1. Custom PostgreSQL (Apache AGE + PgVector) — docker/Dockerfile.dev
#   2. Spring Boot app (MkDocs site embedded at /docs) — docker/Dockerfile.app
#   3. MkDocs dev server for live documentation (port 8000)
#
# Typical workflow: develop on a remote Linux host (GPU / Ollama). Run this script
# either directly on that host, or from your laptop to start the stack over SSH.
#
# Prerequisites: Docker Engine running, docker compose v2.
# Remote: SSH key access to the dev host; repo checked out at REMOTE_PROJECT_PATH.
#
# Usage (on remote dev host — run in project root):
#   ./start-stack.sh
#   ./start-stack.sh --build
#   ./start-stack.sh --no-mkdocs
#
# Usage (from laptop — start stack on remote via SSH):
#   ./start-stack.sh remote
#   ./start-stack.sh remote berdachuk@192.168.0.87
#   ./start-stack.sh --build --remote 192.168.0.87
#
# Environment:
#   REMOTE_HOST, REMOTE_USER, REMOTE_PROJECT_PATH  — SSH target (remote mode)
#   APP_PORT, DB_PORT, MKDOCS_PORT                  — service ports
#   MEDEXPERTMATCH_STACK_LOCAL=1                    — internal; skip SSH delegation
#
#   ./start-stack.sh --help

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${PROJECT_ROOT}/docker-compose.yml"
SCRIPTS_DIR="${PROJECT_ROOT}/scripts"

REMOTE_HOST="${REMOTE_HOST:-192.168.0.87}"
REMOTE_USER="${REMOTE_USER:-berdachuk}"
REMOTE_PROJECT_PATH="${REMOTE_PROJECT_PATH:-/home/berdachuk/projects-ai/med-expert-match-ce}"

APP_PORT="${APP_PORT:-8094}"
DB_PORT="${DB_PORT:-5433}"
MKDOCS_PORT="${MKDOCS_PORT:-8000}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

BUILD="${BUILD:-0}"
AUTO_REBUILD="${AUTO_REBUILD:-1}"
NO_MKDOCS="${NO_MKDOCS:-0}"
REMOTE_MODE=0
REMOTE_TARGET=""

usage() {
    sed -n '2,29p' "$0" | sed 's/^# \?//'
    exit 0
}

while [ $# -gt 0 ]; do
    case "$1" in
        --build) BUILD=1 ;;
        --no-auto-rebuild) AUTO_REBUILD=0 ;;
        --no-mkdocs) NO_MKDOCS=1 ;;
        --remote)
            REMOTE_MODE=1
            shift
            REMOTE_TARGET="${1:-}"
            [ -n "$REMOTE_TARGET" ] && shift || true
            continue
            ;;
        remote) REMOTE_MODE=1 ;;
        -h|--help) usage ;;
        -*)
            echo -e "${RED}Unknown option: ${1}${NC}" >&2
            usage
            ;;
        *)
            REMOTE_MODE=1
            REMOTE_TARGET="$1"
            ;;
    esac
    shift
done

resolve_remote_ssh() {
    if [ -n "$REMOTE_TARGET" ]; then
        if [[ "$REMOTE_TARGET" == *@* ]]; then
            REMOTE_SSH="$REMOTE_TARGET"
        else
            REMOTE_SSH="${REMOTE_USER}@${REMOTE_TARGET}"
        fi
    else
        REMOTE_SSH="${REMOTE_USER}@${REMOTE_HOST}"
    fi
    REMOTE_HOST_DISPLAY="${REMOTE_SSH#*@}"
}

remote_start_stack() {
    resolve_remote_ssh
    echo -e "${BLUE}=== Starting MedExpertMatch Stack on ${REMOTE_SSH} ===${NC}"
    echo "Project path: ${REMOTE_PROJECT_PATH}"
    echo ""

    local remote_cmd="cd '${REMOTE_PROJECT_PATH}' && MEDEXPERTMATCH_STACK_LOCAL=1 bash ./start-stack.sh"
    [ "$BUILD" -eq 1 ] && remote_cmd+=" --build"
    [ "$AUTO_REBUILD" -eq 0 ] && remote_cmd+=" --no-auto-rebuild"  # defer to local side
    [ "$NO_MKDOCS" -eq 1 ] && remote_cmd+=" --no-mkdocs"

    ssh -o StrictHostKeyChecking=no "${REMOTE_SSH}" "${remote_cmd}"

    echo ""
    echo -e "${GREEN}=== Remote Stack Started ===${NC}"
    echo "  App:           http://${REMOTE_HOST_DISPLAY}:${APP_PORT}"
    echo "  Embedded docs: http://${REMOTE_HOST_DISPLAY}:${APP_PORT}/docs"
    if [ "$NO_MKDOCS" -eq 0 ]; then
        echo "  MkDocs (live): http://${REMOTE_HOST_DISPLAY}:${MKDOCS_PORT}"
    fi
    echo "  Database:      ${REMOTE_HOST_DISPLAY}:${DB_PORT}"
    echo ""
    echo "SSH logs:  ssh ${REMOTE_SSH} 'docker compose -f ${REMOTE_PROJECT_PATH}/docker-compose.yml logs -f app'"
    echo "Stop:      ./stop-stack.sh remote ${REMOTE_SSH}"
}

require_docker() {
    if ! command -v docker >/dev/null 2>&1; then
        echo -e "${RED}Docker is not installed or not on PATH${NC}" >&2
        exit 1
    fi
    if ! docker info >/dev/null 2>&1; then
        echo -e "${RED}Docker daemon is not running${NC}" >&2
        exit 1
    fi
}

wait_for_postgres() {
    echo "Waiting for custom PostgreSQL container to become healthy..."
    for i in $(seq 1 30); do
        if docker compose -f "${COMPOSE_FILE}" ps postgres 2>/dev/null | grep -q "healthy"; then
            echo -e "${GREEN}PostgreSQL is healthy (AGE + PgVector)${NC}"
            return 0
        fi
        if [ "$i" -eq 30 ]; then
            echo -e "${RED}PostgreSQL failed to become healthy within 60s${NC}" >&2
            docker compose -f "${COMPOSE_FILE}" logs postgres --tail 30
            exit 1
        fi
        sleep 2
    done
}

wait_for_app() {
    echo "Waiting for Spring Boot app on port ${APP_PORT} (may take 30-90s on first start)..."
    for i in $(seq 1 60); do
        code="$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${APP_PORT}/actuator/health" 2>/dev/null || true)"
        if [ "$code" = "200" ] || [ "$code" = "503" ]; then
            echo -e "${GREEN}Application is responding (HTTP ${code})${NC}"
            return 0
        fi
        if [ "$i" -eq 60 ]; then
            echo -e "${YELLOW}Application not responding — checking for Flyway checksum mismatch...${NC}"
            local flyway_logs
            flyway_logs="$(docker compose -f "${COMPOSE_FILE}" logs app --tail 30 2>/dev/null)"
            if echo "$flyway_logs" | grep -q "checksum mismatch for migration version"; then
                echo -e "${YELLOW}Flyway checksum mismatch detected — auto-repairing...${NC}"
                local resolved_checksum
                resolved_checksum="$(echo "$flyway_logs" | grep "Resolved locally" | grep -oP '\-?\d+' | tail -1)"
                if [ -n "$resolved_checksum" ]; then
                    docker compose -f "${COMPOSE_FILE}" exec -T postgres \
                        psql -U medexpertmatch -d medexpertmatch \
                        -c "UPDATE medexpertmatch.flyway_schema_history SET checksum = ${resolved_checksum} WHERE version = '1';"
                    echo -e "${GREEN}Flyway checksum repaired — restarting app...${NC}"
                    docker compose -f "${COMPOSE_FILE}" restart app
                    # Re-enter the wait loop
                    for j in $(seq 1 60); do
                        code="$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${APP_PORT}/actuator/health" 2>/dev/null || true)"
                        if [ "$code" = "200" ] || [ "$code" = "503" ]; then
                            echo -e "${GREEN}Application is responding (HTTP ${code})${NC}"
                            return 0
                        fi
                        sleep 2
                    done
                fi
            fi
            echo -e "${RED}Application failed to respond within 120s${NC}" >&2
            docker compose -f "${COMPOSE_FILE}" logs app --tail 40
            exit 1
        fi
        sleep 2
    done
}

free_app_port() {
    if command -v ss >/dev/null 2>&1 && ss -tlnp 2>/dev/null | grep -q ":${APP_PORT} "; then
        echo -e "${YELLOW}Port ${APP_PORT} is in use; stopping stray Spring Boot processes...${NC}"
        pkill -f "med-expert-match.*spring-boot:run" 2>/dev/null || true
        sleep 2
        fuser -k "${APP_PORT}/tcp" 2>/dev/null || true
        sleep 1
    fi
}

ensure_env_file() {
    if [ ! -f "${PROJECT_ROOT}/.env" ]; then
        if [ -f "${PROJECT_ROOT}/.env.example" ]; then
            cp "${PROJECT_ROOT}/.env.example" "${PROJECT_ROOT}/.env"
            echo -e "${YELLOW}Created .env from .env.example — set OLLAMA_BASE_URL to your Ollama host${NC}"
        else
            echo -e "${YELLOW}No .env file; LLM defaults to http://127.0.0.1:11434/v1${NC}"
        fi
    fi
}

start_stack_local() {
    echo -e "${BLUE}=== Starting MedExpertMatch Full Stack (local) ===${NC}"
    echo ""

    require_docker
    cd "${PROJECT_ROOT}"
    mkdir -p "${PROJECT_ROOT}/logs"

    ensure_env_file
    free_app_port

    if [ "${BUILD:-0}" -eq 1 ]; then
        echo -e "${YELLOW}Rebuilding custom images (PostgreSQL + app with embedded MkDocs)...${NC}"
        docker compose -f "${COMPOSE_FILE}" build
        echo ""
    fi

    echo -e "${YELLOW}Starting custom PostgreSQL (docker/Dockerfile.dev)...${NC}"
    docker compose -f "${COMPOSE_FILE}" up -d postgres
    wait_for_postgres

    echo ""
    if [ "${AUTO_REBUILD:-1}" -eq 1 ]; then
        local image_id
        image_id="$(docker images -q medexpertmatch-app:latest 2>/dev/null || true)"
        if [ -n "$image_id" ]; then
            local image_time src_time
            image_time="$(docker inspect --format='{{.Created}}' "$image_id" 2>/dev/null | head -1 | xargs -I{} date -d {} +%s 2>/dev/null || echo 0)"
            src_time="$(find src/ -type f -newer /dev/null -printf '%T@\n' 2>/dev/null | sort -rn | head -1 | cut -d. -f1)"
            src_time="${src_time:-0}"
            if [ "$src_time" -gt "$image_time" ]; then
                echo -e "${YELLOW}Source code changed — rebuilding app image...${NC}"
                docker compose -f "${COMPOSE_FILE}" build app
                free_app_port
            fi
        else
            echo -e "${YELLOW}No existing app image — building...${NC}"
            docker compose -f "${COMPOSE_FILE}" build app
            free_app_port
        fi
    fi

    echo -e "${YELLOW}Starting application container (docker/Dockerfile.app)...${NC}"
    docker compose -f "${COMPOSE_FILE}" up -d app
    wait_for_app

    if [ "${NO_MKDOCS:-0}" -eq 0 ]; then
        echo ""
        echo -e "${YELLOW}Starting MkDocs dev server (0.0.0.0:${MKDOCS_PORT})...${NC}"
        MKDOCS_RESTART=0 MKDOCS_ADDR=0.0.0.0 bash "${SCRIPTS_DIR}/start-mkdocs.sh" || true
    fi

    echo ""
    echo -e "${GREEN}=== Stack Started ===${NC}"
    echo "  App (Swagger):  http://localhost:${APP_PORT}/swagger-ui.html"
    echo "  App health:     http://localhost:${APP_PORT}/actuator/health"
    echo "  Embedded docs:  http://localhost:${APP_PORT}/docs"
    if [ "${NO_MKDOCS:-0}" -eq 0 ]; then
        echo "  MkDocs (live):  http://localhost:${MKDOCS_PORT} (also via host LAN IP)"
    fi
    echo "  Database:       localhost:${DB_PORT} (user/db: medexpertmatch)"
    echo ""
    echo "Logs:"
    echo "  docker compose -f docker-compose.yml logs -f app"
    echo "  docker compose -f docker-compose.yml logs -f postgres"
    echo "  tail -f logs/mkdocs.log"
    echo ""
    echo "Stop: ./stop-stack.sh"
}

if [ "${MEDEXPERTMATCH_STACK_LOCAL:-}" != "1" ] && [ "$REMOTE_MODE" -eq 1 ]; then
    remote_start_stack
    exit 0
fi

start_stack_local
