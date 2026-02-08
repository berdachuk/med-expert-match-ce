#!/bin/bash

# Start MedExpertMatch service with local profile
# Usage: ./scripts/start-service.sh [remote_host] [remote_user]

set -e

# When first arg is "remote", use default host 192.168.0.73; otherwise host is first arg
if [ "$1" == "remote" ]; then
    REMOTE_HOST="192.168.0.73"
    REMOTE_USER="${2:-berdachuk}"
    RUN_REMOTE=true
elif [ -n "$1" ]; then
    REMOTE_HOST="$1"
    REMOTE_USER="${2:-berdachuk}"
    RUN_REMOTE=true
else
    REMOTE_HOST="192.168.0.73"
    REMOTE_USER="berdachuk"
    RUN_REMOTE=false
fi
PROJECT_PATH="/home/berdachuk/projects-ai/expert-match-root/med-expert-match"
PROFILE="local"
LOG_FILE="${PROJECT_PATH}/logs/med-expert-match.log"
SERVICE_PORT="8094"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Starting MedExpertMatch Service ===${NC}"
echo "Profile: ${PROFILE}"
echo "Port: ${SERVICE_PORT}"
echo ""

# Check if running remotely
if [ "$RUN_REMOTE" == "true" ]; then
    echo -e "${YELLOW}Starting service on ${REMOTE_HOST}...${NC}"
    
    ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} << EOF
        set -e
        cd ${PROJECT_PATH}
        
        # Check if service is already running
        if pgrep -f "med-expert-match.*spring-boot:run" > /dev/null; then
            echo -e "${YELLOW}Service is already running${NC}"
            pgrep -f "med-expert-match.*spring-boot:run" | while read pid; do
                echo "PID: \$pid"
                ps -p \$pid -o pid,cmd,etime
            done
            exit 0
        fi
        
        # Create logs directory if it doesn't exist
        mkdir -p ${PROJECT_PATH}/logs
        
        # Start service
        echo "Starting service with profile: ${PROFILE}..."
        nohup mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=${PROFILE}" > ${LOG_FILE} 2>&1 &
        SERVICE_PID=\$!
        echo "Service started with PID: \$SERVICE_PID"
        
        # Wait for service to start
        echo "Waiting for service to initialize..."
        sleep 10
        
        # Check if service is running
        if ps -p \$SERVICE_PID > /dev/null; then
            echo -e "${GREEN}Service is running (PID: \$SERVICE_PID)${NC}"
            echo "Log file: ${LOG_FILE}"
            echo "Service URL: http://${REMOTE_HOST}:${SERVICE_PORT}"
            echo ""
            echo "To view logs: tail -f ${LOG_FILE}"
            echo "To check health: curl http://localhost:${SERVICE_PORT}/actuator/health"
        else
            echo -e "${RED}Service process not found - may have failed to start${NC}"
            echo "Last 30 lines of log:"
            tail -30 ${LOG_FILE}
            exit 1
        fi
EOF
else
    echo -e "${YELLOW}Starting service locally...${NC}"
    
    cd ${PROJECT_PATH}
    
    # Check if service is already running
    if pgrep -f "med-expert-match.*spring-boot:run" > /dev/null; then
        echo -e "${YELLOW}Service is already running${NC}"
        pgrep -f "med-expert-match.*spring-boot:run" | while read pid; do
            echo "PID: $pid"
            ps -p $pid -o pid,cmd,etime
        done
        exit 0
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
    
    # Wait for service to start
    echo "Waiting for service to initialize..."
    sleep 10
    
    # Check if service is running
    if ps -p $SERVICE_PID > /dev/null; then
        echo -e "${GREEN}Service is running (PID: $SERVICE_PID)${NC}"
        echo "Log file: ${LOG_FILE}"
        echo "Service URL: http://localhost:${SERVICE_PORT}"
        echo ""
        echo "To view logs: tail -f ${LOG_FILE}"
        echo "To check health: curl http://localhost:${SERVICE_PORT}/actuator/health"
    else
        echo -e "${RED}Service process not found - may have failed to start${NC}"
        echo "Last 30 lines of log:"
        tail -30 ${LOG_FILE}
        exit 1
    fi
fi

echo ""
echo -e "${GREEN}=== Start Complete ===${NC}"
