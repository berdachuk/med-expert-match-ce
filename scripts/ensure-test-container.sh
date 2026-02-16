#!/bin/bash
# Ensures the test container image exists before integration tests.
# Builds medexpertmatch-postgres-test:latest if not found locally.
# Called automatically by Maven in pre-integration-test phase.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
IMAGE_NAME="medexpertmatch-postgres-test:latest"

if docker image inspect "$IMAGE_NAME" >/dev/null 2>&1; then
    echo "Test container image $IMAGE_NAME exists, skipping build"
    exit 0
fi

echo "Test container image $IMAGE_NAME not found, building..."
cd "$PROJECT_ROOT"
"$SCRIPT_DIR/build-test-container.sh"
