#!/bin/bash

# Stop MedExpertMatch service
# Usage: ./scripts/stop-service.sh [remote_host] [remote_user]

set -e

if [ "$1" == "remote" ]; then
    REMOTE_HOST="192.168.0.87"
    REMOTE_USER="${2:-berdachuk}"
    RUN_REMOTE=true
elif [ -n "$1" ]; then
    REMOTE_HOST="$1"
    REMOTE_USER="${2:-berdachuk}"
    RUN_REMOTE=true
else
    REMOTE_HOST="192.168.0.87"
    REMOTE_USER="berdachuk"
    RUN_REMOTE=false
fi
PROJECT_PATH="/home/berdachuk/projects-ai/expert-match-root/med-expert-match"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Stopping MedExpertMatch Service ===${NC}"

# Check if running remotely
if [ "$RUN_REMOTE" == "true" ]; then
    echo -e "${YELLOW}Stopping service on ${REMOTE_HOST}...${NC}"
    
    ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} << EOF
        set -e
        cd ${PROJECT_PATH}
        
        echo "Checking for running service..."
        if pgrep -f "med-expert-match.*spring-boot:run" > /dev/null; then
            echo "Found running service, stopping..."
            pkill -f "med-expert-match.*spring-boot:run"
            sleep 2
            
            # Verify service stopped
            if pgrep -f "med-expert-match.*spring-boot:run" > /dev/null; then
                echo -e "${RED}Service did not stop, forcing kill...${NC}"
                pkill -9 -f "med-expert-match.*spring-boot:run"
                sleep 1
            fi
            
            if ! pgrep -f "med-expert-match.*spring-boot:run" > /dev/null; then
                echo -e "${GREEN}Service stopped successfully${NC}"
            else
                echo -e "${RED}Failed to stop service${NC}"
                exit 1
            fi
        else
            echo -e "${YELLOW}No running service found${NC}"
        fi
EOF
else
    echo -e "${YELLOW}Stopping service locally...${NC}"
    
    cd ${PROJECT_PATH}
    
    echo "Checking for running service..."
    if pgrep -f "med-expert-match.*spring-boot:run" > /dev/null; then
        echo "Found running service, stopping..."
        pkill -f "med-expert-match.*spring-boot:run"
        sleep 2
        
        # Verify service stopped
        if pgrep -f "med-expert-match.*spring-boot:run" > /dev/null; then
            echo -e "${RED}Service did not stop, forcing kill...${NC}"
            pkill -9 -f "med-expert-match.*spring-boot:run"
            sleep 1
        fi
        
        if ! pgrep -f "med-expert-match.*spring-boot:run" > /dev/null; then
            echo -e "${GREEN}Service stopped successfully${NC}"
        else
            echo -e "${RED}Failed to stop service${NC}"
            exit 1
        fi
    else
        echo -e "${YELLOW}No running service found${NC}"
    fi
fi

echo ""
echo -e "${GREEN}=== Stop Complete ===${NC}"
