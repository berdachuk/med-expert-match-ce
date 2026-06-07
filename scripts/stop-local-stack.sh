#!/usr/bin/env bash
#
# Stop local dev stack (mvn Spring Boot + MkDocs). Postgres container is left running by default.
#
# Usage:
#   ./scripts/stop-local-stack.sh
#   ./scripts/stop-local-stack.sh --all        # also stop postgres container
#   ./scripts/stop-local-stack.sh --volumes    # stop postgres and remove DB volume

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_PATH="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="${PROJECT_PATH}/docker-compose.yml"
APP_PORT="${APP_PORT:-8094}"

STOP_POSTGRES=0
REMOVE_VOLUMES=0

while [ $# -gt 0 ]; do
    case "$1" in
        --all) STOP_POSTGRES=1 ;;
        --volumes) STOP_POSTGRES=1; REMOVE_VOLUMES=1 ;;
        -h|--help)
            sed -n '2,8p' "$0" | sed 's/^# \?//'
            exit 0
            ;;
        *) echo "Unknown option: $1" >&2; exit 1 ;;
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

echo "Stopping Spring Boot (mvn spring-boot:run)..."
pkill -f "med-expert-match.*spring-boot:run" 2>/dev/null || true
pkill -f "MedExpertMatchApplication" 2>/dev/null || true
sleep 2
if command -v fuser >/dev/null 2>&1; then
    fuser -k "${APP_PORT}/tcp" 2>/dev/null || true
fi
# Git Bash on Windows: pkill/fuser often miss java.exe; free the app port via PowerShell
if command -v powershell.exe >/dev/null 2>&1; then
    powershell.exe -NoProfile -Command "
      \$p = Get-NetTCPConnection -LocalPort ${APP_PORT} -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique
      foreach (\$pid in \$p) { if (\$pid) { Stop-Process -Id \$pid -Force -ErrorAction SilentlyContinue } }
    " 2>/dev/null || true
fi

echo "Stopping MkDocs..."
pkill -f "mkdocs serve" 2>/dev/null || true

if [ "${STOP_POSTGRES}" -eq 1 ]; then
    if [ "${REMOVE_VOLUMES}" -eq 1 ]; then
        docker_compose down -v
    else
        docker_compose stop postgres
    fi
fi

echo "Local stack stopped."
