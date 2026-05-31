#!/usr/bin/env bash
#
# Stop the full MedExpertMatch stack:
#   - Docker Compose services (custom PostgreSQL + app)
#   - MkDocs dev server (scripts/start-mkdocs.sh)
#   - Stray Spring Boot processes (mvn spring-boot:run)
#
# Typical workflow: develop on a remote Linux host. Run on that host directly,
# or from your laptop to stop the remote stack over SSH.
#
# Usage (on remote dev host):
#   ./stop-stack.sh
#   ./stop-stack.sh --volumes
#
# Usage (from laptop):
#   ./stop-stack.sh remote
#   ./stop-stack.sh remote berdachuk@192.168.0.87
#   ./stop-stack.sh --volumes --remote 192.168.0.87
#
# Environment:
#   REMOTE_HOST, REMOTE_USER, REMOTE_PROJECT_PATH
#   MEDEXPERTMATCH_STACK_LOCAL=1  — internal; skip SSH delegation
#
#   ./stop-stack.sh --help

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${PROJECT_ROOT}/docker-compose.yml"

REMOTE_HOST="${REMOTE_HOST:-192.168.0.87}"
REMOTE_USER="${REMOTE_USER:-berdachuk}"
REMOTE_PROJECT_PATH="${REMOTE_PROJECT_PATH:-/home/berdachuk/projects-ai/med-expert-match-ce}"

APP_PORT="${APP_PORT:-8094}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

REMOVE_VOLUMES="${REMOVE_VOLUMES:-0}"
REMOTE_MODE=0
REMOTE_TARGET=""

usage() {
    sed -n '2,24p' "$0" | sed 's/^# \?//'
    exit 0
}

while [ $# -gt 0 ]; do
    case "$1" in
        --volumes) REMOVE_VOLUMES=1 ;;
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
}

remote_stop_stack() {
    resolve_remote_ssh
    echo -e "${BLUE}=== Stopping MedExpertMatch Stack on ${REMOTE_SSH} ===${NC}"
    echo ""

    local remote_cmd="cd '${REMOTE_PROJECT_PATH}' && MEDEXPERTMATCH_STACK_LOCAL=1 bash ./stop-stack.sh"
    [ "$REMOVE_VOLUMES" -eq 1 ] && remote_cmd+=" --volumes"

    ssh -o StrictHostKeyChecking=no "${REMOTE_SSH}" "${remote_cmd}"

    echo ""
    echo -e "${GREEN}=== Remote Stack Stopped ===${NC}"
}

stop_stack_local() {
    echo -e "${BLUE}=== Stopping MedExpertMatch Full Stack (local) ===${NC}"
    echo ""

    cd "${PROJECT_ROOT}"

    if pgrep -f "med-expert-match.*spring-boot:run" >/dev/null 2>&1; then
        echo -e "${YELLOW}Stopping local Spring Boot process (mvn spring-boot:run)...${NC}"
        pkill -f "med-expert-match.*spring-boot:run" 2>/dev/null || true
        sleep 2
        pkill -9 -f "med-expert-match.*spring-boot:run" 2>/dev/null || true
    fi

    if command -v fuser >/dev/null 2>&1; then
        fuser -k "${APP_PORT}/tcp" 2>/dev/null || true
    fi

    if pgrep -f "mkdocs serve" >/dev/null 2>&1; then
        echo -e "${YELLOW}Stopping MkDocs dev server...${NC}"
        pkill -f "mkdocs serve" 2>/dev/null || true
        sleep 1
        echo -e "${GREEN}MkDocs stopped${NC}"
    else
        echo "MkDocs dev server: not running"
    fi

    if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
        if [ "${REMOVE_VOLUMES:-0}" -eq 1 ]; then
            echo -e "${YELLOW}Stopping Docker Compose stack and removing volumes...${NC}"
            docker compose -f "${COMPOSE_FILE}" down -v --remove-orphans
        else
            echo -e "${YELLOW}Stopping Docker Compose stack (data volume preserved)...${NC}"
            docker compose -f "${COMPOSE_FILE}" down --remove-orphans
        fi
        echo -e "${GREEN}Docker stack stopped${NC}"
    else
        echo -e "${YELLOW}Docker not available; skipped container shutdown${NC}"
    fi

    echo ""
    echo -e "${GREEN}=== Stack Stopped ===${NC}"
    if [ "${REMOVE_VOLUMES:-0}" -eq 0 ]; then
        echo "Database data preserved in Docker volume medexpertmatch-postgres-data"
        echo "To wipe data: ./stop-stack.sh --volumes"
    fi
}

if [ "${MEDEXPERTMATCH_STACK_LOCAL:-}" != "1" ] && [ "$REMOTE_MODE" -eq 1 ]; then
    remote_stop_stack
    exit 0
fi

stop_stack_local
