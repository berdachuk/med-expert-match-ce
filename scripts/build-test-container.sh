#!/bin/bash
# Build the test container image for integration tests
# This image includes PostgreSQL 17 with Apache AGE and PgVector extensions

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "Building medexpertmatch-postgres-test Docker image..."
echo "This may take 5-10 minutes on first build..."

cd "$PROJECT_ROOT"

docker build -f docker/Dockerfile.test -t medexpertmatch-postgres-test:latest .

echo ""
echo "✅ Test container image built successfully!"
echo "Image: medexpertmatch-postgres-test:latest"
echo ""
echo "You can now run integration tests with:"
echo "  mvn test -Dtest=*IT"
