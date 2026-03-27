#!/bin/bash

# Start MkDocs dev server in the background (used by start-service / restart-service-local).
# Skips if mkdocs is missing or mkdocs.yml is absent. Idempotent if already running.
#
# Environment:
#   MKDOCS_PORT   (default 8000)
#   MKDOCS_ADDR   (default 127.0.0.1; use 0.0.0.0 for LAN)
#   MKDOCS_RESTART  set to 1 to stop an existing "mkdocs serve" first (restart flows)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
MKDOCS_PORT="${MKDOCS_PORT:-8000}"
MKDOCS_ADDR="${MKDOCS_ADDR:-127.0.0.1}"
LOG_FILE="${PROJECT_ROOT}/logs/mkdocs.log"

cd "$PROJECT_ROOT"

if [ ! -f "${PROJECT_ROOT}/mkdocs.yml" ]; then
    echo "[MkDocs] mkdocs.yml not found; skipping."
    exit 0
fi

if ! command -v mkdocs &> /dev/null; then
    echo "[MkDocs] mkdocs not on PATH; skipping. Install: pip install -r requirements-docs.txt"
    exit 0
fi

if [ "${MKDOCS_RESTART:-0}" = "1" ]; then
    pkill -f "mkdocs serve" 2>/dev/null || true
    sleep 1
fi

if pgrep -f "mkdocs serve" >/dev/null 2>&1; then
    echo "[MkDocs] already running (http://${MKDOCS_ADDR}:${MKDOCS_PORT})"
    exit 0
fi

mkdir -p "${PROJECT_ROOT}/logs"
nohup mkdocs serve --dev-addr="${MKDOCS_ADDR}:${MKDOCS_PORT}" >"${LOG_FILE}" 2>&1 &
MK_PID=$!
echo "[MkDocs] started PID ${MK_PID} -> http://${MKDOCS_ADDR}:${MKDOCS_PORT}"
echo "[MkDocs] log: ${LOG_FILE}"
