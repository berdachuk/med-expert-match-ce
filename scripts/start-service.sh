#!/bin/bash

# Start MedExpertMatch local dev stack (Postgres + mvn -Plocal + MkDocs), or remote SSH start.
# Prefer: ./scripts/start-local-stack.sh  or  ./scripts/restart-service-local.sh
#
# Usage:
#   ./scripts/start-service.sh              # local stack (idempotent)
#   ./scripts/start-service.sh remote       # remote SSH host

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_PATH="$(cd "$SCRIPT_DIR/.." && pwd)"
REMOTE_PROJECT_PATH="${REMOTE_PROJECT_PATH:-/home/berdachuk/projects-ai/med-expert-match-ce}"

if [ "$1" = "remote" ] || [ -n "$1" ]; then
    REMOTE_HOST="${1:-192.168.0.87}"
    if [ "$REMOTE_HOST" = "remote" ]; then
        REMOTE_HOST="192.168.0.87"
        REMOTE_USER="${2:-berdachuk}"
    else
        REMOTE_USER="${2:-berdachuk}"
    fi
    echo "Starting local stack on ${REMOTE_USER}@${REMOTE_HOST}..."
    ssh -o StrictHostKeyChecking=no "${REMOTE_USER}@${REMOTE_HOST}" \
        "cd '${REMOTE_PROJECT_PATH}' && bash ./scripts/start-local-stack.sh"
    exit 0
fi

exec bash "${SCRIPT_DIR}/start-local-stack.sh"
