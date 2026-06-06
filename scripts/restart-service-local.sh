#!/bin/bash

# Restart MedExpertMatch local dev stack:
#   Docker Postgres + mvn spring-boot:run (-Plocal) + MkDocs dev server.
# Docs: http://localhost:8094/docs redirects to MkDocs on :8000.
#
# Usage:
#   ./scripts/restart-service-local.sh
#   ./scripts/restart-service-local.sh --clean-db
#   ./scripts/restart-service-local.sh remote   # SSH to dev host (legacy)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_PATH="$(cd "$SCRIPT_DIR/.." && pwd)"
REMOTE_PROJECT_PATH="${REMOTE_PROJECT_PATH:-/home/berdachuk/projects-ai/med-expert-match-ce}"
REMOTE_HOST="192.168.0.87"
REMOTE_USER="berdachuk"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

CLEAN_DB=0
if [ "${1:-}" = "--clean-db" ]; then
    CLEAN_DB=1
    shift
fi

echo -e "${BLUE}=== Restarting MedExpertMatch Local Stack ===${NC}"
echo ""

if [ "${1:-}" = "remote" ]; then
    echo -e "${YELLOW}Restarting on ${REMOTE_USER}@${REMOTE_HOST}...${NC}"
    remote_args="./scripts/stop-local-stack.sh --all && ./scripts/start-local-stack.sh"
    if [ "${CLEAN_DB}" -eq 1 ]; then
        remote_args="./scripts/stop-local-stack.sh --volumes && ./scripts/start-local-stack.sh"
    fi
    ssh -o StrictHostKeyChecking=no "${REMOTE_USER}@${REMOTE_HOST}" "cd '${REMOTE_PROJECT_PATH}' && ${remote_args}"
    echo -e "${GREEN}=== Remote Restart Complete ===${NC}"
    exit 0
fi

cd "${PROJECT_PATH}"

echo -e "${YELLOW}Stopping existing local processes...${NC}"
bash "${SCRIPT_DIR}/stop-local-stack.sh"

echo ""
if [ "${CLEAN_DB}" -eq 1 ]; then
    CLEAN_DB=1 bash "${SCRIPT_DIR}/start-local-stack.sh" --clean-db
else
    bash "${SCRIPT_DIR}/start-local-stack.sh"
fi

echo ""
echo -e "${GREEN}=== Restart Complete ===${NC}"
