#!/bin/bash

# Rebuild and restart MedExpertMatch service with local profile on remote server
# Usage: Copy this script to remote host and run it, or run via SSH after authentication

set -e

PROJECT_PATH="/home/berdachuk/projects-ai/expert-match-root/med-expert-match"
PROFILE="local"
LOG_FILE="${PROJECT_PATH}/logs/med-expert-match.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== Rebuilding and Restarting MedExpertMatch Service ===${NC}"
echo "Profile: ${PROFILE}"
echo "Project Path: ${PROJECT_PATH}"
echo ""

cd ${PROJECT_PATH}

# Stop existing service
echo -e "${YELLOW}Stopping existing service...${NC}"
pkill -f "med-expert-match.*spring-boot:run" || echo "No running service found"
sleep 2

# Verify service stopped
if pgrep -f "med-expert-match.*spring-boot:run" > /dev/null; then
    echo -e "${RED}Failed to stop service. Forcing kill...${NC}"
    pkill -9 -f "med-expert-match.*spring-boot:run"
    sleep 2
fi

# Build service
echo -e "${YELLOW}Building service...${NC}"
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}Build failed!${NC}"
    exit 1
fi

echo -e "${GREEN}Build successful!${NC}"
echo ""

# Create logs directory if it doesn't exist
mkdir -p ${PROJECT_PATH}/logs

# Start service
echo -e "${YELLOW}Starting service with profile: ${PROFILE}...${NC}"
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
    echo "Service URL: http://192.168.0.73:8094"
    echo ""
    echo "To view logs: tail -f ${LOG_FILE}"
    echo "To check health: curl http://192.168.0.73:8094/actuator/health"
else
    echo -e "${RED}Service failed to start${NC}"
    echo "Check logs: tail -f ${LOG_FILE}"
    echo ""
    echo "Last 30 lines of log:"
    tail -30 ${LOG_FILE}
    exit 1
fi

echo ""
echo -e "${GREEN}=== Rebuild and Restart Complete ===${NC}"
