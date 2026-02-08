#!/bin/bash

# Serve MkDocs documentation on remote server (192.168.0.73)
# This script builds and serves MkDocs documentation accessible from the network

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DOCS_DIR="$PROJECT_ROOT/docs"
HOST="192.168.0.73"
PORT="8000"

echo "=== Serving MkDocs Documentation ==="
echo ""
echo "Project Root: $PROJECT_ROOT"
echo "Docs Directory: $DOCS_DIR"
echo "Host: $HOST"
echo "Port: $PORT"
echo ""

# Check if mkdocs is installed
if ! command -v mkdocs &> /dev/null; then
    echo "Error: mkdocs is not installed"
    echo "Install with: pip install mkdocs mkdocs-material"
    exit 1
fi

# Check if we're in the project root
if [ ! -f "$PROJECT_ROOT/mkdocs.yml" ]; then
    echo "Error: mkdocs.yml not found in project root"
    exit 1
fi

# Change to project root
cd "$PROJECT_ROOT"

echo "Building MkDocs site..."
mkdocs build --clean

echo ""
echo "Starting MkDocs server..."
echo "Documentation will be available at:"
echo "  - Local: http://localhost:$PORT"
echo "  - Network: http://$HOST:$PORT"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

# Serve on all interfaces (0.0.0.0) to allow network access
# Use 0.0.0.0 to bind to all interfaces, making it accessible from network
mkdocs serve --dev-addr=0.0.0.0:$PORT
