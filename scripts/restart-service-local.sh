#!/bin/bash

# Restart MedExpertMatch full stack: Docker Compose (postgres + app), then MkDocs dev server.
# Local mode builds and starts both postgres and app containers via docker compose up.
# Docs are embedded at /docs in the app container; MkDocs dev server runs alongside on port 8000.
# Project root is the parent of this scripts/ directory (override REMOTE_PROJECT_PATH for remote SSH).

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_PATH="$(cd "$SCRIPT_DIR/.." && pwd)"
REMOTE_PROJECT_PATH="${REMOTE_PROJECT_PATH:-/home/berdachuk/projects-ai/expert-match-root/med-expert-match}"

# Configuration
PROFILE="local,debug"
LOG_FILE="${PROJECT_PATH}/logs/med-expert-match.log"
REMOTE_HOST="192.168.0.87"
REMOTE_USER="berdachuk"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Restarting MedExpertMatch Full Stack ===${NC}"
echo ""

# Check if running locally or remotely
if [ "$1" == "remote" ]; then
    echo -e "${YELLOW}Restarting service remotely on ${REMOTE_HOST}...${NC}"
    REMOTE_LOG_FILE="${REMOTE_PROJECT_PATH}/logs/med-expert-match.log"

    ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} << EOF
        set -e
        cd ${REMOTE_PROJECT_PATH}

        # Stop existing service
        echo "Stopping existing service..."
        pkill -f "med-expert-match.*spring-boot:run" || echo "No running service found"
        sleep 2

        # Verify service stopped
        if pgrep -f "med-expert-match.*spring-boot:run" > /dev/null; then
            echo -e "${RED}Failed to stop service${NC}"
            exit 1
        fi

        # Create logs directory if it doesn't exist
        mkdir -p ${REMOTE_PROJECT_PATH}/logs

        # Start service
        echo "Starting service with profile: ${PROFILE}..."
        cd ${REMOTE_PROJECT_PATH}
        nohup mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=${PROFILE}" > ${REMOTE_LOG_FILE} 2>&1 &
        SERVICE_PID=\$!
        echo "Service started with PID: \$SERVICE_PID"

        # Wait a moment for service to start
        sleep 5

        # Check if service is running
        if ps -p \$SERVICE_PID > /dev/null; then
            echo -e "${GREEN}Service restarted successfully${NC}"
            echo "PID: \$SERVICE_PID"
            echo "Log file: ${REMOTE_LOG_FILE}"
            echo "Service URL: http://${REMOTE_HOST}:8094"
        else
            echo -e "${RED}Service failed to start${NC}"
            echo "Check logs: tail -f ${REMOTE_LOG_FILE}"
            exit 1
        fi

        MKDOCS_RESTART=1 bash ${REMOTE_PROJECT_PATH}/scripts/start-mkdocs.sh || true
EOF
else
    echo -e "${YELLOW}Starting full stack locally...${NC}"

    cd ${PROJECT_PATH}

    # Stop existing Spring Boot and MkDocs processes
    echo "Stopping existing Spring Boot service..."
    pkill -f "med-expert-match.*spring-boot:run" || echo "  No running Spring Boot found"
    sleep 2
    fuser -k 8094/tcp 2>/dev/null || true
    sleep 1

    pkill -f "mkdocs serve" 2>/dev/null && echo "  MkDocs stopped" || echo "  No running MkDocs found"
    sleep 1

    # Start Docker Compose services (postgres with AGE + pgVector)
    echo ""
    echo "Starting Docker Compose services (postgres with custom container)..."
    if ! docker compose ps postgres | grep -q "healthy"; then
        docker compose up -d postgres
        echo "Waiting for postgres to become healthy..."
        for i in $(seq 1 30); do
            if docker compose ps postgres | grep -q "healthy"; then
                echo -e "${GREEN}Postgres is healthy${NC}"
                break
            fi
            if [ "$i" -eq 30 ]; then
                echo -e "${RED}Postgres failed to become healthy${NC}"
                docker compose logs postgres --tail 20
                exit 1
            fi
            sleep 2
        done
    else
        echo -e "${GREEN}Postgres already healthy${NC}"
    fi

    # Build app Docker image if needed (updated Dockerfile.app or source)
    echo ""
    echo "Building app Docker image..."
    docker compose build --build-arg BUILDKIT_INLINE_CACHE=1 app

    # Verify port 8094 is free
    if ss -tlnp 2>/dev/null | grep -q ':8094 '; then
        echo -e "${RED}Port 8094 still in use${NC}"
        exit 1
    fi

    # Create logs directory if it doesn't exist
    mkdir -p ${PROJECT_PATH}/logs

    # Start app container
    echo ""
    echo "Starting app container..."
    docker compose up -d app

    # Wait for Spring Boot inside the container
    echo "Waiting for Spring Boot to start (may take 30-60s)..."
    for i in $(seq 1 60); do
        if curl -s -o /dev/null -w "%{http_code}" http://localhost:8094/actuator/health 2>/dev/null | grep -q "200\|503"; then
            echo -e "${GREEN}Spring Boot is responding${NC}"
            break
        fi
        if [ "$i" -eq 60 ]; then
            echo -e "${RED}Spring Boot failed to start within 120s${NC}"
            echo "Last 30 lines of app log:"
            docker compose logs app --tail 30
            exit 1
        fi
        sleep 2
    done

    echo ""
    echo -e "${GREEN}Full stack started${NC}"
    echo "  App:       http://localhost:8094"
    echo "  Docs:      http://localhost:8094/docs (embedded in app)"
    echo "  MkDocs:    http://localhost:8000 (dev server)"
    echo "  Database:  localhost:5433"
    echo ""
    echo "To view app logs:   docker compose logs -f app"
    echo "To view db logs:    docker compose logs -f postgres"
    echo "To check health:    curl http://localhost:8094/actuator/health"

    echo ""
    MKDOCS_RESTART=1 bash "${SCRIPT_DIR}/start-mkdocs.sh" || true
fi

echo ""
echo -e "${GREEN}=== Restart Complete ===${NC}"
