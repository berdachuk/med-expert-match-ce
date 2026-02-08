#!/bin/bash
# Start MedExpertMatch service with local profile on remote server
# Usage: ./scripts/start-remote-local.sh [remote_host] [remote_user] [project_path]

set -e

REMOTE_HOST="${1:-192.168.0.73}"
REMOTE_USER="${2:-berdachuk}"
PROJECT_PATH="${3:-/home/berdachuk/projects-ai/expert-match-root/med-expert-match}"
LOG_FILE="/tmp/med-expert-match.log"
PROFILE="local"

echo "=== Starting MedExpertMatch Service Remotely ==="
echo "Remote Host: $REMOTE_HOST"
echo "Remote User: $REMOTE_USER"
echo "Project Path: $PROJECT_PATH"
echo "Profile: $PROFILE"
echo ""

# Check if SSH key is available
if [ -f ~/.ssh/id_rsa ] || [ -f ~/.ssh/id_ed25519 ]; then
    echo "SSH key found, attempting connection..."
else
    echo "⚠ Warning: No SSH key found. You may need to enter password."
fi

# SSH and start service
ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} << EOF
    set -e
    cd ${PROJECT_PATH}
    
    echo "=== Pre-flight Checks ==="
    echo "Java version:"
    java -version 2>&1 | head -1
    echo "Maven version:"
    mvn -version 2>&1 | head -1
    echo ""
    
    # Check if service is already running
    if pgrep -f 'med-expert-match' > /dev/null; then
        echo "⚠ Warning: Service appears to be already running"
        echo "PID: \$(pgrep -f med-expert-match | head -1)"
        read -p "Stop existing service and start new one? (y/N): " -n 1 -r
        echo
        if [[ \$REPLY =~ ^[Yy]\$ ]]; then
            pkill -f med-expert-match
            sleep 2
        else
            echo "Aborting. Service already running."
            exit 1
        fi
    fi
    
    echo "=== Starting Service ==="
    echo "Profile: ${PROFILE}"
    echo "Log file: ${LOG_FILE}"
    echo ""
    
    # Start service in background
    nohup mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=${PROFILE}" > ${LOG_FILE} 2>&1 &
    SERVICE_PID=\$!
    echo "Service started with PID: \$SERVICE_PID"
    echo ""
    
    # Wait a bit and check status
    sleep 5
    if ps -p \$SERVICE_PID > /dev/null; then
        echo "✓ Service is running (PID: \$SERVICE_PID)"
    else
        echo "✗ Service may have failed to start. Check logs:"
        tail -30 ${LOG_FILE}
        exit 1
    fi
    
    echo ""
    echo "=== Service Information ==="
    echo "Service URL: http://${REMOTE_HOST}:8080"
    echo "Swagger UI: http://${REMOTE_HOST}:8080/swagger-ui.html"
    echo "API Docs: http://${REMOTE_HOST}:8080/api/v1/openapi.json"
    echo "Health: http://${REMOTE_HOST}:8080/actuator/health"
    echo ""
    echo "Log file: ${LOG_FILE}"
    echo "View logs: tail -f ${LOG_FILE}"
    echo "Stop service: pkill -f med-expert-match"
EOF

echo ""
echo "=== Service Started ==="
echo "To view logs on remote machine:"
echo "  ssh ${REMOTE_USER}@${REMOTE_HOST} 'tail -f ${LOG_FILE}'"
echo ""
echo "To check service status:"
echo "  ssh ${REMOTE_USER}@${REMOTE_HOST} 'pgrep -f med-expert-match'"
echo ""
echo "Service should be accessible at: http://${REMOTE_HOST}:8080"
