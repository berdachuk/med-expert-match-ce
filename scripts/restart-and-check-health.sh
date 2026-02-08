#!/bin/bash

# Restart MedExpertMatch service and check health
# Usage: ./scripts/restart-and-check-health.sh [remote_host] [remote_user]

set -e

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

echo -e "${BLUE}=== Restarting MedExpertMatch Service ===${NC}"
echo "Remote Host: ${REMOTE_HOST}"
echo "Profile: ${PROFILE}"
echo ""

# Check if running remotely
if [ "$RUN_REMOTE" == "true" ]; then
    echo -e "${YELLOW}Restarting service remotely on ${REMOTE_HOST}...${NC}"
    
    ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} << EOF
        set -e
        cd ${PROJECT_PATH}
        
        echo "=== Stopping existing service ==="
        pkill -f "med-expert-match.*spring-boot:run" || echo "No running service found"
        sleep 2
        
        # Verify service stopped
        if pgrep -f "med-expert-match.*spring-boot:run" > /dev/null; then
            echo -e "${RED}Failed to stop service, forcing kill...${NC}"
            pkill -9 -f "med-expert-match.*spring-boot:run"
            sleep 2
        fi
        
        # Create logs directory if it doesn't exist
        mkdir -p ${PROJECT_PATH}/logs
        
        # Start service
        echo "=== Starting service with profile: ${PROFILE} ==="
        nohup mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=${PROFILE}" > ${LOG_FILE} 2>&1 &
        SERVICE_PID=\$!
        echo "Service started with PID: \$SERVICE_PID"
        
        # Wait for service to start
        echo "Waiting for service to start..."
        sleep 10
        
        # Check if service is running
        if ps -p \$SERVICE_PID > /dev/null; then
            echo -e "${GREEN}Service is running (PID: \$SERVICE_PID)${NC}"
        else
            echo -e "${RED}Service process not found${NC}"
            echo "Last 30 lines of log:"
            tail -30 ${LOG_FILE}
            exit 1
        fi
        
        # Check health endpoint
        echo ""
        echo "=== Checking Health Endpoint ==="
        for i in {1..5}; do
            echo "Attempt \$i/5: Checking health..."
            HEALTH_RESPONSE=\$(curl -s -w "\nHTTP_CODE:%{http_code}" http://localhost:${SERVICE_PORT}/actuator/health || echo "HTTP_CODE:000")
            HTTP_CODE=\$(echo "\$HEALTH_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
            
            if [ "\$HTTP_CODE" == "200" ]; then
                echo -e "${GREEN}Health check passed!${NC}"
                echo "\$HEALTH_RESPONSE" | grep -v "HTTP_CODE" | jq . 2>/dev/null || echo "\$HEALTH_RESPONSE" | grep -v "HTTP_CODE"
                exit 0
            elif [ "\$HTTP_CODE" == "000" ]; then
                echo "Service not responding yet, waiting..."
                sleep 5
            else
                echo "Health check returned HTTP \$HTTP_CODE"
                echo "Response: \$HEALTH_RESPONSE"
                sleep 3
            fi
        done
        
        echo -e "${YELLOW}Health check did not succeed after 5 attempts${NC}"
        echo "Service may still be starting. Check logs: tail -f ${LOG_FILE}"
        echo "Service URL: http://${REMOTE_HOST}:${SERVICE_PORT}"
EOF
else
    echo -e "${YELLOW}Restarting service locally...${NC}"
    
    cd ${PROJECT_PATH}
    
    # Stop existing service
    echo "=== Stopping existing service ==="
    pkill -f "med-expert-match.*spring-boot:run" || echo "No running service found"
    sleep 2
    
    # Verify service stopped
    if pgrep -f "med-expert-match.*spring-boot:run" > /dev/null; then
        echo -e "${RED}Failed to stop service, forcing kill...${NC}"
        pkill -9 -f "med-expert-match.*spring-boot:run"
        sleep 2
    fi
    
    # Create logs directory if it doesn't exist
    mkdir -p ${PROJECT_PATH}/logs
    
    # Start service
    echo "=== Starting service with profile: ${PROFILE} ==="
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
    echo "Waiting for service to start..."
    sleep 10
    
    # Check if service is running
    if ps -p $SERVICE_PID > /dev/null; then
        echo -e "${GREEN}Service is running (PID: $SERVICE_PID)${NC}"
    else
        echo -e "${RED}Service process not found${NC}"
        echo "Last 30 lines of log:"
        tail -30 ${LOG_FILE}
        exit 1
    fi
    
    # Check health endpoint (local profile binds to 192.168.0.73, so use that for health check)
    echo ""
    echo "=== Checking Health Endpoint ==="
    HEALTH_URL="http://192.168.0.73:${SERVICE_PORT}/actuator/health"
    for i in {1..5}; do
        echo "Attempt $i/5: Checking health at ${HEALTH_URL}..."
        HEALTH_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" "${HEALTH_URL}" || echo "HTTP_CODE:000")
        HTTP_CODE=$(echo "$HEALTH_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
        
        if [ "$HTTP_CODE" == "200" ]; then
            echo -e "${GREEN}Health check passed!${NC}"
            echo "$HEALTH_RESPONSE" | grep -v "HTTP_CODE" | jq . 2>/dev/null || echo "$HEALTH_RESPONSE" | grep -v "HTTP_CODE"
            echo "Service URL: http://192.168.0.73:${SERVICE_PORT}"
            exit 0
        elif [ "$HTTP_CODE" == "000" ]; then
            echo "Service not responding yet, waiting..."
            sleep 5
        else
            echo "Health check returned HTTP $HTTP_CODE"
            echo "Response: $HEALTH_RESPONSE"
            sleep 3
        fi
    done
    
    echo -e "${YELLOW}Health check did not succeed after 5 attempts${NC}"
    echo "Service may still be starting. Check logs: tail -f ${LOG_FILE}"
    echo "Service URL: http://192.168.0.73:${SERVICE_PORT}"
fi

echo ""
echo -e "${GREEN}=== Restart and Health Check Complete ===${NC}"
