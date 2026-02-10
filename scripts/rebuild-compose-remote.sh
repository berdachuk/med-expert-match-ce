#!/bin/bash
# Rebuild Docker Compose images and start all services on remote host.
# App: http://REMOTE_HOST:8094  Docs: http://REMOTE_HOST:8095  DB: REMOTE_HOST:5433
# Usage: ./scripts/rebuild-compose-remote.sh [user@]host
# Example: ./scripts/rebuild-compose-remote.sh 192.168.0.87
#          ./scripts/rebuild-compose-remote.sh berdachuk@192.168.0.87

set -e

REMOTE="${1:-192.168.0.87}"
PROJECT_PATH="/home/berdachuk/projects-ai/med-expert-match-ce"

echo "Rebuilding and starting Docker Compose on ${REMOTE} (project: ${PROJECT_PATH})"
ssh "$REMOTE" "cd ${PROJECT_PATH} && docker compose build --no-cache && docker compose up -d"
echo ""
echo "Stack started. App: http://${REMOTE#*@}:8094  Docs: http://${REMOTE#*@}:8095"
echo "Health: curl http://${REMOTE#*@}:8094/actuator/health"
