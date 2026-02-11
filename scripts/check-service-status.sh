#!/bin/bash

# Check MedExpertMatch service status and troubleshoot
# Usage: ./scripts/check-service-status.sh [remote_host] [remote_user]

set -e

REMOTE_HOST="${1:-192.168.0.87}"
REMOTE_USER="${2:-berdachuk}"
PROJECT_PATH="/home/berdachuk/projects-ai/expert-match-root/med-expert-match"
LOG_FILE="${PROJECT_PATH}/logs/med-expert-match.log"
SERVICE_PORT="8094"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== MedExpertMatch Service Status Check ===${NC}"
echo "Remote Host: ${REMOTE_HOST}"
echo "Port: ${SERVICE_PORT}"
echo ""

# Check if running remotely
if [ "$1" == "remote" ] || [ -n "$1" ]; then
    echo -e "${YELLOW}Checking service status on ${REMOTE_HOST}...${NC}"
    
    ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} << EOF
        set -e
        cd ${PROJECT_PATH}
        
        echo "=== Process Check ==="
        if pgrep -f "med-expert-match.*spring-boot:run" > /dev/null; then
            echo -e "${GREEN}Service process is running${NC}"
            pgrep -f "med-expert-match.*spring-boot:run" | while read pid; do
                echo "PID: \$pid"
                ps -p \$pid -o pid,cmd,etime,pcpu,pmem
            done
        else
            echo -e "${RED}Service process is NOT running${NC}"
        fi
        
        echo ""
        echo "=== Port Check ==="
        if netstat -tuln 2>/dev/null | grep ":${SERVICE_PORT}" > /dev/null || ss -tuln 2>/dev/null | grep ":${SERVICE_PORT}" > /dev/null; then
            echo -e "${GREEN}Port ${SERVICE_PORT} is in use${NC}"
            netstat -tuln 2>/dev/null | grep ":${SERVICE_PORT}" || ss -tuln 2>/dev/null | grep ":${SERVICE_PORT}"
        else
            echo -e "${RED}Port ${SERVICE_PORT} is NOT in use${NC}"
        fi
        
        echo ""
        echo "=== Health Check ==="
        HEALTH_RESPONSE=\$(curl -s -w "\nHTTP_CODE:%{http_code}" --connect-timeout 5 http://localhost:${SERVICE_PORT}/actuator/health 2>&1 || echo "HTTP_CODE:000")
        HTTP_CODE=\$(echo "\$HEALTH_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
        
        if [ "\$HTTP_CODE" == "200" ]; then
            echo -e "${GREEN}Health endpoint is responding${NC}"
            echo "\$HEALTH_RESPONSE" | grep -v "HTTP_CODE" | jq . 2>/dev/null || echo "\$HEALTH_RESPONSE" | grep -v "HTTP_CODE"
        else
            echo -e "${RED}Health endpoint is NOT responding (HTTP \$HTTP_CODE)${NC}"
            echo "Response: \$HEALTH_RESPONSE"
        fi
        
        echo ""
        echo "=== Recent Logs (last 50 lines) ==="
        if [ -f "${LOG_FILE}" ]; then
            tail -50 "${LOG_FILE}"
        else
            echo -e "${YELLOW}Log file not found: ${LOG_FILE}${NC}"
        fi
        
        echo ""
        echo "=== Error Logs (if any) ==="
        if [ -f "${LOG_FILE}" ]; then
            grep -i "error\|exception\|failed" "${LOG_FILE}" | tail -20 || echo "No errors found in recent logs"
        fi
EOF
else
    echo -e "${YELLOW}Checking service status locally...${NC}"
    
    cd ${PROJECT_PATH}
    
    echo "=== Process Check ==="
    if pgrep -f "med-expert-match.*spring-boot:run" > /dev/null; then
        echo -e "${GREEN}Service process is running${NC}"
        pgrep -f "med-expert-match.*spring-boot:run" | while read pid; do
            echo "PID: $pid"
            ps -p $pid -o pid,cmd,etime,pcpu,pmem
        done
    else
        echo -e "${RED}Service process is NOT running${NC}"
    fi
    
    echo ""
    echo "=== Port Check ==="
    if netstat -tuln 2>/dev/null | grep ":${SERVICE_PORT}" > /dev/null || ss -tuln 2>/dev/null | grep ":${SERVICE_PORT}" > /dev/null; then
        echo -e "${GREEN}Port ${SERVICE_PORT} is in use${NC}"
        netstat -tuln 2>/dev/null | grep ":${SERVICE_PORT}" || ss -tuln 2>/dev/null | grep ":${SERVICE_PORT}"
    else
        echo -e "${RED}Port ${SERVICE_PORT} is NOT in use${NC}"
    fi
    
    echo ""
    echo "=== Health Check ==="
    HEALTH_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" --connect-timeout 5 http://localhost:${SERVICE_PORT}/actuator/health 2>&1 || echo "HTTP_CODE:000")
    HTTP_CODE=$(echo "$HEALTH_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    
    if [ "$HTTP_CODE" == "200" ]; then
        echo -e "${GREEN}Health endpoint is responding${NC}"
        echo "$HEALTH_RESPONSE" | grep -v "HTTP_CODE" | jq . 2>/dev/null || echo "$HEALTH_RESPONSE" | grep -v "HTTP_CODE"
    else
        echo -e "${RED}Health endpoint is NOT responding (HTTP $HTTP_CODE)${NC}"
        echo "Response: $HEALTH_RESPONSE"
    fi
    
    echo ""
    echo "=== Recent Logs (last 50 lines) ==="
    if [ -f "${LOG_FILE}" ]; then
        tail -50 "${LOG_FILE}"
    else
        echo -e "${YELLOW}Log file not found: ${LOG_FILE}${NC}"
    fi
    
    echo ""
    echo "=== Error Logs (if any) ==="
    if [ -f "${LOG_FILE}" ]; then
        grep -i "error\|exception\|failed" "${LOG_FILE}" | tail -20 || echo "No errors found in recent logs"
    fi
fi

echo ""
echo -e "${BLUE}=== Status Check Complete ===${NC}"
