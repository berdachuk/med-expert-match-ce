#!/bin/bash

# Restart MedExpertMatch service with local profile, then MkDocs (scripts/start-mkdocs.sh).
# This script stops any running instance and starts a new one.
# Project root is the parent of this scripts/ directory (override REMOTE_PROJECT_PATH for remote SSH).

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_PATH="$(cd "$SCRIPT_DIR/.." && pwd)"
REMOTE_PROJECT_PATH="${REMOTE_PROJECT_PATH:-/home/berdachuk/projects-ai/expert-match-root/med-expert-match}"

# Configuration
PROFILE="local"
LOG_FILE="${PROJECT_PATH}/logs/med-expert-match.log"
REMOTE_HOST="192.168.0.87"
REMOTE_USER="berdachuk"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== Restarting MedExpertMatch Service ===${NC}"
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
    echo -e "${YELLOW}Restarting service locally...${NC}"

    cd ${PROJECT_PATH}

    # Stop existing service and free port 8094
    echo "Stopping existing service..."
    pkill -f "med-expert-match.*spring-boot:run" || echo "No running service found"
    sleep 2
    fuser -k 8094/tcp 2>/dev/null || true
    sleep 2

    # Verify service stopped (no process and port free)
    if pgrep -f "med-expert-match.*spring-boot:run" > /dev/null; then
        echo -e "${RED}Failed to stop service (process still running)${NC}"
        exit 1
    fi
    if ss -tlnp 2>/dev/null | grep -q ':8094 '; then
        echo -e "${RED}Port 8094 still in use${NC}"
        exit 1
    fi

    # Create logs directory if it doesn't exist
    mkdir -p ${PROJECT_PATH}/logs

    # Start service
    echo "Starting service with profile: ${PROFILE}..."
    export MEDEXPERTMATCH_DB_URL=jdbc:postgresql://localhost:5433/medexpertmatch
    export MEDEXPERTMATCH_DB_USERNAME=medexpertmatch
    export MEDEXPERTMATCH_DB_PASSWORD=medexpertmatch
    unset SPRING_DATASOURCE_USERNAME
    unset SPRING_DATASOURCE_PASSWORD
    unset SPRING_DATASOURCE_URL
    nohup mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=${PROFILE}" > ${LOG_FILE} 2>&1 &
    SERVICE_PID=$!
    echo "Service started with PID: $SERVICE_PID"

    # Wait a moment for service to start
    sleep 5

    # Check if service is running
    if ps -p $SERVICE_PID > /dev/null; then
        echo -e "${GREEN}Service restarted successfully${NC}"
        echo "PID: $SERVICE_PID"
        echo "Log file: ${LOG_FILE}"
        echo "Service URL: http://localhost:8094"
        echo ""
        echo "To view logs: tail -f ${LOG_FILE}"
        echo "To check health: curl http://localhost:8094/actuator/health"
    else
        echo -e "${RED}Service failed to start${NC}"
        echo "Check logs: tail -f ${LOG_FILE}"
        exit 1
    fi

    echo ""
    MKDOCS_RESTART=1 bash "${SCRIPT_DIR}/start-mkdocs.sh" || true
fi

echo ""
echo -e "${GREEN}=== Restart Complete ===${NC}"
